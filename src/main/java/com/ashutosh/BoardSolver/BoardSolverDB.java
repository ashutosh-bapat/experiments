package com.ashutosh.BoardSolver;

import com.google.common.annotations.VisibleForTesting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Database to store the board state and moves
public class BoardSolverDB {
    private static final Logger LOGGER = Logger.getLogger(BoardSolverDB.class.getName());
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String statesTableName;
    private final String movesTableName;
    private final String dbSchema;
    private static final int MAX_STATES_TO_FETCH = 10000;

    enum BoardProcState {
        NEW,
        QUEUED,
        PROCESSED
    };

    static
    {
        try
        {
            Class.forName("org.postgresql.Driver");
        }
        catch (ClassNotFoundException cnfex)
        {
            throw new ExceptionInInitializerError(cnfex);
        }
    }

    public BoardSolverDB(String dbUrl, String dbUser, String dbPassword, String statesTableName, String movesTableName,
                         String dbSchema) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.dbSchema = dbSchema;
        this.statesTableName = dbSchema + "." + statesTableName;
        this.movesTableName = dbSchema + "." + movesTableName;
    }

    public void createObjects() throws SQLException {
        String createSchemeStmt = "CREATE SCHEMA " + dbSchema;
        // States table records all the states reachable from the initial state.
        String createStateTableStmt = "CREATE TABLE " + statesTableName +
                "(id BIGINT PRIMARY KEY," + // uniquely identifies a state
                "state_desc VARCHAR UNIQUE," + // describes the state of board
                // the state of this board state
                // new - for a newly created state, yet to be queued for processing
                // processing - queued for processing or being processed
                // processed - done, all moves in
                "state_proc varchar NOT NULL," +
                // is final state
                "is_final boolean NOT NULL" +
                ")";
        // bv_moves records moves possible for each board state
        String createMovesTableStmt = "CREATE TABLE " + movesTableName +
                // identifier indicating the starting board state for this move
                "(start_state BIGINT REFERENCES " + statesTableName + "(id) NOT NULL," +
                // Description of move
                "move_desc varchar NOT NULL," +
                // identifier of the resultant board state after applying this move
                "end_state BIGINT REFERENCES " + statesTableName + "(id) NOT NULL," +
                // start and end state are unique for every move and identify the move
                "UNIQUE(start_state, end_state)," +
                "UNIQUE(start_state, move_desc)" +
                ")";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement createStmt = conn.createStatement()) {
            createStmt.executeUpdate(createSchemeStmt);
            createStmt.executeUpdate(createStateTableStmt);
            createStmt.executeUpdate(createMovesTableStmt);
        }
    }

    public void dropObjects() throws SQLException {
        String dropCommand = "DROP TABLE " + movesTableName + "," + statesTableName;
        String dropSchemaCommand = "DROP SCHEMA " + dbSchema;
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                Statement dropStmt = conn.createStatement()) {
            dropStmt.executeUpdate(dropCommand);
            dropStmt.executeUpdate(dropSchemaCommand);
        }
    }

    public void clearObjects() throws SQLException {
        String truncateCommand = "TRUNCATE TABLE " + movesTableName + "," + statesTableName;
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement truncateStmt = conn.createStatement()) {
            truncateStmt.executeUpdate(truncateCommand);
        }
    }

    /**
     * Given the starting state of the board, find the solution i.e. the sets of moves which when applied leads to the
     * final state/s. Return all the solutions.
     * TODO: For too many solution using an interator instead of a collection will avoid OOM.
     * @param initialState the state for which to find the solution.
     */
    public Map<String, BoardState> findSolutions(BoardState initialState) throws SQLException {
        String resultCTEName = "moves_path";
        String movesSep = "->";
        String nonRecursiveQuery = "SELECT start_state start_state, move_desc moves, end_state end_state" +
                                        " FROM " + movesTableName +
                                        " WHERE start_state = " + initialState.getId();
        String recursiveQuery = "SELECT mp.start_state start_state, mp.moves || " + quoteString(movesSep) + " || m.move_desc moves, m.end_state end_state" +
                                    " FROM " + movesTableName + " m, " + resultCTEName + " mp" +
                                    " WHERE mp.end_state = m.start_state";
        String finalQuery = "SELECT mp.moves, s.state_desc, s.id" +
                                " FROM " + resultCTEName + " mp, " + statesTableName + " s" +
                                " WHERE mp.end_state = s.id AND s.is_final";
        String solutionQuery = "WITH RECURSIVE " + resultCTEName + " AS (" +
                                    nonRecursiveQuery + " UNION " + recursiveQuery + ") " + finalQuery;
        Map<String, BoardState> solutions = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement solutionQueryStmt = conn.createStatement();
             ResultSet rs = solutionQueryStmt.executeQuery(solutionQuery)) {

            while (rs.next()) {
                String moves_path = rs.getString(1);
                String finalStateDesc = rs.getString(2);
                Long finalStateId = rs.getLong(3);
                BoardState finalState = initialState.newState(finalStateId, finalStateDesc);
                solutions.put(moves_path, finalState);
            }
        }

        return solutions;
    }

    public BSDCConn getConnection() throws SQLException {
        return new BSDCConn();
    }

    private String quoteString(String str) {
        return "'" + str + "'";
    }

    public class BSDCConn implements AutoCloseable {
        private final Connection conn;
        private final PreparedStatement searchStateStmt;
        private final PreparedStatement insertStateStmt;
        private final PreparedStatement fetchNewStateStmt;
        private final PreparedStatement fetchQueuedStateStmt;
        private final PreparedStatement updateStateProcStmt;
        private final PreparedStatement insertMoveStmt;

        public BSDCConn() throws SQLException {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            conn.setAutoCommit(false);
            searchStateStmt = conn.prepareStatement("SELECT id FROM " + statesTableName + " WHERE state_desc = ?");
            insertStateStmt = conn.prepareStatement("INSERT INTO " + statesTableName + "(id, state_desc, state_proc, is_final) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING RETURNING id");
            updateStateProcStmt = conn.prepareStatement("UPDATE " + statesTableName + " SET state_proc = " +
                    quoteString(BoardProcState.PROCESSED.toString()) + " WHERE id = ?");
            fetchNewStateStmt = conn.prepareStatement("UPDATE " + statesTableName + " SET state_proc = " + quoteString(BoardProcState.QUEUED.toString()) +
                    " WHERE id IN (SELECT id FROM " + statesTableName + " WHERE state_proc = " + quoteString(BoardProcState.NEW.toString()) + " LIMIT ?)" +
                    " RETURNING id, state_desc");
            fetchQueuedStateStmt = conn.prepareStatement("SELECT id, state_desc FROM " + statesTableName + " WHERE state_proc = " +
                    quoteString(BoardProcState.QUEUED.toString()));
            insertMoveStmt = conn.prepareStatement("INSERT INTO " + movesTableName + " (start_state, move_desc, end_state) " +
                    " VALUES (?, ?, ?)");
        }

        // Insert the given state in the database if it's already not recorded. If the state is inserted anew the given id
        // is used for insertion. In this case the function returns true. If the given state is already in the database the
        // function returns false. In either case, given state is updated with it's identifier in the database.

        // TODO: Modify this method to pass a list of states with possible ids instead of one state at a time. Helps to add all
        // the resultant moves in one go, improving performance.
        // Also, for testing pass a flag to indicate whether to just search the state instead of inserting when absent.
        public void searchAndInsertState(BoardState state, long id) throws SQLException {
            String boardStateDesc = state.getDesc();
            boolean commitTran = true;

            // 1. If the state is already in the database get its id. Usually a state is searched multiple times, so most of
            // the times this step should succeed. We could just execute 2 and 3 below, but INSERTs are costlier than SELECT
            // so this step actually improves the performance.
            // TODO: convert these steps into a UDF thus saving two trips to database in worst case.
            searchStateStmt.setString(1, boardStateDesc);
            try (ResultSet rsSSS = searchStateStmt.executeQuery()) {
                if (!rsSSS.next()) {
                    // 2. Try inserting the state in the database and get its id. If the state is already there (somebody inserted it between this
                    //    and the above step) insert will not return anything. Otherwise it will return the id.
                    insertStateStmt.setLong(1, id);
                    insertStateStmt.setString(2, boardStateDesc);
                    insertStateStmt.setString(3, BoardProcState.NEW.toString());
                    insertStateStmt.setBoolean(4, state.isFinalState());
                    try (ResultSet rsISS = insertStateStmt.executeQuery()) {
                        if (!rsISS.next()) {
                            // 3. Search the state again if insert didn't return anything. Now we should find the state in the database since
                            //    a state is never deleted. Any exception here will be caught by the outermost try block.
                            searchStateStmt.setString(1, boardStateDesc);
                            try (ResultSet rsSSS2 = searchStateStmt.executeQuery()) {
                                state.setId((rsISS.getLong(1)));
                            }
                        } else {
                            state.setId((rsISS.getLong(1)));
                        }
                    }
                } else {
                    state.setId((rsSSS.getLong(1)));
                }
            } catch (SQLException sqe) {
               commitTran = false;
               throw sqe;
            } finally {
                // We are in auto commit false mode, end the transaction
                endTransaction(commitTran);
            }
        }

        // We insert all the moves pertaining to a given state in one INSERT
        // statement and also update the status of the starting state to examined.
        // We do this in a single transaction so that when the status of the
        // starting state is set as examined, we know that all the moves pertaining
        // to that state are recorded, otherwise none are recorded.
        public void addMoves(BoardState state, Map<BoardMove, BoardState> newStates) throws SQLException {
            if (newStates.isEmpty()) {
                // No moves nothing to be done.
                return;
            }

            boolean commitTran = true;
            try {
                // Add moves to a batch INSERT and execute it only ones
                for (Map.Entry<BoardMove, BoardState> entry : newStates.entrySet()) {
                    insertMoveStmt.setLong(1, state.getId());
                    insertMoveStmt.setString(2, entry.getKey().getDesc());
                    insertMoveStmt.setLong(3, entry.getValue().getId());
                    insertMoveStmt.addBatch();
                }
                // TODO: should check whether the number of rows actually inserted is same as the number of moves passed.
                insertMoveStmt.executeBatch();
                updateStateProcStmt.setLong(1, state.getId());
                // TODO: Should make sure that only one state is updated.
                updateStateProcStmt.executeUpdate();
            } catch (SQLException sqle) {
                LOGGER.severe("error while inserting moves using statement: " + insertMoveStmt);
                commitTran = false;
                throw sqle;
            } finally {
                endTransaction(commitTran);
            }
        }

        private void endTransaction(boolean commit) throws SQLException {
            if (commit) {
                conn.commit();
            } else {
                conn.rollback();
            }

        }
        // Fetch new states. Mark those states are queued in the database.
        public LinkedList<BoardState> readUnprocessedStates(BoardState sampleState)
                throws IllegalStateException, SQLException {
            boolean commitTran = true;
            LinkedList<BoardState> states = new LinkedList<>();

            fetchNewStateStmt.setInt(1, MAX_STATES_TO_FETCH);
            fetchNewStateStmt.setFetchSize(MAX_STATES_TO_FETCH);
            try(ResultSet rs = fetchNewStateStmt.executeQuery()) {
                while (rs.next()) {
                    BoardState state = sampleState.newState(rs.getLong(1), rs.getString(2));
                    states.add(state);
                }
            } catch (SQLException sqe) {
                commitTran = false;
                throw sqe;
            } finally {
                // End the transaction even if it's only a read only transaction.
               endTransaction(commitTran);
            }

            LOGGER.info("fetched " + states.size() + " states.");
            return states;
        }

        /**
         * Check if the given moves the given starting state are recorded in the database.
         * @param startingState, starting board state on which the moves i applied
         * @param moves distinct moves to check
         * @return true if all the moves are recorded with the given starting state, with a valid resultant state. False oetherwise.
         * @throws SQLException
         */
        @VisibleForTesting
        boolean hasMoves(BoardState startingState, List<BoardMove> moves) throws SQLException {

            String movesStr = moves.stream()
                                    .map(move -> quoteString(move.getDesc()))
                                    .collect(Collectors.joining(","));
            // count(distinct) is used to avoid counting a state twice in case the same move is recorded twice in the database
            // (which should not happen because of unique constraint on the table. But nonetheless used as protection.
            // The NOT NULL constraint on 
            String countMovesQuery = "SELECT count(distinct move_desc) FROM " + movesTableName + " WHERE move_desc IN (" +
                    movesStr + ") AND start_state = " + startingState.getId();
            try (Statement countStmt = conn.createStatement();
                    ResultSet rs = countStmt.executeQuery(countMovesQuery)) {
                if (!rs.next()) {
                    throw new IllegalStateException("Query " + countMovesQuery + " should always return at least one row.");
                }
                return rs.getLong(1) == moves.size();
            }
        }

        @Override
        public void close() throws SQLException {
            try {
                searchStateStmt.close();
                insertStateStmt.close();
                fetchNewStateStmt.close();
                fetchQueuedStateStmt.close();
                updateStateProcStmt.close();
                insertMoveStmt.close();
            } finally {
                conn.close();
            }
        }
    }
}

