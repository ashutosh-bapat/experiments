package com.ashutosh.Sudoku;

public enum Symbol {
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9");

    String str;

    Symbol(String str) {
       this.str = str;
    }

    public String toString() {
        return str;
    }

    static Symbol findSymbolFor(String str) {
        for (Symbol val: values()) {
            if (val.str.equals(str))
                return val;
        }

        return null;
    }
}
