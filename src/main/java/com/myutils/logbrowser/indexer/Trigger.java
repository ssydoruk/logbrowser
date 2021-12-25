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
public class Trigger extends Record {

    private final SingleThreadParser parser;
    int m_msgId;
    private String m_text;

    public int getM_handlerId() {
        return parser.getM_handlerId();
    }


    public int getM_sipId() {
        return parser.getM_sipId();
    }


    public int getM_tlibId() {
        return parser.getM_tlibId();
    }

    public int getM_jsonId() {
        return parser.getM_jsonId();
    }


    Trigger(int fileID, SingleThreadParser singleThreadParser) {
        super(TableType.Trigger, fileID);
        parser = singleThreadParser;
    }

    @Override
    public String toString() {
        return "Trigger{" + "m_msgId=" + m_msgId + ", m_text=" + m_text + '}' + super.toString();
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setInt(1, Main.getRef(ReferenceType.HANDLER, getM_text()));
        Main.logger.trace("m_handlerId:"
                + getM_handlerId() + " m_msgId:"
                + m_msgId + " m_sipId:"
                + getM_sipId() + " m_tlibId:" + getM_tlibId() + " m_jsonId" + getM_jsonId());
        stmt.setInt(2, getM_handlerId());
        stmt.setInt(3, (m_msgId == 1 ? getM_sipId() : 0));
        stmt.setInt(4, (m_msgId == 0 ? getM_tlibId() : 0));
        stmt.setInt(5, (m_msgId == 2 ? getM_jsonId() : 0));
        stmt.setInt(6, getFileID());
        stmt.setLong(7, getM_fileOffset());
        stmt.setInt(8, getM_line());
        return true;
    }

    public int getM_msgId() {
        return m_msgId;
    }

    public String getM_text() {
        return m_text;
    }

    public void SetText(String text) {
        m_text = text;
    }

    public void SetMsgId(int id) {
        m_msgId = id;
    }

}
