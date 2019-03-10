package com.ashutosh.BrainVitae;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class BVOffsetTest {
    private final static Random random = new Random();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBVOffset() throws Exception {
        int row = random.nextInt();
        int col = random.nextInt();

        // We need different row and col values for the inequality test later.
        if (row == col) {
            row = row + 1;
        }
        BVOffset offset = new BVOffset(row, col);
        Assert.assertEquals(row, offset.getRowOff());
        Assert.assertEquals(col, offset.getColOff());
        Assert.assertEquals(offset, new BVOffset(row, col));
        Assert.assertNotEquals(offset, new BVOffset(col, row));
    }
}