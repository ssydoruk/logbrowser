package com.myutils.logbrowser.inquirer.gui;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

@SuppressWarnings("serial")
public class TableMap extends AbstractTableModel implements TableModelListener {

    protected TableModel model;

    public TableModel getModel() {
        return model;
    }

    public void setModel(TableModel model) {
        this.model = model;
        model.addTableModelListener(this);
    }

    // By default, Implement TableModel by forwarding all messages
    // to the model.
    public Object getValueAt(int aRow, int aColumn) {
        return model.getValueAt(aRow, aColumn);
    }

    @Override
    public void setValueAt(Object aValue, int aRow, int aColumn) {
        model.setValueAt(aValue, aRow, aColumn);
    }

    public int getRowCount() {
        return (model == null) ? 0 : model.getRowCount();
    }

    public int getColumnCount() {
        return (model == null) ? 0 : model.getColumnCount();
    }

    @Override
    public String getColumnName(int aColumn) {
        return model.getColumnName(aColumn);
    }

    @Override
    public Class getColumnClass(int aColumn) {
        return model.getColumnClass(aColumn);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return model.isCellEditable(row, column);
    }
//
// Implementation of the TableModelListener interface,
//

    // By default forward all events to all the listeners.
    public void tableChanged(TableModelEvent e) {
        fireTableChanged(e);
    }
}
