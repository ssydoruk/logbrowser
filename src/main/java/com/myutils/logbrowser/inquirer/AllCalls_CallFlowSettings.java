/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

/**
 *
 * @author ssydo
 */
class AllCalls_CallFlowSettings extends AllInteractionsSettings {

    private final CallFlowResults callFlowResults;
    private static AllCalls_CallFlowSettings INSTANCE = null;

    public static AllCalls_CallFlowSettings getInstance(CallFlowResults routingResults, Window c) {
        if (INSTANCE == null) {
            INSTANCE = new AllCalls_CallFlowSettings(routingResults, c);
        }
        return INSTANCE;
    }

    public AllCalls_CallFlowSettings(CallFlowResults res, Window parent) {
        super(parent);
        this.callFlowResults = res;
        setContent(null);
    }

    @Override
    ArrayList<Pair<String, String>> sortFieldsModel() {
        return callFlowResults.getAllSortFields();
    }

}
