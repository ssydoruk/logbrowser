/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.inquirer.IAggregateQuery;
import com.myutils.logbrowser.inquirer.PrintStreams;
import com.myutils.logbrowser.inquirer.inquirer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

/**
 * @author ssydoruk
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation", "this-escape"})
public class ReportFrameAggregate extends ReportFrame {

    private IAggregateQuery query;

    public ReportFrameAggregate(PrintStreams ps) {
        super(ps);
        JButton addToolbarButton = addToolbarButton("Close", "Close this report", (new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                windowClose();
            }

        }));

        addToolbarButton("Editor", "Open the report in editor", (new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoadEditor();
            }

        }));

        addToolbarButton("Excel", "Open the report in Excel", (new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    LoadExcel();
                } catch (IOException ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
            }

        }));

        addToolbarButton("Pivot", "Open the report in Excel and build pivot table", (new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoadExcelPivot();
            }

        }));

        addWindowListener(new WindowListener() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeExcel();
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }

        });
    }

    public ReportFrameAggregate(PrintStreams ps, IAggregateQuery iaq) {
        this(ps);
        this.query = iaq;
    }

    private void LoadEditor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void LoadExcel() throws IOException {
        TabResultDataModel mod = (TabResultDataModel) tableView.getModel();
        Excel.aggregateEdit(mod.exportToExcel(tableView, query.getExcelName()));
    }

    private void LoadExcelPivot() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void closeExcel() {
        Excel.close();
    }

}
