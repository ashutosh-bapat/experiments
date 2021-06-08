package com.ashutosh.Sudoku;

import java.util.*;
import java.util.stream.Collectors;

public class Cell {
    final private String id;
    final private EnumSet<Symbol> possibleValues ;
    private Symbol finalValue = null;
    final private List<Club> clubs;
    final private Queue<Cell> candidates;

    Cell(Symbol finalValue, List<Club> clubs, Queue<Cell> candidates, String id) {
        this(EnumSet.of(finalValue), clubs, candidates, id);
    }

    Cell(List<Club> clubs, Queue<Cell> candidates, String id) {
        this(EnumSet.allOf(Symbol.class), clubs, candidates, id);
    }

    Cell(EnumSet<Symbol> possibleValues, List<Club> clubs, Queue<Cell> candidates, String id) {
        this.possibleValues = possibleValues;
        this.clubs = clubs;
        this.finalValue = null;
        this.candidates = candidates;
        this.id = id;
    }

    // Returns true if the cell had a valid candidate and resolves it. If the cell was already resolved, it will return
    // false.
    boolean resolve() {
        // Already resolved, ignore
        if (finalValue != null) {
            return false;
        }
        if (possibleValues.size() != 1) {
            throw new IllegalStateException("cell (" + id + ") has more than one possible values: " + possibleValuesStr());
        }

        // There should be only one element
        finalValue = possibleValues.iterator().next();
        clubs.forEach(club -> club.resolved(this));
        return true;
    }

    String possibleValuesStr() {
        return possibleValues.stream().map(value -> value.str).collect(Collectors.joining(","));
    }

    EnumSet<Symbol> getPossibleValues() {
        return possibleValues.clone();
    }

    Symbol getFinalValue() {
        if (finalValue == null) {
           throw new IllegalStateException("cell (" + id + ") does not have a final value yet. Its possible values are: " + possibleValuesStr());
        }
        return finalValue;
    }

    void remove(Symbol symbol) {
        // If the symbol was not present or already removed, nothing to do.
        if (!possibleValues.remove(symbol))
            return;

        if (possibleValues.size() == 0) {
            if (finalValue == null) {
                throw new IllegalStateException("cell (" + id + ") does not have any possible value after removing: " + symbol);
            }
        }

        if (possibleValues.size() == 1) {
            candidates.add(this);
        }

        clubs.forEach(club -> club.removeCellForSymbol(this, symbol));
    }

    String getId() {
        return id;
    }

    boolean isResolved() {
        return finalValue != null;
    }

    // Function used by other classes to set a symbol that is deduced to occupy this cell. For example, if a row sees
    // that a particular cell is the only one to contain a given symbol, it would invoke this method.
    void setSymbol(Symbol symbol) {
       if (!possibleValues.contains(symbol))  {
           throw new IllegalArgumentException("cell (" + id + ") does not have " + symbol + " in the possible values.");
       }

       // Remove all the other symbols from possible values
       for (Symbol otherSymbol : possibleValues) {
           if (otherSymbol != symbol) {
               remove(otherSymbol);
           }
       }
    }

    public List<Club> getClubs() {
        return clubs;
    }
}
