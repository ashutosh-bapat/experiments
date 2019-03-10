package com.ashutosh.BrainVitae;

enum BVDirLabel {UP, DOWN, LEFT, RIGHT};

public class BVDirection {
    private BVDirLabel label;
    private BVOffset nextOff;
    private BVOffset jumpOff;

    BVDirection(BVDirLabel dirlabel)
    {
        label = dirlabel;
        switch (dirlabel)
        {
            case UP:
                nextOff = new BVOffset(-1, 0);
                jumpOff = new BVOffset(-2, 0);
                break;

            case DOWN:
                nextOff = new BVOffset(1, 0);
                jumpOff = new BVOffset(2, 0);
                break;

            case LEFT:
                nextOff = new BVOffset(0, -1);
                jumpOff = new BVOffset(0, -2);
                break;

            case RIGHT:
                nextOff = new BVOffset(0, 1);
                jumpOff = new BVOffset(0, 2);
                break;

            default:
                throw new IllegalArgumentException("Unknown direction " + dirlabel);
        }
    }

    public String toString() {
        return label.toString();
    }

    BVOffset getNextOffset() {
        return nextOff;
    }

    BVOffset getJumpOffset() {
        return jumpOff;
    }
}
