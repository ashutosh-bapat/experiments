package com.ashutosh.BrainVitae;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class BVBoardMoveTest {
    private static final Random random = new Random();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBVBoardMove() throws Exception {
        BVCell cell = new BVCell(random.nextInt(), random.nextInt());
        BVDirection dir = new BVDirection(BVDirLabel.LEFT);
        BVBoardMove move = new BVBoardMove(cell, dir);
        Assert.assertEquals(dir, move.getDirection());
        Assert.assertEquals(cell, move.getStartBVCell());
        Assert.assertEquals(cell.toString() + " " + dir.toString(), move.getDesc());
    }
}