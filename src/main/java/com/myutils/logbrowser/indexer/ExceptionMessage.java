/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class ExceptionMessage extends Message {

    private final String exception;
    private final String msg;

    ExceptionMessage(ArrayList<String> m_MessageContents, String m_msgName, String m_msg1, int fileID) {
        super(TableType.WWEException, fileID);
        this.exception = m_msgName;
        this.msg = m_msg1;
    }

    String getException() {
        return exception;
    }

    String getExceptionMessage() {
        return msg;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.Exception, getException()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ExceptionMessage, getExceptionMessage()));
        return true;
    }
}
