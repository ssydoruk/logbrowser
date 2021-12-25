/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class URSCONNIDSID extends Message {

    private final String ConnID;
    private final String SIDID;

    URSCONNIDSID(String ConnID, String SIDID, int fileID) {
        super(TableType.URSCONNIDSID, fileID);
        this.ConnID = ConnID;
        this.SIDID = SIDID;
    }

    public String GetSid() {
        return SIDID;
    }

    String GetConnID() {
        return ConnID;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, GetConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSSID, GetSid()));
        return true;

    }
}
