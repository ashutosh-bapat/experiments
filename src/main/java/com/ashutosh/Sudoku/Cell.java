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

    void resolve() {
        // Already resolved, ignore
        if (finalValue != null) {
            return;
        }
        if (possibleValues.size() != 1) {
            throw new IllegalStateException("cell (" + id + ") has more than one possible values: " + possibleValues());
        }

        // There should be only one element
        finalValue = possibleValues.iterator().next();
        clubs.forEach(club -> club.resolved(this));
    }

    String possibleValues() {
        return possibleValues.stream().map(value -> value.str).collect(Collectors.joining(","));
    }

    Symbol getFinalValue() {
        if (finalValue == null) {
           throw new IllegalStateException("cell (" + id + ") does not have a final value yet. Its possible values are: " + possibleValues());
        }
        return finalValue;
    }

    void remove(Symbol symbol) {
        possibleValues.remove(symbol);
        if (possibleValues.size() == 0) {
            if (finalValue == null) {
                throw new IllegalStateException("cell (" + id + ") does not have any possible value after removing: " + symbol);
            }
        }

        if (possibleValues.size() == 1) {
            candidates.add(this);
        }
    }
}
