/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.w3c.dom.Document;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ssydoruk
 */

/*It extends TServer message, however there is now TServer message attributes*/
public class ORSMetricExtension extends Message {

    private static final Pattern regTMessageStart = Pattern.compile("^<([\\w~]+) sid='([\\w~]+)");
    //    private static Matcher regReqID = Pattern.compile("^\\s*<eval_expr .+expression='system.LastSubmitRequestId;.+result='(\\d+)' />");
    private static final Pattern regFunc = Pattern.compile("(?:function|name)='([^']+)'");
    private static final Pattern regNS = Pattern.compile("namespace='.+\\/([^']+)'");
    private static final Pattern regNamespace = Pattern.compile("namespace='[^']/([^\\/]+)'");

    public String sid = "";
    public String Method = "";
    private String param1 = null;
    private String param2 = null;
    private String ns = null;

    public ORSMetricExtension(String line, int fileID) {
        super(TableType.ORSMetricExtension, line, fileID);
        FindSID();
    }

    ORSMetricExtension(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.ORSMetricExtension, m_MessageContents, fileID);
        FindSID();
    }

    ORSMetricExtension(String line, String _Method, String _sid, int fileID) {
        super(TableType.ORSMetricExtension, line, fileID);
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

    public String getXMLProperty(Document doc, String propertyName) {
        String ret = null;//doc.getElementsByTagName(propertyName);

        if (ret != null) {
            return ret;
        } else {
            return "";
        }
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

    String getMethodFunc() {
        String line = m_MessageLines.get(0);
        Matcher m;
        String func;
        if ((m = regFunc.matcher(line)).find()) {
            func = m.group(1);
        } else {
            func = line;
        }
        return func;
    }

    String getNameSpace() {
        if (ns == null) {
            String line = m_MessageLines.get(0);
            Matcher m;
            if (line != null && (m = regNS.matcher(line)).find()) {
                ns = m.group(1);
            }
        }
        return ns;
    }

    void setFetchMethod(String group) {
        this.param1 = group;
    }

    void setFetchURI(String group) {
        this.param2 = group;
    }

    String getParam1() {
        return param1;

    }

    String getParam2() {
        return param2;
    }

    void parseNS(String rest) {
        Main.logger.trace("r: " + rest);
        if (rest != null) {
            Matcher m1;
            if ((m1 = regNamespace.matcher(rest)).find()) {
                ns = m1.group(1);
            }
        }
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ORSSID, sid));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSNS, getNameSpace()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.METRICFUNC, getMethodFunc()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.METRIC_PARAM1, getParam1()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.METRIC_PARAM2, getParam2()));
        return true;

    }
}
