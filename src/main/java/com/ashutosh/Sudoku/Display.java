package com.ashutosh.Sudoku;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

public class Display {

    public static void main(String[] argv) {
        int size = 9;
        String[][] matrix = new String[size][size];
        JTextField[][] cellTexts = new JTextField[size][size];
       JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(size, size));

        for(int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                cellTexts[row][col] = new JTextField();
                panel.add(cellTexts[row][col]);
            }
        }

        if (JOptionPane.showConfirmDialog(null, panel, "Enter the matrix", JOptionPane.OK_CANCEL_OPTION)
                                        == JOptionPane.OK_OPTION)
        {
            for(int row = 0; row < size; row++){
                for(int col = 0; col < size; col++){
                    if (StringUtils.isEmpty(cellTexts[row][col].getText())) {
                        matrix[row][col] = null;
                    } else {
                        matrix[row][col] = cellTexts[row][col].getText();
                    }
                }
            }
        }

        Solver solver = new Solver(matrix);
        matrix = solver.solve();

        for(int row = 0; row < size; row++){
            for(int col = 0; col < size; col++){
                cellTexts[row][col].setText(matrix[row][col]);
            }
        }

        JOptionPane.showConfirmDialog(null, panel, "Solution matrix", JOptionPane.OK_CANCEL_OPTION);
    }
}
