package com.ashutosh.BrainVitae;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class BrainVitaeBoardTest extends TestCase {

    @Test
    public void testBVBoard7() throws IllegalArgumentException {
        final int BOARD_SIZE = 7;
        BrainVitaeBoard board = new BrainVitaeBoard(BOARD_SIZE);
        int i;
        int j;
        int expIndex = 0;

        // 2 * 2 corners are unused
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 2; j++) {
                Assert.assertFalse(board.isUsed(new BVCell(i, j)));
                Assert.assertFalse(board.isUsed(new BVCell(BOARD_SIZE - i - 1, j)));
                Assert.assertFalse(board.isUsed(new BVCell(i, BOARD_SIZE - j - 1)));
                Assert.assertFalse(board.isUsed(new BVCell(BOARD_SIZE - i - 1, BOARD_SIZE - j - 1)));
            }
        }

        for (i = 0; i <= 1; i++) {
            for (j = 2; j <= 4; j++) {
                BVCell cell = new BVCell(i, j);

                Assert.assertTrue(board.isUsed(cell));
                Assert.assertEquals(expIndex++, board.getIndex(cell));
            }
        }

        for (i = 2; i <= 4; i++) {
            for (j = 0; j < BOARD_SIZE; j++) {
                BVCell cell = new BVCell(i, j);

                Assert.assertTrue(board.isUsed(cell));
                Assert.assertEquals(expIndex++, board.getIndex(cell));
            }
        }

        for (i = 5; i < BOARD_SIZE; i++) {
            for (j = 2; j <= 4; j++) {
                BVCell cell = new BVCell(i, j);

                Assert.assertTrue(board.isUsed(cell));
                Assert.assertEquals(expIndex++, board.getIndex(cell));
            }
        }

        Assert.assertEquals(BOARD_SIZE * BOARD_SIZE - 16, expIndex);
        Assert.assertEquals(expIndex, board.numUsedCells);
    }
}