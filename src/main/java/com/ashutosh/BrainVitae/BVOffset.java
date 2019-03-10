package com.ashutosh.BrainVitae;

class BVOffset {
    private int		rowoff;
    private int		coloff;

    BVOffset(int inrowoff, int incoloff) {
        rowoff = inrowoff;
        coloff = incoloff;
    }

    int getRowOff() {
        return rowoff;
    }

    int getColOff() {
        return coloff;
    }

    // TODO: It is recommended that when equals is overridden, hashCode also needs to be overridden. See various
    // references about this in Java articles.
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BVOffset)) {
            return false;
        }
        return equals((BVOffset) o);
    }

    private boolean equals(BVOffset other) {
       if (this.rowoff != other.rowoff) {
           return false;
       }

        return this.coloff == other.coloff;
    }
}
