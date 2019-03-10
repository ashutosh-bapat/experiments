package com.ashutosh.BrainVitae;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BVDirectionTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBVDirection() {
       for (BVDirLabel label : BVDirLabel.values()) {
          BVDirection bvDir = new BVDirection(label);
          Assert.assertEquals(bvDir.getJumpOffset().getColOff(), 2 * bvDir.getNextOffset().getColOff());
          Assert.assertEquals(bvDir.getJumpOffset().getRowOff(), 2 * bvDir.getNextOffset().getRowOff());

          // Given the above assertion it suffices to check just one of the offsets.
           switch (label) {
               case UP:
                   Assert.assertEquals(new BVOffset(-2, 0), bvDir.getJumpOffset());
                   break;

               case DOWN:
                   Assert.assertEquals(new BVOffset(2, 0), bvDir.getJumpOffset());
                   break;

               case LEFT:
                   Assert.assertEquals(new BVOffset(0, -2), bvDir.getJumpOffset());
                   break;

               case RIGHT:
                   Assert.assertEquals(new BVOffset(0, 2), bvDir.getJumpOffset());
                   break;

               default:
                   throw new IllegalArgumentException("Unknown direction " + label);
           }
           Assert.assertEquals(bvDir.toString(), label.toString());
       }
    }
}