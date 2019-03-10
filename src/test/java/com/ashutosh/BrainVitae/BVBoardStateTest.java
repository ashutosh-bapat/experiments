package com.ashutosh.BrainVitae;

import com.ashutosh.BoardSolver.BoardMove;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class BVBoardStateTest {
    private static final Random random = new Random();
    private static final Logger LOGGER = Logger.getLogger(BVBoardStateTest.class.getName());
    private static final double fracMovesToTest = 0.2;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    private void testBVBoardState(BVBoardState state) {
        int size = state.getBoardSize();
        // A strip of vertical and horizontal three cells around median can be filled or empty. Rest are unused.
        int numCellsInVertMiddleStrip = BVBoardState.FILLED_STRIP_WIDTH * size;
        int numCellsInHoriMiddleStrip = BVBoardState.FILLED_STRIP_WIDTH * size;
        int numVertHorCommonCells = BVBoardState.FILLED_STRIP_WIDTH * BVBoardState.FILLED_STRIP_WIDTH;
        int numTotalCells = numCellsInVertMiddleStrip + numCellsInHoriMiddleStrip - numVertHorCommonCells;
        Assert.assertEquals(numTotalCells, state.getNumFilled() + state.getNumEmpty());

        long id = random.nextLong();
        // get and set id agree
        state.setId(id);
        Assert.assertEquals(id, state.getId());

        // state descriptor when fed back results in the same state
        Assert.assertEquals(state, new BVBoardState(state.getId(), state.getDesc()));

        // test copy constructor
        Assert.assertEquals(state, new BVBoardState(state));
    }

    @Test
    public void testClassConstants() {
        // For now only boards with 3 cell wide strips filled are supported.
        Assert.assertEquals(3, BVBoardState.FILLED_STRIP_WIDTH);

        // The width of the strip should always be odd since we support odd sizes only.
        Assert.assertEquals(1, BVBoardState.FILLED_STRIP_WIDTH % 2);
    }

    @Test
    public void testInitialState() {
        for (int size = 7; size <= 9; size = size + 2) {
            int midpoint = size / 2;
            BVBoardState state = new BVBoardState(size);
            // An initial state has middle cell empty.
            Assert.assertEquals(1, state.getNumEmpty());
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    BVCell cell = new BVCell(row, col);
                    if (row == midpoint && col == midpoint) {
                        Assert.assertTrue(state.isEmptyCell(cell));
                    } else if (BVBoardState.withinFilledStrip(row, midpoint) ||
                                BVBoardState.withinFilledStrip(col, midpoint)) {
                        Assert.assertTrue(state.isFilledCell(cell));
                    } else {
                        Assert.assertFalse(state.isFilledCell(cell));
                        Assert.assertFalse(state.isEmptyCell(cell));
                    }
                }
            }
            testBVBoardState(state);
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void testInvalidBoardSizes() {
        // Any even sized board is invalid
        BVBoardState state = new BVBoardState(random.nextInt() * 2);

        // Any odd value less than filled width size should is invalid
        for (int i = 1; i <= BVBoardState.FILLED_STRIP_WIDTH; i = i + 2) {
            state = new BVBoardState(i);
        }
    }

    // Test board states with some random-ness checking the properties of a random board state.
    // We do this by starting with the initial state and applying one of the possible moves randomly and repeating this
    // on the resulting state till there are no possible moves. This is fine since BrainVitae state graphs are DAGs.
    @Test
    public void testRandomState() {
        testApplyMove(new BVBoardState(7));
    }

    private void testApplyMove(BVBoardState state) {
        testBVBoardState(state);
        List<BoardMove> moves = state.getPossibleMoves();
        if (moves.size() <= 0) {
            return;
        }

        // No move should be possible when there is only one cell filled.
        Assert.assertNotEquals(1, state.getNumFilled());

        int numMovesToTest = (int) Math.ceil(moves.size() * fracMovesToTest);
        for (int cnt = 0; cnt < numMovesToTest; cnt++) {
            // Select a random move and apply.
            // FIXME: This random-ness in the test should be fixed. Instead we should have fixed
            // starting states, with fixed set of moves testing corner and normal cases.
            BoardMove selectedMove = moves.get(random.nextInt(moves.size()));
            Assert.assertTrue(selectedMove instanceof  BVBoardMove);
            BVBoardMove bvMove = (BVBoardMove) selectedMove;
            BVBoardState newState = (BVBoardState) state.apply(selectedMove);
            LOGGER.info("Applied move " + selectedMove.getDesc() + " on state " + state.getDesc() + " resulting in state " + newState.getDesc());

            // Check that the state of resultant board.
            Assert.assertEquals(state.getNumEmpty() + 1, newState.getNumEmpty());
            Assert.assertEquals(state.getNumFilled() - 1, newState.getNumFilled());
            Assert.assertTrue(newState.isEmptyCell(bvMove.getStartBVCell()));
            Assert.assertTrue(newState.isFilledCell(new BVCell(bvMove.getStartBVCell(), bvMove.getDirection().getJumpOffset())));
            Assert.assertTrue(newState.isEmptyCell((new BVCell(bvMove.getStartBVCell(), bvMove.getDirection().getNextOffset()))));
            testApplyMove(newState);
        }
   }

   // TODO: Complete state for isFinalState. Right now there is no way we can create a final state without exposing
   // com.ashutosh.BrainVitae.BVBoardState.fillCell and com.ashutosh.BrainVitae.BVBoardState.emptyCell.
   @Test
    public void testFinalState() {
   }
}