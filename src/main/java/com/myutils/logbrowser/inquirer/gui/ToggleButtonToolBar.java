/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.inquirer.gui;

import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/**
 *
 * @author ssydo
 */
class ToggleButtonToolBar extends JToolBar {
    
    public ToggleButtonToolBar() {
        super();
        //            JButton bt = addButton("Console", "Print this report on console for navigation in Notepad++");
        //            bt.addActionListener(new ActionListener() {
        //                @Override
        //                public void actionPerformed(ActionEvent e) {
        //                    printConsole();
        //                }
        //
        //            });
        //            bt = addButton("Search", "Go back to search dialog");
        //            bt.addActionListener(new ActionListener() {
        //                @Override
        //                public void actionPerformed(ActionEvent e) {
        //                    OpenSearch();
        //                }
        //
        //            });
        //            bt = addButton("Notepad++", "Go to notepad");
        //            bt.addActionListener(new ActionListener() {
        //                @Override
        //                public void actionPerformed(ActionEvent e) {
        //                    OpenNotepad();
        //                }
        //
        //            });
    } //            JButton bt = addButton("Console", "Print this report on console for navigation in Notepad++");
    //            bt.addActionListener(new ActionListener() {
    //                @Override
    //                public void actionPerformed(ActionEvent e) {
    //                    printConsole();
    //                }
    //
    //            });
    //            bt = addButton("Search", "Go back to search dialog");
    //            bt.addActionListener(new ActionListener() {
    //                @Override
    //                public void actionPerformed(ActionEvent e) {
    //                    OpenSearch();
    //                }
    //
    //            });
    //            bt = addButton("Notepad++", "Go to notepad");
    //            bt.addActionListener(new ActionListener() {
    //                @Override
    //                public void actionPerformed(ActionEvent e) {
    //                    OpenNotepad();
    //                }
    //
    //            });

    JButton addButton(String title, String hint0, ActionListener act) {
        JButton tb = new JButton(title);
        tb.setToolTipText(hint0);
        tb.addActionListener(act);
        add(tb);
        return tb;
    }

    JToggleButton addToggleButton(Action a) {
        JToggleButton tb = new JToggleButton((String) a.getValue(Action.NAME), (Icon) a.getValue(Action.SMALL_ICON));
        tb.setMargin(ReportFrame.zeroInsets);
        tb.setEnabled(a.isEnabled());
        tb.setToolTipText((String) a.getValue(Action.SHORT_DESCRIPTION));
        tb.setAction(a);
        return addButton(tb);
    }

    JButton addButton(String title, String tooltip) {
        //            tb.setMargin(zeroInsets);
        JButton tb = new JButton(title);
        tb.setToolTipText(tooltip);
        return addButton(tb);
    }
    
    public JButton addButton(JButton bt){
        add(bt);
        return bt;
    }
    
    public JToggleButton addButton(JToggleButton bt) {
        add(bt);
        return bt;
    }
}
