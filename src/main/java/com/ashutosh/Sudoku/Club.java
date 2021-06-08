package com.ashutosh.Sudoku;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

public class Club {
    final private List<Cell> members;
    private EnumMap<Symbol, Cell> symbolMap = new EnumMap(Symbol.class);
    final private String id;

    Club(String id) {
        members = new LinkedList<>();
        this.id = id;
    }

    boolean add(Cell cell) {
        return members.add(cell);
    }

    void resolved(Cell cell) {
        Symbol cellSymbol = cell.getFinalValue();
        if (symbolMap.get(cellSymbol) != null) {
            throw new IllegalStateException("club (" + id + ") already has a cell mapped to symbol: " + cellSymbol);
        }
        symbolMap.put(cellSymbol, cell);
        members.forEach(member -> member.remove(cellSymbol));
    }
}
