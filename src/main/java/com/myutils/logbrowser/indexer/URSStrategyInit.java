/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Pattern;

public final class URSStrategyInit extends Message {

    private static final Pattern uuidPattern = Pattern.compile("^calluuid ([\\w~]+) is bound");
    private final String ConnID;
    private final String strategyMsg;
    private final String strategyName;
    private String FileLine;

    URSStrategyInit(String line, String ConnID, String strategyName, String strategyMsg, int fileID) {
        super(TableType.URSStrategyInit, line, fileID);
        this.ConnID = ConnID;
        this.strategyName = strategyName;
        this.strategyMsg = strategyMsg;
        Main.logger.trace(this.toString());
    }

    @Override
    public String toString() {
        return "URSStrategy{" + "FileLine=" + FileLine + ", ConnID=" + ConnID + ", rest=" + strategyMsg + '}';
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, GetConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, getUUID()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.URSStrategyMsg, getStrategyMsg()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.URSStrategyName, getStrategyName()));
//                setFieldString(stmt,11, getCgMsg());

        return true;
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

    String getUUID() {
        return FindByRx(uuidPattern, strategyMsg, 1, "");
    }

    String getStrategyName() {
        return strategyName;
    }

}
