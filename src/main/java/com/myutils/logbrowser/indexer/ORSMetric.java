/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ssydoruk
 */

/*It extends TServer message, however there is now TServer message attributes*/
public class ORSMetric extends Message {

    private static final Pattern regTMessageStart = Pattern.compile("^<(\\w+) sid='([\\w~]+)");
    private static final Pattern regReqID = Pattern.compile("^\\s*<eval_expr .+expression='system.LastSubmitRequestId;.+result='(\\d+)' />");

    public String sid = "";
    public String Method = "";
    private String param1;

    public ORSMetric(String line) {
        super(TableType.ORSMetric, line);
        FindSID();
    }

    ORSMetric(ArrayList m_MessageContents) {
        super(TableType.ORSMetric, m_MessageContents);
        FindSID();
    }

    ORSMetric(String line, String _Method, String _sid) {
        super(TableType.ORSMetric, line);
        Method = _Method;
        sid = _sid;
    }

    private void FindSID() {
        Matcher m;
        String s = m_MessageLines.get(0);

        if (s != null && (m = regTMessageStart.matcher(s)).find()) {
            Method = m.group(1);
            sid = m.group(2);
        }
    }

    public void setMethod(String Method) {
        this.Method = Method;
    }

    public String getXMLProperty(Document doc, String propertyName) {
        String ret = null;//doc.getElementsByTagName(propertyName);

        if (ret != null) {
            return ret;
        } else {
            return "";
        }
    }

    //   expression='system.LastSubmitRequestId;system.LastSubmitRequestId=_event.data.requestid;' result='216731' />
    int getReqID() {
        Matcher m;
        String s = m_MessageLines.get(0);

        if (s != null && (m = regReqID.matcher(s)).find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                Main.logger.error(e);
                return -1;
            }
        }
        return -1;
    }

    int getcgdbid() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    int getrecHandle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    int getchID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    String getSessID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getParam1() {
        return param1;
    }

    void setParam1(String group) {
        this.param1 = group;
    }

}
