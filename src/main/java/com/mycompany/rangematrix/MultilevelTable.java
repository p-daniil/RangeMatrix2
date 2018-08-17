/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.rangematrix;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 *
 * @author daniil_pozdeev
 */
public class MultilevelTable extends JComponent {

    private RangeMatrixModel model;
    private Graphics2D g2d;
    private FontMetrics fm;

    public MultilevelTable(RangeMatrixModel model) {
        init(model);
    }

    MultilevelTable() {

    }

    private void init(RangeMatrixModel model) {
        this.model = model;
        //updateUI();
    }

    @Override
    public void updateUI() {
        setUI(UIManager.getUI(this));
        invalidate();
    }

    public RangeMatrixModel getModel() {
        return model;
    }
    
    float lowestPointHeader = 0;
    int columnCounter = 0;
    float sidePanelWidth = 200;

    ArrayList<Float> cellXList = new ArrayList<>();
    ArrayList<Float> cellYList = new ArrayList<>();
    ArrayList<Float> cellWidthList = new ArrayList<>();
    ArrayList<Float> cellHeightList = new ArrayList<>();

    public float setLowestPoint(float y, float cellHeight) {
        if (y > lowestPointHeader) {
            lowestPointHeader = y + cellHeight;
            return y;
        } else {
            return lowestPointHeader + cellHeight;
        }
    }

    public ArrayList<Object> getLeafColumns(Object parentColumn, ArrayList<Object> leafColumnList) {
        int columnCount = model.getColumnGroupCount(parentColumn);

        for (int i = 0; i < columnCount; i++) {
            Object child = model.getColumnGroup(parentColumn, i);
            boolean isGroup = model.isColumnGroup(child);
            if (isGroup) {
                getLeafColumns(child, leafColumnList);
            } else {
                leafColumnList.add(child);
            }
        }
        return leafColumnList;
    }

    float spaceAroundName = 4;

    public void setSpaceAroundName(int newSpace) {
        this.spaceAroundName = newSpace;
    }

    public float getCellHeight(int heightMultiplier) {
        return (fm.getHeight() + 2 * spaceAroundName) * heightMultiplier;
    }

    public float getWidthOfColumnName(Object column, FontMetrics fm) {
        String columnName = model.getColumnGroupName(column);
        return fm.stringWidth(columnName) + 2 * spaceAroundName;
    }

    public float getWidthOfColumn(Object column, FontMetrics fm) {
        float columnWidth = 0;
        ArrayList<Object> leafColumnList = getLeafColumns(column, new ArrayList<Object>());
        if (leafColumnList.size() == 0) {
            return fm.stringWidth(model.getColumnGroupName(column)) + 2 * spaceAroundName;
        }
        for (Object leafColumn : leafColumnList) {
            float leafColumnWidth = getWidthOfColumnName(leafColumn, fm);
            columnWidth += leafColumnWidth;
        }
        return columnWidth;
    }

    public ArrayList<Object> getColumnBrothers(Object parentColumn, ArrayList<Object> columnBrothersList) {
        int columnCount = model.getColumnGroupCount(parentColumn);

        for (int i = 0; i < columnCount; i++) {
            Object child = model.getColumnGroup(parentColumn, i);
            columnBrothersList.add(child);
        }
        return columnBrothersList;
    }
    
    public int getMaxRowIndex(Object parentColumn, int maxRowIndex) {
        int columnCount = model.getColumnGroupCount(parentColumn);
        ArrayList<Integer> maxRowIndexList = new ArrayList<>();
        
        for (int i = 0; i < columnCount; i++) {            
            Object child = model.getColumnGroup(parentColumn, i);
            boolean isGroup = model.isColumnGroup(child);
            if (isGroup) {
                maxRowIndex++;
                getRowIndex(child, maxRowIndex);
                maxRowIndex++;
                maxRowIndexList.add(maxRowIndex);
            }
            maxRowIndex = 0;
        }
        return Collections.max(maxRowIndexList);
    }

    public int getRowIndex(Object columnBrother, int countOfGenerations) {
        int columnCount = model.getColumnGroupCount(columnBrother);

        for (int i = 0; i < columnCount; i++) {
            Object child = model.getColumnGroup(columnBrother, i);
            boolean isGroup = model.isColumnGroup(child);
            if (isGroup) {
                countOfGenerations++;
                getRowIndex(child, countOfGenerations);
                countOfGenerations++;
            }
        }
        return countOfGenerations;
    }

