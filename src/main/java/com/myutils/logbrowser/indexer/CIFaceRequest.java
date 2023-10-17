/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class CIFaceRequest extends SIPServerBaseMessage {

    private String m_Name;
    private Long m_refId;
    private String m_thisDN;
    private String m_otherDN;

    public CIFaceRequest(boolean m_handlerInProgress, int m_handlerId, int fileID) {
        super(TableType.CIFaceRequest, m_handlerInProgress, m_handlerId, fileID);
    }

    @Override
    public String toString() {
        return "CIFaceRequest{" + "m_Name=" + m_Name + ", m_refId=" + m_refId + ", m_thisDN=" + m_thisDN + ", m_otherDN=" + m_otherDN + '}' + super.toString();
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        setFieldInt(stmt, 1, Main.getRef(ReferenceType.TEvent, getM_Name()));
        stmt.setLong(2, getM_refId());
        setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, SingleQuotes(getM_thisDN())));
        setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, SingleQuotes(getM_otherDN())));
        stmt.setInt(5, getM_handlerId());
        stmt.setInt(6, getFileID());
        stmt.setLong(7, getM_fileOffset());
        stmt.setInt(8, getM_line());
        Main.logger.trace("m_handlerId:" + getM_handlerId());
        return true;
    }

    public String getM_Name() {
        return m_Name;
    }

    public Long getM_refId() {
        return m_refId;
    }

    public String getM_thisDN() {
        return m_thisDN;
    }

    public String getM_otherDN() {
        return m_otherDN;
    }

    public void SetName(String name) {
        m_Name = name;
    }

    public void SetRefId(Long refId) {
        m_refId = refId;
    }

    public void SetRefId(String refId) {
        if (refId != null && !"".equals(refId)) {
            m_refId = Long.parseLong(refId);
        }
    }

    public void SetThisDn(String thisDn) {
        if (thisDn != null && thisDn.length() > 0) {
            m_thisDN = thisDn;
        }
    }

    public void SetOtherDn(String otherDn) {
        if (otherDn != null && otherDn.length() > 0) {
            m_otherDN = otherDn;
        }
    }
    /*
     public static String InitDB(DBAccessor accessor, int batchId) {
     m_batchId = batchId;
     String query = "CREATE TABLE IF NOT EXISTS cireq_"+m_alias+" ("+
     "id INTEGER PRIMARY KEY ASC,"+
     "name CHAR(64),"+
     "refId bigint,"+
     "thisDN char(32),"+
     "otherDN char(32),"+
     "HandlerId INTEGER,"+
     "FileId INTEGER,"+
     "FileOffset bigint,"+
     "Line INTEGER);";
     accessor.ExecuteQuery(query);
     return "INSERT INTO cireq_"+m_alias+" VALUES(NULL,?,?,?,?,?,?,?,?);";
     }

     public void AddToDB(DBAccessor accessor) {
     if (m_refId == null) 
     return;
     PreparedStatement stmt = accessor.GetStatement(m_batchId);
     try {
     setFieldString(stmt,1, m_Name);
     stmt.setLong(2, m_refId);
     setFieldString(stmt,3, "'"+m_thisDN+"'");
     setFieldString(stmt,4, "'"+m_otherDN+"'");
     stmt.setInt(5, m_handlerId);
     stmt.setInt(6, m_fileId);
     stmt.setLong(7,getM_fileOffset());
     stmt.setInt(8, getM_line());
            
     accessor.SubmitStatement(m_batchId);
     } catch (Exception e) {
     Main.logger.error("Could not add CIFace:Request record: "+e);
     }
     }
     **/

}
