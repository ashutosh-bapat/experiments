package com.ashutosh.BoardSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleBoardState implements BoardState {

    private int state;
    private final static int finalState = 10;
    private long id;

    public SampleBoardState() {
        this.id = 0;
        this.state = 1;
    }

    public SampleBoardState(int state) {
        this.id = 0;
        this.state = state;
    }

    SampleBoardState(SampleBoardState other) {
        this.id = 0;
        this.state = other.state;
    }

    public static Long numSolutions() {
        return numPaths(1, finalState);
    }

    private static Long numPaths(int start, int end) {
        assert end != start;
        if (end == start + 1) {
            return 1L;
        }

        long numPaths = 0L;
        // Number of paths from start to end is sum of product of number of paths from start to a midpoint and the number of
        // paths from the midpoint to end for each of the midpoints + one path from start to the end without any midpoint.
        for (int i = start + 1; i < end; i++) {
            numPaths = numPaths + numPaths(i, end);
        }
        return numPaths + 1;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public List<BoardMove> getPossibleMoves() {
        List<BoardMove> result = new ArrayList<>(finalState - state);

        for (int cnt = 1; state + cnt <= finalState; cnt++) {
           SampleBoardMove move = new SampleBoardMove(cnt);
           result.add(move);
        }
        return result;
    }

    @Override
    public BoardState apply(BoardMove move) {
        if (!(move instanceof SampleBoardMove)) {
            throw new IllegalArgumentException("Given move is not of type " + SampleBoardMove.class.getCanonicalName());
        }

        return apply((SampleBoardMove) move);
    }

    SampleBoardState apply(SampleBoardMove move) {
        SampleBoardState newState = new SampleBoardState(this);
        newState.state = this.state + move.getInc();
        return newState;
    }

    @Override
    public String getDesc() {
        return Integer.toString(state);
    }

    @Override
    public BoardState newState(long id, String stateDesc) {
        SampleBoardState result = new SampleBoardState(Integer.valueOf(stateDesc));
        result.setId(id);
        return result;
    }

    @Override
    public boolean isFinalState() {
        return state == finalState;
    }
}
