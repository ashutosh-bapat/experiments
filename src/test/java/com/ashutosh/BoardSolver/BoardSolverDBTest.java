package com.ashutosh.BoardSolver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardSolverDBTest {
    private final static String dbUrl = "jdbc:postgresql://localhost/boardgames";
    private final static String dbUser = "boardgames";
    private final static String dbPassword = "boardgames";
    private final static String statesTableName = "states";
    private final static String movesTableName = "moves";
    private final static String dbSchema = BoardSolverDBTest.class.getSimpleName().toLowerCase();
    private final static BoardSolverDB db = new BoardSolverDB(dbUrl, dbUser, dbPassword, statesTableName, movesTableName,
                                                                dbSchema);

    @BeforeClass
    public static void setUpClass() throws Exception {
        db.createObjects();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        db.dropObjects();
    }

    @After
    public void tearDownTest() throws Exception {
        db.clearObjects();
    }

    @Test
    public void testDbOperations() throws Exception {
        long id = 1000000;
        final SampleBoardState initialSampleState = new SampleBoardState();

        try (BoardSolverDB.BSDCConn bsdbConn = db.getConnection()) {
            // Insert a new state with a given id, state should be updated with the given id
            bsdbConn.searchAndInsertState(initialSampleState, id);
            Assert.assertEquals(id, initialSampleState.getId());

            // Try inserting the same state with a different id
            long newId = id + 100;
            bsdbConn.searchAndInsertState(initialSampleState, newId);
            Assert.assertEquals(id, initialSampleState.getId());
            Assert.assertNotEquals(newId, initialSampleState.getId());

            // Try inserting a cloned state with a different id, we should get the same id back
            SampleBoardState copyState = new SampleBoardState(initialSampleState);
            bsdbConn.searchAndInsertState(copyState, newId);
            Assert.assertEquals(id, copyState.getId());
            Assert.assertNotEquals(newId, copyState.getId());

            // Test applyMoves
            List<BoardMove> moves = initialSampleState.getPossibleMoves();
            Map<BoardMove, BoardState> movesToStates = new HashMap<>(moves.size());
            for (BoardMove move : moves) {
                BoardState result = initialSampleState.apply(move);
                newId = newId + 100;
                bsdbConn.searchAndInsertState(result, newId);
                movesToStates.put(move, result);
            }
            bsdbConn.addMoves(initialSampleState, movesToStates);

            // The initial state should be processed now. Every move would produce a new state. Hence number of unprocessed
            // states should be same as the number of moves.
            Assert.assertEquals(moves.size(), bsdbConn.readUnprocessedStates(initialSampleState).size());

            // There shouldn't be any unprocessed states now.
            Assert.assertEquals(0, bsdbConn.readUnprocessedStates(initialSampleState).size());

            // All the applied moves should be in the database
            Assert.assertTrue(bsdbConn.hasMoves(initialSampleState, moves));
        }
    }
}