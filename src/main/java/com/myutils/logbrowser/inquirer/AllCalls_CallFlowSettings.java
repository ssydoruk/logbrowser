/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import java.awt.Window;
import java.util.ArrayList;

/**
 *
 * @author ssydo
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation", "this-escape"})
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
