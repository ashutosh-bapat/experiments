package com.ashutosh.BoardSolver;

import java.util.List;

public interface BoardState {

    /**
     * @return all possible moves in a given board state
     */
    List<BoardMove> getPossibleMoves();

    /**
     * @param move, move to apply
     * @return the state resulting from applying given move on the current state.
     */
    BoardState apply(BoardMove move);

    /**
     * @return id of the state
     */
    long getId();

    /**
     * @param id, set id of the current state to the given id.
     */
    void setId(long id);

    /**
     * @return string description of the state.
     */
    String getDesc();

    /**
     * This method is used to construct an object of a derived BoardState class from the given description. Ideally we
     * should be using some form of object factory, or invoke a constructor given the class.
     * @param stateDesc, string description of state
     * @return BoardState representing the given description
     */
    BoardState newState(long id, String stateDesc);

    /**
     * Is the given state the final board state indicating a successful game?
     * @return
     */
    boolean isFinalState();
}
