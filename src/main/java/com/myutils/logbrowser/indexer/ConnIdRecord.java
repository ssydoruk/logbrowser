/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 */
public class ConnIdRecord extends SIPServerBaseMessage {

    private final String m_connId;

    private final boolean m_created;
    private boolean isTemp;
    private String newID;

    ConnIdRecord(String oldId, String newId, boolean b, boolean m_handlerInProgress, int m_handlerId, int fileID) {
        this(oldId, b, m_handlerInProgress, m_handlerId, fileID);
        this.newID = newId;
//        Main.logger.info("ConnIdRecord: ["+oldId+"] new["+newId+"]");
    }

    public ConnIdRecord(String connId, boolean created, boolean m_handlerInProgress, int m_handlerId, int fileID) {
        super(TableType.ConnID, m_handlerInProgress, m_handlerId, fileID);
        this.isTemp = false;
        m_connId = connId;
        m_created = created;
    }

    public String getNewID() {
        return newID;
    }

    public boolean isIsTemp() {
        return isTemp;
    }

    @Override
    public String toString() {
        return "ConnIdRecord{" + "m_connId=" + m_connId + ", m_created=" + m_created + '}' + super.toString();
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setInt(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, getM_connId()));
        stmt.setInt(7, (isM_handlerInProgress() ? getM_handlerId() : 0));
        Main.logger.trace("m_handlerId :" + getM_handlerId() + " m_handlerInProgress" + isM_handlerInProgress());

        stmt.setBoolean(8, isM_created());
        stmt.setBoolean(9, isIsTemp());
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.ConnID, getNewID()));
        return true;

    }

    //    public void AddToDB(DBAccessor accessor) {
//        PreparedStatement stmt = accessor.GetStatement(m_batchId);
//        try {
//            setFieldString(stmt,1, m_connId);
//            stmt.setBoolean(2, m_created);
//            stmt.setInt(3, m_fileId);
//            accessor.SubmitStatement(m_batchId);
//        } catch (Exception e) {
//            System.out.println("Could not add ConnId manipulations record: "+e);
//        }
//    }
    public String getM_connId() {
        return m_connId;
    }

    public boolean isM_created() {
        return m_created;
    }

    public void setTempConnid(boolean b) {
        isTemp = b;
    }

}
