package com.ashutosh.BoardSolver;

public class SampleBoardMove implements BoardMove {
    private int inc;

    SampleBoardMove(int inc) {
        this.inc = inc;
    }

    @Override
    public String getDesc() {
        return Integer.toString(inc);
    }

    public int getInc() {
        return inc;
    }
}
