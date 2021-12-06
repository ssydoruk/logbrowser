/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

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
