/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Pattern;

public final class URSStrategyInit extends Message {

    private String FileLine;
    private String ConnID;
    private String strategyMsg;
    private final String strategyName;

    URSStrategyInit(String line, String ConnID, String strategyName, String strategyMsg) {
        super(TableType.URSStrategyInit, line);
        this.FileLine = FileLine;
        this.ConnID = ConnID;
        this.strategyName = strategyName;
        this.strategyMsg = strategyMsg;
        Main.logger.trace(this.toString());
    }

    @Override
    public String toString() {
        return "URSStrategy{" + "FileLine=" + FileLine + ", ConnID=" + ConnID + ", rest=" + strategyMsg + '}';
    }

    String GetConnID() {
        return ConnID;
    }

    String getFileFun() {
        return FileLine;
    }

    String getStrategyMsg() {
        return strategyMsg;
    }

    private static final Pattern uuidPattern = Pattern.compile("^calluuid ([\\w~]+) is bound");

    String getUUID() {
        return FindByRx(uuidPattern, strategyMsg, 1, "");
    }

    String getStrategyName() {
        return strategyName;
    }

}
