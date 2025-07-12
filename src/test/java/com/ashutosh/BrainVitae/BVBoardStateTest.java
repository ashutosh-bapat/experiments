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
    private static final BrainVitaeBoard board = new BrainVitaeBoard(7);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    private void testBVBoardState(BVBoardState state) {
        int size = state.board.getBoardSize();
        // A strip of vertical and horizontal three cells around median can be filled or empty. Rest are unused.
        int numCellsInVertMiddleStrip = board.FILLED_STRIP_WIDTH * size;
        int numCellsInHoriMiddleStrip = board.FILLED_STRIP_WIDTH * size;
        int numVertHorCommonCells = board.FILLED_STRIP_WIDTH * state.board.FILLED_STRIP_WIDTH;
        int numTotalCells = numCellsInVertMiddleStrip + numCellsInHoriMiddleStrip - numVertHorCommonCells;
        Assert.assertEquals(numTotalCells, state.getNumFilled() + state.getNumEmpty());


        // state descriptor when fed back results in the same state
        Assert.assertEquals(state, new BVBoardState(state.board, state.getId()));

        // test copy constructor
        Assert.assertEquals(state, new BVBoardState(state));
    }

    @Test
    public void testClassConstants() {

        // For now only boards with 3 cell wide strips filled are supported.
        Assert.assertEquals(3, board.FILLED_STRIP_WIDTH);

        // The width of the strip should always be odd since we support odd sizes only.
        Assert.assertEquals(1, board.FILLED_STRIP_WIDTH % 2);
    }

    @Test
    public void testInitialState() {
        for (int size = 7; size <= 9; size = size + 2) {
            BrainVitaeBoard board = new BrainVitaeBoard(size);
            BVBoardState state = new BVBoardState(board);
            // An initial state has middle cell empty.
            Assert.assertEquals(1, state.getNumEmpty());
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    BVCell cell = new BVCell(row, col);
                    if (cell.equals(board.midcell)) {
                        Assert.assertTrue(state.isEmptyCell(cell));
                    } else if (board.withinFilledStrip(cell)) {
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

    // Test board states with some random-ness checking the properties of a random board state.
    // We do this by starting with the initial state and applying one of the possible moves randomly and repeating this
    // on the resulting state till there are no possible moves. This is fine since BrainVitae state graphs are DAGs.
    @Test
    public void testRandomState() {
        testApplyMove(new BVBoardState(board));
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