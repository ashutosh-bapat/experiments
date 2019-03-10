package com.ashutosh.BoardSolver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class BoardSolverTest {
    private final static String dbUrl = "jdbc:postgresql://localhost/boardgames";
    private final static String dbUser = "boardgames";
    private final static String dbPassword = "boardgames";
    private final static String statesTableName = "states";
    private final static String movesTableName = "moves";
    private final static String dbSchema = BoardSolverTest.class.getSimpleName().toLowerCase();
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

    private void testBoardSolver(BoardSolver solver, BoardState initialState) throws Exception {
        solver.solve();
        Map<String, BoardState>  solutions = db.findSolutions(initialState);
        Assert.assertEquals(SampleBoardState.numSolutions(), Long.valueOf(solutions.size()));
        for (BoardState finalState : solutions.values()) {
            Assert.assertTrue(finalState.isFinalState());
        }
    }

    @Test
    public void testSingleThreadedBoardSolver() throws Exception {
        SampleBoardState initialState = new SampleBoardState();
        BoardSolver solver = new BoardSolver(1, initialState, db);
        testBoardSolver(solver, initialState);
    }

    @Test
    public void testMultiThreadedBoardSolver() throws Exception {
        for (int numThreads = 2; numThreads < 5; numThreads++) {
            SampleBoardState initialState = new SampleBoardState();
            BoardSolver solver = new BoardSolver(2, initialState, db);
            testBoardSolver(solver, initialState);
        }
    }
}