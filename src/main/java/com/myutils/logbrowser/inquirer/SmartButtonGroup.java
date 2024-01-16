package com.myutils.logbrowser.inquirer;

import javax.swing.*;
import java.awt.*;

public class SmartButtonGroup<T> extends ButtonGroup {
    @Override
    public void add(AbstractButton b) {
        throw new UnsupportedOperationException("No object supplied");
    }

    JPanel pan;

    public SmartButtonGroup() {
        this.pan = new JPanel();
        pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
    }

    public JPanel getPan() {
        return pan;
    }

    public void add(JRadioButton button, T attachedObject) {
        ExtendedModel<T> model = new ExtendedModel<>(attachedObject);
        model.setSelected(button.isSelected());
        button.setModel(model);
        pan.add(button);
        super.add(button);
    }

    @SuppressWarnings("unchecked")
    public T getSelectedObject() {
        ButtonModel selModel = getSelection();
        return selModel != null ? ((ExtendedModel<T>)selModel).obj : null;
    }

    public static class ExtendedModel<T> extends javax.swing.JToggleButton.ToggleButtonModel {
        public T obj;

        private ExtendedModel(T object) {
            obj = object;
        }
    }
}