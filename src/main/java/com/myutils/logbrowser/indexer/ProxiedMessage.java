/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.intOrDef;
import java.util.regex.Pattern;

/**
 *
 * @author ssydoruk
 */
public class ProxiedMessage extends Message {

    private static final Pattern regAttributeReferenceID = Pattern.compile("AttributeReferenceID[\\s\\[]+(\\w+)");
    private static final Pattern regAttributeThisDN = Pattern.compile("AttributeThisDN[\\s\\[]+(\\w+)");
    private static final Pattern regFrom = Pattern.compile("from[\\s\\(]+\\([\\w]+\\s([^\\s]+)");
    private static final Pattern regTo = Pattern.compile("to[\\s\\(]+\\([\\w]+\\s([^\\s]+)");
    private final String event;
    private final String contents;

    public ProxiedMessage(String evName, String contents) {
        super(TableType.TLibProxied, contents);
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
