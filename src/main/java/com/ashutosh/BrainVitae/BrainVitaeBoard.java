package com.ashutosh.BrainVitae;

public class BrainVitaeBoard {
    private final int cellToBitIndexMap[][];
    static final int FILLED_STRIP_WIDTH=3;
    public final BVCell midcell;

    final int numUsedCells;

    static final int MAX_CELLS = 64;
    static final int UNUSED_CELL_INDEX = -1;

    public int getBoardSize() {
        return boardSize;
    }

    private final int boardSize;

    public boolean withinFilledStrip(BVCell cell) {
        int filledWidthAroundMidpoint = FILLED_STRIP_WIDTH / 2;
        if (cell.getRow() <= midcell.getRow() + filledWidthAroundMidpoint && cell.getRow() >= midcell.getRow() - filledWidthAroundMidpoint) {
            return true;
        } else if (cell.getCol() <= midcell.getCol() + filledWidthAroundMidpoint && cell.getCol() >= midcell.getCol() - filledWidthAroundMidpoint) {
            return true;
        }
        return false;
    }

    BrainVitaeBoard(int size) throws IllegalArgumentException
    {
        if (size % 2 != 1) {
            throw new IllegalArgumentException("invalid board size " + size + ". Size should be an odd integer.");
        }

        if (size < FILLED_STRIP_WIDTH) {
            throw new IllegalArgumentException("invalid board size " + size + ". Size should be at least " + FILLED_STRIP_WIDTH + ".");
        }

        boardSize = size;
        cellToBitIndexMap = new int[size][size];

        final int midpoint = size / 2;
        midcell = new BVCell(midpoint, midpoint);
        int index = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BVCell cell = new BVCell(row, col);
                if (withinFilledStrip(cell)) {
                    setIndex(cell, index++);
                } else {
                    setIndex(cell, UNUSED_CELL_INDEX);
                }
            }
        }
        numUsedCells = index;
    }

    private boolean isValidCell(BVCell cell) {
        return (cell.getRow() >= 0 && cell.getRow() < boardSize && cell.getCol() >= 0 && cell.getCol() < boardSize);
    }

    private void setIndex(BVCell cell, int index) throws IllegalStateException {
        if (!isValidCell(cell))
            throw new IllegalArgumentException("invalid cell " + cell + " for board size " + boardSize);

        if (index >= MAX_CELLS)
            throw new IllegalStateException("can not handle boards with more than " + MAX_CELLS + " cells");

        cellToBitIndexMap[cell.getRow()][cell.getCol()] = index;
    }

    public int getIndex(BVCell cell) {
        if (!isUsed(cell))
            throw new IllegalArgumentException("Cell " + cell + " does not have an index");
        return cellToBitIndexMap[cell.getRow()][cell.getCol()];
    }

    public boolean isUsed(BVCell cell) {
        return (cellToBitIndexMap[cell.getRow()][cell.getCol()] != UNUSED_CELL_INDEX);
    }
}
