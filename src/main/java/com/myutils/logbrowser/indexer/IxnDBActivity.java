/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class IxnDBActivity extends Message {

    private final String activity;
    private final String ixnid;
    private String reqid = null;

    IxnDBActivity(String activity, String reqid, String ixnid, int fileID) {
        super(TableType.IxnDB, fileID);
        this.activity = activity;
        this.reqid = reqid;
        this.ixnid = ixnid;
    }

    public String getActivity() {
        return activity;
    }

    public int getReqid() {
        try {
            return Integer.parseInt(reqid);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getIxnid() {
        return ixnid;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.DBActivity, getActivity()));
        stmt.setInt(7, getReqid());
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.IxnID, getIxnid()));
        return true;
    }
}
