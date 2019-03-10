package com.ashutosh.BoardSolver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class StateProcessorTest {
    private final static String dbUrl = "jdbc:postgresql://localhost/boardgames";
    private final static String dbUser = "boardgames";
    private final static String dbPassword = "boardgames";
    private final static String statesTableName = "states";
    private final static String movesTableName = "moves";
    private final static String dbSchema = StateProcessorTest.class.getSimpleName().toLowerCase();
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
    public void testProcess() throws SQLException {
        try (BoardSolverDB.BSDCConn dbConn = db.getConnection()) {
            long initId = 100000;
            AtomicLong counter = new AtomicLong(initId);
            StateProcessor sp = new StateProcessor(db, counter);
            SampleBoardState sbs = new SampleBoardState(1);

            // Let method process add moves and resultant states to DB.
            dbConn.searchAndInsertState(sbs, counter.getAndIncrement());
            sp.process(sbs, dbConn);

            // Get the moves and states outside process and compare those with the result
            List<BoardMove> moves = sbs.getPossibleMoves();
            long unusedId = counter.get();
            dbConn.hasMoves(sbs, moves);
            // TODO: We should search all the states in a single call
            for (BoardMove move : moves) {
                BoardState resultState = sbs.apply(move);
                dbConn.searchAndInsertState(resultState, unusedId);
                // The state should be found in the database and with id of our choice.
                Assert.assertTrue(resultState.getId() >= initId && resultState.getId() < unusedId);
            }
        }
    }

    @Test
    public void testRunAndWaiting() throws SQLException, InterruptedException {
        AtomicLong idCounter = new AtomicLong(1);
        BlockingQueue<BoardState> statesQ = new ArrayBlockingQueue<>(3);
        StateProcessor stateProcessor = new StateProcessor(db, statesQ, "testthread1", idCounter);
        stateProcessor.start();

        // Sleep for a small amount of time so that the thread tries to fetch from the queue.
        Thread.sleep(10000);
        // There's no state in the queue, so the thread should be waiting now.
        Assert.assertTrue(stateProcessor.isWaitingForNextState());
        SampleBoardState sbs = new SampleBoardState(1);

        // Let method process add moves and resultant states to DB.
        try (BoardSolverDB.BSDCConn dbConn = db.getConnection()) {
            dbConn.searchAndInsertState(sbs, idCounter.getAndIncrement());

            // Fill the queue a couple of times and make sure that it gets drained
            for (int i = 0; i < 2; i++) {
                LinkedList<BoardState> unprocessedStates = dbConn.readUnprocessedStates(sbs);
                for (BoardState state : unprocessedStates) {
                    statesQ.put(state);
                }
                // Sleep till the state processor thread empties the queue
                Thread.sleep(10000);
                Assert.assertEquals(0, statesQ.size());
                // There's no state in the queue, so the thread should be waiting now.
                Assert.assertTrue(stateProcessor.isWaitingForNextState());
            }
        }
        // At the end interrupt the state processor so that it ends execution, wait for a small duration for it to end itself.
        stateProcessor.interrupt();
        Thread.sleep(10);
        Assert.assertEquals(Thread.State.TERMINATED, stateProcessor.getState());
        stateProcessor.join();
    }
}