/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class URSRI extends URSRIBase {

    public URSRI(TableType t, ArrayList messageLines, int fileID) {
        super(t, messageLines, fileID);
    }

    URSRI( int fileID) {
        super(TableType.URSRI, fileID);
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, getConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.App, getClientApp()));
        stmt.setInt(8, getClientID());
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, getORSSID()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.URSMETHOD, getFunc()));
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.URSMETHOD, getSubFunc()));
        stmt.setInt(12, getRefid());
        stmt.setBoolean(13, isInbound());
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, getUUID()));
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.HTTPRequest, getUriParams()));

        setTableValues(16, stmt);
        return true;

    }
}
