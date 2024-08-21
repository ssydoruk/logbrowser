/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.jidesoft.swing.CheckBoxListSelectionModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author ssydoruk
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation"})
public class CheckBoxListSettings extends MyCheckBoxList {

    public CheckBoxListSettings(DefaultListModel lmRefValues, JPanel jpo) {
        super(lmRefValues, jpo);
        menu.add(new javax.swing.JPopupMenu.Separator());
        menu.add(new JMenuItem(new AbstractAction("Delete selected") {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected(e);
            }
        }));

    }

    private HashSet<Integer> hsFromArray(int[] arr) {
        HashSet<Integer> idx = new HashSet<>(arr.length);
        for (int i = 0; i < arr.length; i++) {
            idx.add(arr[i]);

        }
        return idx;
    }

    private void deleteSelected(ActionEvent e) {
        int[] selectedIndices = getSelectedIndices();
        int[] checkBoxListSelectedIndices = getCheckBoxListSelectedIndices();

        HashSet<Integer> idx = hsFromArray(selectedIndices);
        HashSet<Integer> checked = hsFromArray(checkBoxListSelectedIndices);

        DefaultListModel model = (DefaultListModel) getModel();
        DefaultListModel newModel = new DefaultListModel();
        CheckBoxListSelectionModel newSelectionModel;
        ArrayList<Integer> checkedArr = new ArrayList<>(checkBoxListSelectedIndices.length);

        for (int i = 0; i < model.size(); i++) {
            if (!idx.contains(i)) {
                newModel.addElement(model.get(i));
                if (checked.contains(i)) {
                    checkedArr.add(newModel.size() - 1);
                }

            }
        }
        setModel(newModel);

        newSelectionModel = new CheckBoxListSelectionModel();
//        newSelectionModel.setSelectionMode(checkBoxListSelectionModel.getSelectionMode());
        newSelectionModel.addSelectionInterval(1, 2);
        int[] ch = new int[checkedArr.size()];
        for (int i = 0; i < checkedArr.size(); i++) {
            ch[i] = checkedArr.get(i);

        }
        setCheckBoxListSelectedIndices(ch);

//        setSelectionModel(newSelectionModel);
        Object source = e.getSource();
        inquirer.logger.debug(source);
    }

}
