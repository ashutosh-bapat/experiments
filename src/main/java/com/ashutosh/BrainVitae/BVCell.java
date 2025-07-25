package com.ashutosh.BrainVitae;

public class BVCell {
    private int		row;
    private int		col;

    public BVCell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public BVCell(BVCell BVCell, BVOffset off) {
        row = BVCell.getRow() + off.getRowOff();
        col = BVCell.getCol() + off.getColOff();
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String toString() {
        return "(" + row + ", " + col + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BVCell) {
            BVCell other = (BVCell) obj;
            return row == other.row && col == other.col;
        } else {
            return false;
        }
    }
}
