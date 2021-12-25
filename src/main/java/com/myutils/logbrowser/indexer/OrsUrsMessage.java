/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static Utils.Util.intOrDef;

/**
 * @author ssydoruk
 */
public class OrsUrsMessage extends Message {

    String _SID = null;
    String _Call = null;
    //    Properties m_fields;

    public OrsUrsMessage(String event, ArrayList messageLines, int fileID) {
        super(TableType.ORSUrs, fileID);
        m_MessageLines = messageLines;
        if (m_MessageLines != null && m_MessageLines.size() > 0) {
            String get = m_MessageLines.get(0);
            if (get != null && get.contains("HandleREvent")) {
                SetInbound(true);
            }
        }
//        m_isInbound = event.equals("HandleREvent");
//        m_fields = new Properties();
//        for (Object o: messageLines) {
//            String line = (String)o;
//            line = line.trim();
//            String[] words = line.split("\\t");
//            if (words.length == 2) {
//                if (words[1].charAt(0) == '\'')
//                    words[1] = words[1].substring(1, words[1].length() - 1);
//                m_fields.put(words[0], words[1]);
//            }
//        }
    }

    public String GetSid() {
        if (_SID == null) {
            _SID = GetHeaderValueNoQotes("session");
        }
        return _SID;
    }

    public String GetCall() {
//        if( _Call==null)
//            _Call=GetHeaderValueNoQotes("call");
//        if(_Call!=null && !_Call.equals(GetSid()))
//            return _Call;
        return "";
    }

    public String GetMessage() {
        String res = GetHeaderValue("message");
        if ((res == null || res.length() == 0) && !isInbound() && GetSid() == null) {
            res = GetHeaderValue("statistic");
        }
        return res;
    }

    public int GetRefId() {
        String refId = GetHeaderValue("refID");
        if (refId == null || refId.length() == 0) {
            return -1;
        }
        return Integer.parseInt(refId);
    }

    public int GetReqId() {
        String evt = GetHeaderValue("event");
        if (evt == null || evt.length() == 0) {
            return -1;
        }
        long event = Integer.parseInt(evt);
        if (event == 1000000101) {
            String msg = GetMessage();
            if (msg == null) {
                return -1;
            }
            try {
                int req = Integer.parseInt(msg);
                return req;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        if (event == 1000000102) {
            try {
                String msg = GetMessage();
                if (msg == null) {
                    return -1;
                }
                JSONObject obj = new JSONObject(msg);
                JSONObject data = obj.getJSONObject("data");
                return data.getInt("requestid");
            } catch (JSONException e) {
                return -1;
            }
        }
        return -1;
    }

    public long GetEvent() {
        String evt = GetHeaderValue("event");
        if (evt == null || evt.length() == 0) {
            return -1;
        }
        return intOrDef(evt, 0);
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        stmt.setBoolean(6, isInbound());
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSSID, GetSid()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.UUID, GetCall()));
        stmt.setLong(9, GetEvent());
        stmt.setInt(10, GetRefId());
        stmt.setInt(11, GetReqId());

        setFieldInt(stmt, 12, Main.getRef(ReferenceType.ORSMODULE, GetHeaderValue("module")));
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.ORSMETHOD, GetHeaderValue("method")));
        return true;
    }
}
