package com.ashutosh.BoardSolver;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class StateProcessor extends Thread {
    private static final Logger LOGGER = Logger.getLogger(StateProcessor.class.getName());
    private BlockingQueue<BoardState> statesQ;
    private BoardSolverDB db;
    private AtomicLong idCounter;
    private AtomicBoolean waitingForNextState = new AtomicBoolean();

    public StateProcessor(BoardSolverDB db, BlockingQueue<BoardState> statesQ, String name, AtomicLong idCounter) {
        super(name);
        this.db = db;
        this.statesQ = statesQ;
        this.idCounter = idCounter;
    }

    public StateProcessor(BoardSolverDB db, AtomicLong idCounter) {
        this.db = db;
        this.idCounter = idCounter;
    }

    boolean isWaitingForNextState() {
        // There is a small time window when thread is not waiting waitingForNextState is true. So check the actual
        // state of thread as well.
        return waitingForNextState.get() && !getState().equals(State.RUNNABLE);
    }

    /**
     * Get moves possible on the given state of board and apply them to produce resultant state. The moves and the
     * resultant states added to the database using the given connection.
     * @param state the state to process, should be added to the database already. Otherwise, throws fk violation
     * @param dbConn the database connection to use
     * @throws SQLException
     */
    public void process(BoardState state, BoardSolverDB.BSDCConn dbConn) throws SQLException {
        List<BoardMove> moves = state.getPossibleMoves();
        Map<BoardMove, BoardState> newStates = new HashMap<>();
        long id = idCounter.getAndIncrement();

        for (BoardMove move : moves) {
           BoardState newState = state.apply(move);
           dbConn.searchAndInsertState(newState, id);

           // Update state id counter if current id is used.
           if (newState.getId() == id) {
               id = idCounter.getAndIncrement();
           }
           newStates.put(move, newState);
        }

        dbConn.addMoves(state, newStates);
    }

    private BoardState getNextState() throws InterruptedException {
        waitingForNextState.set(true);
        BoardState state = statesQ.take();
        waitingForNextState.set(false);
        return state;
    }

    // Each state processor thread pulls one state at a time and processes it till it gets a "last" state indicating
    // that there are no more states available for processing. The "last" state is indicated by an invalid state.
    public void run()
    {
        BoardState curState;
        long nprocessed = 0;
        final long REPORTING_PERIOD = 100000;
        Exception e = null;
        waitingForNextState.set(false);

        LOGGER.info("Started thread " + super.getName());
        try (BoardSolverDB.BSDCConn dbConn = db.getConnection())
        {
            while ((curState = getNextState()) != null)
            {
                nprocessed++;
                process(curState, dbConn);

                if (nprocessed % REPORTING_PERIOD == 0) {
                    LOGGER.info("completed processing " + nprocessed + " states.");
                }
            }
        } catch (InterruptedException ie) {
            LOGGER.info("got interrupted with message " + ie.getMessage() + ". Exiting.");
            LOGGER.info("finished after processing " + nprocessed + " states");
            return;
        } catch (SQLException sqle) {
            LOGGER.severe("received SQL exception " + sqle.getMessage() + ". Exiting.");
            e = sqle;
        }
    }
};
