package com.ashutosh.BrainVitae;

import com.ashutosh.BoardSolver.BoardMove;

/**
 * A move defines in which direction a peg at starting position moves. A move takes the board from one state to the other.
 */
public class BVBoardMove implements BoardMove {
    private BVCell startBVCell;
    private BVDirection dir;

    public BVBoardMove(BVCell BVCell, BVDirection indir)
    {
        startBVCell = BVCell;
        dir = indir;
    }

    @Override
    public String getDesc() {
        return startBVCell.toString() + " " + dir.toString();
    }

    BVCell getStartBVCell() {
        return startBVCell;
    }

    BVDirection getDirection() {
        return dir;
    }
}
