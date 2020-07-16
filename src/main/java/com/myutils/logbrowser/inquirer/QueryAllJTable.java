/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.gui.QueryJTable;
import com.myutils.logbrowser.inquirer.gui.ReportFrameQuery;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author ssydoruk
 */
public class QueryAllJTable extends QueryJTable {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private final IQueryResults qry;
    private IQueryResults.SearchFields searchField;
    private final QueryDialog qd;
    private boolean followLog = false;

    ListSelectionChanged listSelectionChanged = null;
    ReportFrameQuery frm = null;

    public QueryAllJTable(IQueryResults qry, QueryDialog qd, FullTableColors all) throws Exception {
        super(all);
        this.searchField = qry.getSearchField();
        this.qry = qry;
        this.qd = qd;
        addMouseListener(new MyMouseAdapter() {
        });
        getPopupMenu().addSeparator();
        changeFollowSelection(followLog);

        listSelectionChanged = new ListSelectionChanged(this);

        JMenuItem menuItem = new JMenuItem("Open in new window");
        getPopupMenu().add(menuItem);

        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCall(popupRow, false, true);
            }
        });

        JCheckBoxMenuItem followSource = new JCheckBoxMenuItem("Follow change (doubleclick otherwise)");
        followSource.setSelected(followLog);
        followSource.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFollowSelection(((JCheckBoxMenuItem) e.getSource()).getModel().isSelected());

            }

        });
        getPopupMenu().add(followSource);

    }

    private void changeFollowSelection(boolean selected) {
        followLog = selected;
        if (followLog) {
            getSelectionModel().addListSelectionListener(listSelectionChanged);
        } else {
            getSelectionModel().removeListSelectionListener(listSelectionChanged);
        }
    }


    /**
     *
     * @param row - row id. Row is from table, so needs adjustment for usage in
     * model
     * @param isDblClick
     */
    public void showCall(int row, boolean isDblClick, boolean newForm) {
        QueryJTable.DataModel mod = (QueryJTable.DataModel) getModel();

        Pair<SelectionType, String> sc = null;
        row = convertRowIndexToModel(row);//Row is from table, so needs adjustment

        if (row >= 0) {
            sc = mod.getSearchColumn(row, searchField);
        }
        if (sc != null) {
//                    ReportFrameQuery frm = new ReportFrameQuery(qry, searchField, searchID);
            ReportFrameQuery theFrm = null;
            if (newForm) {
                theFrm = new ReportFrameQuery();

            } else {
                if (frm == null) {
                    frm = new ReportFrameQuery();
                }
                theFrm = frm;
            }
            try {
                qd.addSelection(sc.getValue());
                theFrm.showReport(qry, qd, sc.getKey(), sc.getValue());
                theFrm.doShow(isDblClick);
            } catch (Exception ex) {
                logger.log(org.apache.logging.log4j.Level.FATAL, ex);
            }
        }
    }
    class MyMouseAdapter extends MouseAdapter {
        
        @Override
        public void mousePressed(MouseEvent e) {
//            inquirer.logger.trace("pressed");
if (e.getClickCount() == 2) { // on doubleclick open in editor
    
    QueryAllJTable source = (QueryAllJTable) e.getSource();
    
//                source.showCall(source.convertRowIndexToModel(source.rowAtPoint(e.getPoint())), true);
int rowAtPoint = source.rowAtPoint(e.getPoint());
int convertRowIndexToModel = source.convertRowIndexToModel(source.rowAtPoint(e.getPoint()));
source.showCall(source.rowAtPoint(e.getPoint()), true, false);

} else {
}
        }
        
    }

    class ListSelectionChanged implements ListSelectionListener {

        private final QueryAllJTable theTable;

        private ListSelectionChanged(QueryAllJTable aThis) {
            this.theTable = aThis;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                DefaultListSelectionModel sm = (DefaultListSelectionModel) e.getSource();
                inquirer.logger.trace("Selection Changed" + sm.toString() + "");
                theTable.showCall(sm.getMinSelectionIndex(), false, false);
            }
        }
    }
}