    public int getHeightMultiplier(Object parentColumn, boolean isGroup, int rowIndex, int maxRowIndex) {
        if (!isGroup) {
//            ArrayList<Object> columnBrothersList = getColumnBrothers(parentColumn, new ArrayList<Object>());
//            ArrayList<Integer> countOfGenerationsList = new ArrayList<>();
//
//            for (Object columnBrother : columnBrothersList) {
//                int countOfGenerations = getCountOfGenerations(columnBrother, 0);
//                countOfGenerationsList.add(countOfGenerations);
//            }
            
            //return 1 + Collections.max(countOfGenerationsList);
            return (4 - rowIndex) + 1;
        } else {
            return 1;
        }
    }

    public void drawColumns(Object parentColumn, float parentCellX, float parentCellY, int rowCounter, int maxRowIndex) {
        int columnCount = model.getColumnGroupCount(parentColumn);
        float cellX = parentCellX;
        float cellY = parentCellY;

        for (int i = 0; i < columnCount; i++) {
            Object child = model.getColumnGroup(parentColumn, i);
            String columnName = model.getColumnGroupName(child);

            float cellWidth = getWidthOfColumn(child, fm);

            boolean isGroup = model.isColumnGroup(child);

            int heightMultiplier = getHeightMultiplier(parentColumn, isGroup, rowCounter, maxRowIndex);

            float cellHeight = getCellHeight(heightMultiplier);
            
            setLowestPoint(cellY, cellHeight);

            Rectangle2D rect = new Rectangle2D.Float(cellX, cellY, cellWidth, cellHeight);
            g2d.draw(rect);
            g2d.drawString(columnName,
                    cellX + cellWidth / 2 - fm.stringWidth(columnName) / 2,
                    cellY + cellHeight / 2 - fm.getHeight() / 2 + 12);        //12 - высота верхней панели окна

            if (isGroup) {
                rowCounter++;
                cellY += cellHeight;
                drawColumns(child, cellX, cellY, rowCounter, maxRowIndex);
                rowCounter--;
                cellY -= cellHeight;
            }            
            cellX += cellWidth;            
        }
    }

    public void drawRows(Object parentRow, float parentCellY, float parentCellHeight, float cellWidth) {

        int rowCount = model.getRowGroupCount(parentRow);

        for (int i = 0; i < rowCount; i++) {
            Object child = model.getRowGroup(parentRow, i);
            String rowName = model.getRowGroupName(child);

            float cellHeight = parentCellHeight / (rowCount);
            float cellX = columnCounter * cellWidth;
            float cellY = parentCellY + i * cellHeight;

            Rectangle2D rect = new Rectangle2D.Float(cellX, cellY, cellWidth, cellHeight);
            g2d.draw(rect);
            g2d.drawString(rowName, cellX + cellWidth / 2 - fm.stringWidth(rowName) / 2, cellY + 15);

            boolean isGroup = model.isRowGroup(child);
            if (isGroup) {
                columnCounter++;
                drawRows(child, cellY, cellHeight, cellWidth);
                columnCounter--;
            } else {
                g2d.drawLine((int) sidePanelWidth, (int) cellY, getWidth(), (int) cellY);
                cellYList.add(cellY);
                cellHeightList.add(cellHeight);
            }
        }
    }

    public void drawValues(FontMetrics fm, Graphics2D g2d) {

        for (int i = 0; i < cellYList.size(); i++) {
            for (int j = 0; j < cellXList.size(); j++) {
                String value = (model.getValueAt(j, i)).toString();
                g2d.drawString(value, cellXList.get(j) + cellWidthList.get(j) / 2 - fm.stringWidth(value) / 2, cellYList.get(i) + 15);
            }
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {

        g2d = (Graphics2D) g;
        fm = g2d.getFontMetrics();

        Object parentColumn = ((Model) model).getColumnRoot();
        int maxGeneration = getMaxRowIndex(parentColumn, 0);

        drawColumns(parentColumn, 0, 0, 0, maxGeneration);
//        Object parentRow = ((Model)model).getRowRoot();
//        float parentCellHeight = getHeight() - lowestPointHeader;
//        float cellWidth = 100;
//        
//        
//        drawRows(parentRow, fm, g2d, lowestPointHeader, parentCellHeight, cellWidth);
//        
//        drawValues(fm, g2d);

    }
}
