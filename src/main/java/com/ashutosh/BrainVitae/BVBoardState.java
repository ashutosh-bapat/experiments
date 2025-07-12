package com.ashutosh.BrainVitae;

import com.ashutosh.BoardSolver.BoardMove;
import com.ashutosh.BoardSolver.BoardState;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * State of BrainVitae board.
  */
//
// At any time during play, the state of board can be indicated by the state of
// each hole - whether it's unused, filled or empty. Certain holes near the four
// corners are never used and their state always remains "unused". Rest of the
// holes are either filled or empty. A 7 X 7 board looks like below at the start
// of the game. X indicates an unused hole. 0 indicates a filled hole and O
// indicates an empty hole.
//
// X X 0 0 0 X X
// X X 0 0 0 X X
// 0 0 0 0 0 0 0
// 0 0 0 O 0 0 0
// 0 0 0 0 0 0 0
// X X 0 0 0 X X
// X X 0 0 0 X X
//
public class BVBoardState implements BoardState {
    private enum CellState {FILLED, EMPTY}

    final BrainVitaeBoard board;
    static private final BVDirection[] BVDIRECTIONS; // shared by all instances

    private CellState[][] boardState = null;

    private Long id = null;

    int getNumEmpty() {
        return numEmpty;
    }

    private int numEmpty = 0;

    int getNumFilled() {
        return numFilled;
    }

    private int numFilled = 0;

    // Initialize BVDIRECTIONS
    static
    {
        BVDirLabel[] dirlabels = BVDirLabel.values();
        int			cnt;

        BVDIRECTIONS = new BVDirection[dirlabels.length];

        cnt = 0;
        for (BVDirLabel dirlabel:dirlabels)
        {
            BVDIRECTIONS[cnt] = new BVDirection(dirlabel);
            cnt++;
        }

        assert cnt == BVDIRECTIONS.length : "expected " + BVDIRECTIONS.length + " BVDIRECTIONS but found " + cnt;
    }

    /**
     * Gives initial state of a board for a given board.
     *
     * The initial state is all the holes filled except the middle one.
     *
     * @param board underlying board on which the initial state is to be constructed
     */
    BVBoardState(BrainVitaeBoard board) {
        this.board = board;

        boardState = new CellState[board.getBoardSize()][board.getBoardSize()];

        int midpoint = board.getBoardSize() / 2;
        for (int row = 0; row < board.getBoardSize(); row++) {
            for (int col = 0; col < board.getBoardSize(); col++) {
                BVCell cell = new BVCell(row, col);

                if (!board.isUsed(cell))
                    continue;

                if (row == midpoint && col == midpoint) {
                    emptyCell(cell, false);
                } else {
                    fillCell(cell, false);
                }
            }
        }
    }

    /**
     * Copies the given state of board into another instance.
     *
     * The constructor is usually used to apply a move to a given board state.
     * @param other board state to be copied
     */
    BVBoardState(@org.jetbrains.annotations.NotNull BVBoardState other) {
        this.board = other.board;
        this.id = null; // to be calculated later if required
        boardState = new CellState[board.getBoardSize()][board.getBoardSize()];
        for (int row = 0; row < board.getBoardSize(); row++) {
            for (int col = 0; col < board.getBoardSize(); col++) {
                BVCell cell = new BVCell(row, col);
                switch (other.getCellState(cell)) {
                    case EMPTY:
                        emptyCell(cell, false);
                        break;
                    case FILLED:
                        fillCell(cell, false);
                        break;
                    default:
                        throw new IllegalStateException("Illegal state " + other.getCellState(cell) + " found.");
                }
            }
        }

        assert (this.numEmpty == other.numEmpty && this.numFilled == other.numFilled);
    }

