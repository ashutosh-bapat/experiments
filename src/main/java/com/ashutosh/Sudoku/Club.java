package com.ashutosh.Sudoku;

import java.util.*;

public class Club {
    final private List<Cell> members;
    final private EnumMap<Symbol, Cell> symbolMap = new EnumMap(Symbol.class);
    final private EnumMap<Symbol, HashSet<Cell>> symbolPossibleCellMap = new EnumMap(Symbol.class);
    final private String id;
    final private Queue<Cell> candidate;

    Club(String id, Queue<Cell> candidate) {
        members = new LinkedList<>();
        this.id = id;
        this.candidate = candidate;
    }

    boolean add(Cell cell) {
        for (Symbol symbol: cell.getPossibleValues()) {
            HashSet<Cell> possibleCells = symbolPossibleCellMap.get(symbol);
            if (possibleCells == null) {
               possibleCells = new HashSet<>();
               symbolPossibleCellMap.put(symbol, possibleCells);
            }
            possibleCells.add(cell);
        }
        return members.add(cell);
    }

    void resolved(Cell cell) {
        Symbol cellSymbol = cell.getFinalValue();
        if (symbolMap.get(cellSymbol) != null) {
            throw new IllegalStateException("club (" + id + ") already has a cell mapped to symbol: " + cellSymbol);
        }
        symbolMap.put(cellSymbol, cell);
        HashSet<Cell> possibleCells = symbolPossibleCellMap.get(cellSymbol);
        if (!possibleCells.contains(cell)) {
           throw new IllegalStateException("cell (" + cell.getId() + ") is not in the list of club (" + id + ")'s possible cells for symbol " + cellSymbol);
        }
        // Remove the symbol assigned to the resolved cell from all other members and remove these members from the
        // possible cells for that symbol
        for (Cell member: members) {
            if (member == cell)
                continue;

            member.remove(cellSymbol);
            possibleCells.remove(member);
            if (possibleCells.size() <= 0) {
                throw new IllegalStateException("club (" + id + ") does not have any cells which can contain symbol: " + cellSymbol +
                                                ". Ideally it should have had cell (" + cell.getId() + ") in it.");
            }
        }

        // Remove the resolved cells from possible values of other symbols
        for (Map.Entry<Symbol, HashSet<Cell>> entry : symbolPossibleCellMap.entrySet()) {
            if (entry.getKey() != cellSymbol) {
                removeCellForSymbol(cell, entry.getKey());
            }
        }
    }

    // Inform the club of a given cell that it can not contain the given symbol
    void removeCellForSymbol(Cell cell, Symbol symbol) {
        HashSet<Cell> possibleCells = symbolPossibleCellMap.get(symbol);
        possibleCells.remove(cell);

        if (possibleCells.size() <= 0) {
            throw new IllegalStateException("club (" + id + ") does not have any cells which can contain symbol " + symbol);
        }

        if (possibleCells.size() == 1) {
            Cell possibleCandidate = possibleCells.iterator().next();
            if (!possibleCandidate.isResolved()) {
                possibleCandidate.setSymbol(symbol);
                candidate.add(possibleCandidate);
            }
        }
    }
}
