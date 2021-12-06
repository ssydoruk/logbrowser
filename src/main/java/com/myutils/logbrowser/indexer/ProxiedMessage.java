/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

/**
 * @author ssydoruk
 */
public class ProxiedMessage extends Message {

    private static final Pattern regAttributeReferenceID = Pattern.compile("AttributeReferenceID[\\s\\[]+(\\w+)");
    private static final Pattern regAttributeThisDN = Pattern.compile("AttributeThisDN[\\s\\[]+(\\w+)");
    private static final Pattern regFrom = Pattern.compile("from[\\s\\(]+\\([\\w]+\\s([^\\s]+)");
    private static final Pattern regTo = Pattern.compile("to[\\s\\(]+\\([\\w]+\\s([^\\s]+)");
    private final String event;
    private final String contents;

 private int m_tlibId;

    public int getM_tlibId() {
        return m_tlibId;
    }

    public void setM_tlibId(int m_tlibId) {
        this.m_tlibId = m_tlibId;
    }

    public int getM_handlerId() {
        return m_handlerId;
    }

    public void setM_handlerId(int m_handlerId) {
        this.m_handlerId = m_handlerId;
    }

    private int m_handlerId;

    public ProxiedMessage(String evName, String contents, int fileID) {
        super(TableType.TLibProxied, contents, fileID);
        this.event = evName;
        this.contents = contents;

    }

    public String getEvent() {
        return event;
    }

    public String getRefid() {
        return FindByRx(regAttributeReferenceID, contents, 1, "");
    }

    public String getTo() {
        return FindByRx(regTo, contents, 1, "");
    }

    public String getDn() {
        return FindByRx(regAttributeThisDN, contents, 1, "");
    }

    public String getFrom() {
        return FindByRx(regFrom, contents, 1, "");
    }

    long getRefID() {
        return intOrDef(getRefid(), 0);
    }

}