    /**
     * Construct board state for the given Id.
     *
     * This is inverse of getId() i.e. BVBoardState(board, otherBoardState.getId()) == otherBoardState.
     * @param board
     * @param id
     */
    BVBoardState(BrainVitaeBoard board, long id)
    {
        this.id = id;
        this.board = board;
        BigInteger idBitSet = BigInteger.valueOf(id);
        boardState = new CellState[board.getBoardSize()][board.getBoardSize()];
        for (int crow = 0; crow < board.getBoardSize(); crow++)
        {
            for (int ccol = 0; ccol < board.getBoardSize(); ccol++)
            {
                BVCell cell = new BVCell(crow, ccol);
                if (idBitSet.testBit(board.getIndex(cell)))
                {
                    fillCell(new BVCell(crow, ccol), false);
                }
                else
                {
                    emptyCell(new BVCell(crow, ccol), false);
                }
            }
        }
    }

    /**
     * @return id of the state
     */
    @Override
    public long getId() {
        if (id != null)
            return id;

        BigInteger idBitSet = BigInteger.valueOf(0);

        for (int row = 0; row < board.getBoardSize(); row++) {
            for (int col = 0; col < board.getBoardSize(); col++) {
                BVCell cell = new BVCell(row, col);
                int cellIndex = board.getIndex(cell);
                switch (getCellState(cell))
                {
                    case EMPTY:
                        idBitSet.clearBit(cellIndex);
                        break;
                    case FILLED:
                        idBitSet.setBit(cellIndex);
                }
            }
        }
        id = idBitSet.longValue();
        return id;
    }

    // Apply the given move to the given board state and return the resultant
    // board state as the newly constructed board
    @Override
    public BoardState apply(BoardMove move) {
        if (!(move instanceof BVBoardMove)) {
           throw new IllegalArgumentException("Expecting a " + BVBoardMove.class.getName() + " but received a " +
                                                move.getClass().getName());
        }
       return apply((BVBoardMove) move);
    }

    /**
     * @return all possible moves in a given board state
     */
    @Override
    public List<BoardMove> getPossibleMoves() {
        List<BoardMove> possMoves = new LinkedList<>();
        int			row;
        int			col;

        // For every filled cell in the board, check every direction for a
        // possible move. A move in a given direction is possible if
        // 1. the cell next to the given cell in the given direction is filled
        // 2. the cell next to the cell in step 1 is empty
        for (row = 0; row < board.getBoardSize(); row++)
        {
            for (col = 0; col < board.getBoardSize(); col++)
            {
                BVCell curcell = new BVCell(row, col);

                if (isFilledCell(curcell))
                {
                    for (BVDirection dir: BVDIRECTIONS)
                    {
                        BVCell nextBVCell = new BVCell(curcell, dir.getNextOffset());
                        BVCell jumpBVCell = new BVCell(curcell, dir.getJumpOffset());

                        if (isFilledCell(nextBVCell) && isEmptyCell(jumpBVCell)) {
                            possMoves.add(new BVBoardMove(curcell, dir));
                        }
                    }
                }
            }
        }
        return possMoves;
    }

    /**
     * @return string description of the state.
     */
    @Override
    public String getDesc() {
        String		out = new String();
        int			row;
        int			col;
        String		sep;

        sep = new String("[");
        for (row = 0; row < board.getBoardSize(); row++)
        {
            sep = sep + "[";
            for (col = 0; col < board.getBoardSize(); col++)
            {
                out = out + sep + getCellState(new BVCell(row, col)).toString();
                sep = ", ";
            }
            sep = "], ";
        }

        out = out + "]]";
        return out;
    }


    /**
     * This method is used to construct an object of a derived BoardState class from the given description. Ideally we
     * should be using some form of object factory, or invoke a constructor given the class.
     * @param stateDesc, string description of state
     * @return BoardState representing the given description
     */
    @Override
    public BoardState newState(long id, String stateDesc) {
        return new BVBoardState(board, id);
    }

