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
class AllMedia_CallFlowSettings extends AllInteractionsSettings {

    private final MediaServerResults callFlowResults;
    private static AllMedia_CallFlowSettings INSTANCE = null;

    public static AllMedia_CallFlowSettings getInstance(MediaServerResults routingResults, Window c) {
        if (INSTANCE == null) {
            INSTANCE = new AllMedia_CallFlowSettings(routingResults, c);
        }
        return INSTANCE;
    }

    public AllMedia_CallFlowSettings(MediaServerResults res, Window parent) {
        super(parent);
        this.callFlowResults = res;
        setContent(null);
    }

    @Override
    ArrayList<Pair<String, String>> sortFieldsModel() {
        return MediaServerResults.getAllSortFields();
    }

}
