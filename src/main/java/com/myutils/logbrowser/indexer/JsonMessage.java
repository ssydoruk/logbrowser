/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 */
public class JsonMessage extends SIPServerBaseMessage {

    JSONObject m_contents;

    public JsonMessage(JSONObject contents, boolean m_handlerInProgress, int m_handlerId, int fileID) {
        super(TableType.JSon,m_handlerInProgress,m_handlerId, fileID);
        m_contents = contents;
    }

    @Override
    public String toString() {
        return "JsonMessage{" + "m_contents=" + m_contents + "m_jsonId=" + getRecordID() + '}' + super.toString();
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setInt(1, getRecordID());

        stmt.setTimestamp(2, new Timestamp(GetAdjustedUsecTime()));
        stmt.setBoolean(3, isInbound());
        setFieldString(stmt, 4, GetOrigUri());
        setFieldInt(stmt, 5, Main.getRef(ReferenceType.SIPURI, GetOrigUri()));
        setFieldInt(stmt, 6, Main.getRef(ReferenceType.SIPURI, GetDestUri()));
        stmt.setInt(7, GetSipsId());
        stmt.setInt(8, getFileID());
        stmt.setLong(9, getM_fileOffset());
        stmt.setLong(10, getFileBytes());
        stmt.setInt(11, getRecordID());
        stmt.setInt(12, getM_line());
        stmt.setInt(13, getHanglerID());
        return true;

    }

    public String GetOrigUri() {
        try {
            JSONObject orig = m_contents.getJSONObject("origin");
            String uri = orig.getString("uri");
            return uri;
        } catch (JSONException e) { // not found
            return "";
        }
    }

    public String GetDestUri() {
        try {
            JSONObject dest = m_contents.getJSONObject("destination");
            String uri = dest.getString("uri");
            return uri;
        } catch (JSONException e) { // not found
            return "";
        }
    }

    public boolean isRouting() {
        return m_contents.has("routing");
    }

    public int GetSipsId() {
        try {
            return m_contents.getInt("sips-request-id");
        } catch (JSONException e) { // not found
            return -1;
        }
    }

    int getHanglerID() {
        return (isM_handlerInProgress()  ? getHanglerID() : 0);
    }
}