    /**
     * Fill the given cell on the board
     * @param cell - cell to be filled
     * @param checkState - if false, the board is being populated. Thus the given cell may be filled or empty or
     *                    uninitialized, just set it as filled. Otherwise, the cell being filled should be emptied.
     */
    private void fillCell(BVCell cell, boolean checkState) {
        // If the cell is empty, decrement the number of empty cells.
        if (isEmptyCell(cell)) {
            numEmpty--;
        } else if (isFilledCell(cell) || getCellState(cell) == null) {
            // If the cell is filled or in an uninitialized state
            if (checkState) {
                throw new IllegalStateException("Cell " + cell.toString() + " is " + getCellState(cell) + " and can not be filled.");
            }
            if (isFilledCell(cell)) {
                // If the cell is already filled no action is required.
                return;
            }
        }

        // Mark the cell filled, increment the counter
        boardState[cell.getRow()][cell.getCol()] = CellState.FILLED;
        numFilled++;
        id = null; // state changed so id needs to be calculate again if required
    }

    /**
     * Empty the given cell on the board
     * @param cell - cell to be emptied
     * @param checkStates - if false, the board is being populated. Thus the given cell may be filled or empty or
     *                    uninitialized, just set it as empty. Otherwise, the cell being emptied should be filled.
     */
    private void emptyCell(BVCell cell, boolean checkStates) {
        // If the cell is filled, decrement the number of filled cells.
        if (isFilledCell(cell)) {
            numFilled--;
        } else if (isEmptyCell(cell) || getCellState(cell) == null) {
            // If the cell is empty or in an uninitialized state
            if (checkStates) {
                throw new IllegalStateException("Cell " + cell.toString() + " is " + getCellState(cell) + " and can not be emptied.");
            }
            if (isEmptyCell(cell)) {
                // If the cell is already empty no action is required.
                return;
            }
        }

        // Mark the cell empty, increment the counter
        boardState[cell.getRow()][cell.getCol()] = CellState.EMPTY;
        numEmpty++;
        id = null; // state changed so id needs to be calculate again if required
    }

    // The board can be considered to have unused cells beyond the board size.
    // This allows us to avoid another method isValidCell() and call it
    // everywhere
    private CellState getCellState(BVCell cell)
    {
        int			row = cell.getRow();
        int			col = cell.getCol();
        if (row >= 0 && row < board.getBoardSize() && col >= 0 && col < board.getBoardSize())
            return boardState[row][col];
        throw new IllegalArgumentException("invalid cell " + cell);
    }

    boolean isEmptyCell(BVCell BVCell)
    {
        return getCellState(BVCell) == CellState.EMPTY;
    }

    boolean isFilledCell(BVCell BVCell) {
        return getCellState(BVCell) == CellState.FILLED;
    }

    private BVBoardState apply(BVBoardMove move) {
        // Copy the current state and apply given move.
        BVBoardState result = new BVBoardState(this);
        // The resultant state's id is unknown. Wipe out the input state's id.
        result.id = null;

        BVCell startCell = move.getStartBVCell();
        BVDirection dir = move.getDirection();
        BVCell jumpCell = new BVCell(startCell, dir.getJumpOffset());
        BVCell nextCell = new BVCell(startCell, dir.getNextOffset());

        // A move constitutes moving the peg in the starting cell to the target
        // cell (jumpCell) over the cell in the middle of those two, emptying
        // the starting and the middle cell and filling the target cell.
        result.emptyCell(startCell, true);
        result.fillCell(jumpCell, false);
        result.emptyCell(nextCell, true);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof  BVBoardState)) {
            return false;
        }

        return equals((BVBoardState) o);
    }

    /**
     * We do not compare other.id since that may not be assigned when comparison happens.
     *
     * @param other
     * @return true if other is logically equivalent to this
     */
    private boolean equals(BVBoardState other) {
        if (board != other.board) {
            return false;
        }

        if (numEmpty != other.numEmpty) {
            return false;
        }

        if (numFilled != other.numFilled) {
            return false;
        }

        if (boardState.length != other.boardState.length) {
            return false;
        }

        if (this.id != null && other.id != null && this.id != other.id)
            return false;

        for (int cnt = 0; cnt < boardState.length; cnt++) {
            if (!Arrays.equals(boardState[cnt], other.boardState[cnt])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isFinalState() {
        return numFilled == 1;
    }
}
