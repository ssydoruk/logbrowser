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
class CURSReportType extends AllInteractionsSettings {
    
    private final RoutingResults routingResults;
    private final SmartButtonGroup<IGetAllProc> selectAllReportTypeGroup;
    private static CURSReportType INSTANCE = null;

    public static CURSReportType getInstance(RoutingResults routingResults, Window c) {
        if (INSTANCE == null) {
            INSTANCE = new CURSReportType(routingResults, c);
        }
        return INSTANCE;
    }

    public SmartButtonGroup<IGetAllProc> getSelectAllReportTypeGroup() {
        return selectAllReportTypeGroup;
    }

    public CURSReportType(RoutingResults routingResults, Window parent) {
        super(parent);
        this.routingResults = routingResults;
        JPanel pan = new JPanel();
        pan.setBorder(new TitledBorder("Request selection"));
        pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
        setResizable(false);
        setTitle("Request type");
        selectAllReportTypeGroup = new SmartButtonGroup<>();
        JRadioButton bt = new JRadioButton("ORS interactions");
        bt.setSelected(true);
        pan.add(bt);
        selectAllReportTypeGroup.add(bt, (qd, settings) -> routingResults.getAllORSCalls(qd, settings));
        bt = new JRadioButton("ORS strategies");
        pan.add(bt);
        selectAllReportTypeGroup.add(bt, (qd, settings) -> routingResults.getAllORSStrategies(qd, settings));
        bt = new JRadioButton("URS interactions");
        pan.add(bt);
        selectAllReportTypeGroup.add(bt, (qd, settings) -> routingResults.getAllURSCalls(qd, settings));
        bt = new JRadioButton("URS strategies");
        pan.add(bt);
        selectAllReportTypeGroup.add(bt, (qd, settings) -> routingResults.getAllURSStrategies(qd, settings));
        setContent(pan);
    }

    @Override
    ArrayList<Pair<String, String>> sortFieldsModel() {
        return routingResults.getAllSortFields();
    }
    
}
