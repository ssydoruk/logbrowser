package com.myutils.logbrowser.inquirer;

import javax.swing.*;
import java.awt.*;

public class SmartButtonGroup<T> extends ButtonGroup {
    @Override
    public void add(AbstractButton b) {
        throw new UnsupportedOperationException("No object supplied");
    }


    public SmartButtonGroup() {
    }

    public void add(JRadioButton button, T attachedObject) {
        ExtendedModel<T> model = new ExtendedModel<>(attachedObject);
        model.setSelected(button.isSelected());
        button.setModel(model);
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