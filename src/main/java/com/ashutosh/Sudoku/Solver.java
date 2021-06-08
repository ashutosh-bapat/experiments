package com.ashutosh.Sudoku;

import org.apache.commons.lang3.time.StopWatch;

import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Solver {
    private final int size;
    private final Cell[][] cells;
    private final Club[] rows;
    private final Club[] cols;
    private final Club[] squares;
    private final Queue<Cell> candidates;
    StopWatch stopWatch = new StopWatch();

    Solver(String[][] matrix) {
        // This should be a square matrix
        size = matrix.length;
        cells = new Cell[size][size];
        rows = new Club[size];
        cols = new Club[size];
        squares = new Club[size];
        candidates = new LinkedBlockingQueue<>();
        int size_sqrt = (int) Math.sqrt(size);

        for (int i = 0; i < size; i++) {
            rows[i] = new Club("row " + i, candidates);
            cols[i] = new Club("column " + i, candidates);
            squares[i] = new Club("square " + i, candidates);
        }

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Cell cell;
                int squareno = (row / size_sqrt) * size_sqrt  + (col / size_sqrt);
                String cellId = "(" + row + "," + col + ")";

                if (matrix[row][col] != null) {
                    Symbol cellSymbol = Symbol.findSymbolFor(matrix[row][col].trim());
                    if (cellSymbol == null) {
                        throw new IllegalArgumentException("can not recognize symbol " + matrix[row][col]);
                    }
                    cell = new Cell(cellSymbol, List.of(rows[row], cols[col], squares[squareno]), candidates,
                            cellId);
                    candidates.add(cell);
                } else {
                    cell = new Cell(List.of(rows[row], cols[col], squares[squareno]), candidates, cellId);
                }
                cells[row][col] = cell;
                rows[row].add(cell);
                cols[col].add(cell);
                squares[squareno].add(cell);
            }
        }
    }

    String[][] solve() {
        stopWatch.start();
        String[][] outputMatrix;
        int resolvedCells = 0;

        while(!candidates.isEmpty()) {
            Cell cell = candidates.remove();
            if (cell.resolve()) {
                resolvedCells = resolvedCells + 1;
            }
        }

        if (resolvedCells != size * size) {
            System.out.println("Resolved only " + resolvedCells + " and not all " + size * size);
        }

        outputMatrix = new String[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                try {
                    outputMatrix[row][col] = cells[row][col].getFinalValue().str;
                } catch (IllegalStateException ise) {
                    outputMatrix[row][col] = cells[row][col].possibleValuesStr();
                }
            }
        }
        stopWatch.stop();
        return outputMatrix;
    }

    long timeTakenToSolve() {
        return stopWatch.getTime(TimeUnit.MILLISECONDS);
    }
}
