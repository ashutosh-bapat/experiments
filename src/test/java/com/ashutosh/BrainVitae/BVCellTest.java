package com.ashutosh.BrainVitae;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class BVCellTest {
    private final int NUM_ITER = 100;
    private final int MAX_RAND_VAL = 1000;
    private final Random rand = new Random();

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSimpleConstructor() {
        for (int i = 0; i < NUM_ITER; i++) {
            int row = rand.nextInt(MAX_RAND_VAL);
            int col = rand.nextInt(MAX_RAND_VAL);
            BVCell bvCell = new BVCell(row, col);
            Assert.assertEquals(row, bvCell.getRow());
            Assert.assertEquals(col, bvCell.getCol());
            Assert.assertEquals("(" + row + ", " + col + ")", bvCell.toString());
        }
    }

    @Test
    public void testOffsetConstructor() {
        for (int i = 0; i < NUM_ITER; i++) {
            int row = rand.nextInt(MAX_RAND_VAL);
            int col = rand.nextInt(MAX_RAND_VAL);
            int rowOff = rand.nextInt(MAX_RAND_VAL);
            int colOff = rand.nextInt(MAX_RAND_VAL);

            // Generate negative offsets as well.
            if (rand.nextBoolean()) {
                rowOff = 0 - rowOff;
            }
            if (rand.nextBoolean()) {
                colOff = 0 - colOff;
            }
            BVCell bvCell = new BVCell(new BVCell(row, col), new BVOffset(rowOff, colOff));
            Assert.assertEquals(row + rowOff, bvCell.getRow());
            Assert.assertEquals(col + colOff, bvCell.getCol());
            Assert.assertEquals("(" + (row + rowOff) + ", " + (col + colOff) + ")", bvCell.toString());
        }
    }
}