/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class TemplateDBRecord extends Message {

    public TemplateDBRecord(ArrayList messageLines, int fileID) {
        super(TableType.ORSUrs, messageLines, fileID);
    }

    public String GetSid() {
        return GetHeaderValueNoQotes("session");
    }

    public String GetCall() {
        return GetHeaderValueNoQotes("call");
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        return false;
    }
}
