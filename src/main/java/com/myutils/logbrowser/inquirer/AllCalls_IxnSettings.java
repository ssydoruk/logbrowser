/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import java.awt.Window;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

/**
 *
 * @author ssydo
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation", "this-escape"})
class AllCalls_IxnSettings extends AllInteractionsSettings {

    private final IxnServerResults ixnResults;
    private final SmartButtonGroup<IGetAllProc> selectAllReportTypeGroup;
    private static AllCalls_IxnSettings INSTANCE = null;

    public static AllCalls_IxnSettings getInstance(IxnServerResults _ixnResults, Window c) {
        if (INSTANCE == null) {
            INSTANCE = new AllCalls_IxnSettings(_ixnResults, c);
        }
        return INSTANCE;
    }

    public SmartButtonGroup<IGetAllProc> getSelectAllReportTypeGroup() {
        return selectAllReportTypeGroup;
    }

    public AllCalls_IxnSettings(IxnServerResults _ixnResults, Window parent) {
        super(parent);
        this.ixnResults = _ixnResults;
        JPanel pan = new JPanel();
        pan.setBorder(new TitledBorder("Request selection"));
        pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
        setResizable(false);
        setTitle("Request type");
        selectAllReportTypeGroup = new SmartButtonGroup<>();
        JRadioButton bt;
        bt = new JRadioButton("ORS interactions");
        bt.setSelected(true);
        pan.add(bt);
        selectAllReportTypeGroup.add(bt, (qd, settings) -> _ixnResults.getAllInteractions(qd, settings));

        bt = new JRadioButton("Agent logins");
        pan.add(bt);
        selectAllReportTypeGroup.add(bt, (qd, settings) -> _ixnResults.getAllAgentLogins(qd, settings));
        setContent(pan);
    }

    @Override
    ArrayList<Pair<String, String>> sortFieldsModel() {
        return ixnResults.getAllSortFields();
    }

}
