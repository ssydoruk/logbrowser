/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.json.JSONException;
import org.json.JSONObject;

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
