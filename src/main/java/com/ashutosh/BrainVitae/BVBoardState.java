package com.ashutosh.BrainVitae;

import com.ashutosh.BoardSolver.BoardMove;
import com.ashutosh.BoardSolver.BoardState;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

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
    private enum CellState {UNUSED, FILLED, EMPTY};
    static private BVDirection BVDIRECTIONS[]; // shared by all instances

    static final int FILLED_STRIP_WIDTH=3;

    private CellState boardState[][] = null;

    int getBoardSize() {
        return boardSize;
    }

    private int boardSize = 0;
    // Even for a 9 * 9 sized board the number possible of states fits an 8 byte
    // long integer, even though the actual number of states may not fit an 8
    // byte long integer
    private long id = 0;

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
        BVDirLabel dirlabels[] = BVDirLabel.values();
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

    BVBoardState(int size) throws IllegalArgumentException {
        if (size % 2 != 1) {
            throw new IllegalArgumentException("invalid board size " + size + ". Size should be an odd integer.");
        }

        if (size < FILLED_STRIP_WIDTH) {
            throw new IllegalArgumentException("invalid board size " + size + ". Size should be at least " + FILLED_STRIP_WIDTH + ".");
        }

        boardSize = size;
        boardState = new CellState[size][size];
        id = 0;

        int midpoint = size / 2;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BVCell cell = new BVCell(row, col);
                if (row == midpoint && col == midpoint) {
                    emptyCell(cell, false);
                } else if (withinFilledStrip(row, midpoint) || withinFilledStrip(col, midpoint)) {
                    fillCell(cell, false);
                } else {
                    markCellUnused(cell);
                }
            }
        }
    }

    static boolean withinFilledStrip(int point, int midpoint) {
        int filledWidthAroundMidpoint = FILLED_STRIP_WIDTH / 2;
        if (point <= midpoint + filledWidthAroundMidpoint && point >= midpoint - filledWidthAroundMidpoint) {
            return true;
        }
        return false;
    }

    BVBoardState(BVBoardState other) {
        boardSize = other.boardSize;
        boardState = new CellState[boardSize][boardSize];
        id = other.id;
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                BVCell cell = new BVCell(row, col);
                switch (other.getCellState(cell)) {
                    case EMPTY:
                        emptyCell(cell, false);
                        break;
                    case FILLED:
                        fillCell(cell, false);
                        break;
                    case UNUSED:
                        markCellUnused(cell);
                        break;
                    default:
                        throw new IllegalStateException("Illegal state " + other.getCellState(cell) + " found.");
                }
            }
        }
    }

    BVBoardState(long id, String stateDesc)
    {
        String		strarr[];
        int			crow;
        int			ccol;

        this.id = id;
        strarr = stateDesc.substring(2, stateDesc.length() - 2).split(Pattern.quote("], ["));
        boardSize = strarr.length;
        boardState = new CellState[boardSize][boardSize];
        for (crow = 0; crow < boardSize; crow++)
        {
            String		cellstates[];

            cellstates = strarr[crow].split(", ");
            if (cellstates.length != boardSize) {
                throw new IllegalArgumentException("wrong number of tokens while parsing state string, expected " + boardSize + " but got " + cellstates.length);
            }
            for (ccol = 0; ccol < boardSize; ccol++)
            {
                switch (CellState.valueOf(cellstates[ccol]))
                {
                    case EMPTY:
                        emptyCell(new BVCell(crow, ccol), false);
                        break;

                    case FILLED:
                        fillCell(new BVCell(crow, ccol), false);
                        break;

                    case UNUSED:
                        markCellUnused(new BVCell(crow, ccol));
                        break;
                }
            }
        }
    }

    // Empty constructor, used to define an invalid board state.
    private BVBoardState() {
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
     * Return an invalid board state, used to signal end of processing
     * @return an invalid state
     */
    public BoardState getInvalidState() {
        return new BVBoardState();
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
        for (row = 0; row < boardSize; row++)
        {
            for (col = 0; col < boardSize; col++)
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
     * @return id of the state
     */
    @Override
    public long getId() {
        return id;
    }

    /**
     * @param id, set id of the current state to the given id.
     */
    @Override
    public void setId(long id) {
        this.id = id;
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
        for (row = 0; row < boardSize; row++)
        {
            sep = sep + "[";
            for (col = 0; col < boardSize; col++)
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
        return new BVBoardState(id, stateDesc);
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
    }

    /**
     * Mark the given cell as unused. This is called only when the board is being initialized, so the state of cell is
     * not expected to be initialized.
     * @param cell
     */
    private void markCellUnused(BVCell cell) {
        if (getCellState(cell) != null)
            throw new IllegalStateException("Cell " + cell.toString() + " is " + getCellState(cell) + " and is in use.");
        boardState[cell.getRow()][cell.getCol()] = CellState.UNUSED;
    }

    // The board can be considered to have unused cells beyond the board size.
    // This allows us to avoid another method isValidCell() and call it
    // everywhere
    private CellState getCellState(BVCell BVCell)
    {
        int			row = BVCell.getRow();
        int			col = BVCell.getCol();
        if (row >= 0 && row < boardSize && col >= 0 && col < boardSize)
            return boardState[row][col];
        return CellState.UNUSED;
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
        result.id = 0;

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
        if (boardSize != other.boardSize) {
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
