/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSSCXMLScript extends Message {

    //-> SCXML : 0000017F-040206B5-0001  METRIC <extension name="set_flex_attr" namespace="http://genesyslabs.com/schemas/ocs/treatments" />
//
//-> SCXML : 0000017F-040206B5-0001  1 SetFlexAttrAction.Execute()
    private static final Pattern regMetric = Pattern.compile("^\\s*METRIC <(\\w+)");
    private final String sessID;
    private final String rest;

    OCSSCXMLScript(String sessID, String rest, int fileID) {
        super(TableType.OCSSCXMLScript, fileID);
        this.sessID = sessID;
        this.rest = rest;
    }

    String getSessID() {
        return sessID;
    }

    String getMetric() {
        return FindByRx(regMetric, rest, 1, "");
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSSCXMLSESSION, getSessID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.METRIC, getMetric()));
        return true;

    }
}
