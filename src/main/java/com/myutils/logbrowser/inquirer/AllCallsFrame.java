/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.ScreenInfo;

import javax.swing.*;
import java.awt.*;

/**
 * @author ssydoruk
 */
final class AllCallsFrame extends JFrame {

    public AllCallsFrame(QueryAllJTable tab) throws HeadlessException {

    }

    AllCallsFrame(JScrollPane jScrollPane) {
        super();
        JPanel listPane = new JPanel();

        listPane.setLayout(
                new BoxLayout(listPane, BoxLayout.Y_AXIS));
        add(listPane);
        Dimension d = jScrollPane.getPreferredSize();
        listPane.add(jScrollPane);
        setMinimumSize(d);
        ScreenInfo.CenterWindow(this);

        pack();
        setVisible(
                true);

    }

}
