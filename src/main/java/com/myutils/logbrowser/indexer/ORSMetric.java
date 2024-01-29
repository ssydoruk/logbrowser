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
public class ORSMetric extends Message {

    private static final Pattern regTMessageStart = Pattern.compile("^<(\\w+) sid='([\\w~]+)");
    private static final Pattern regReqID = Pattern.compile("^\\s*<eval_expr .+expression='system.LastSubmitRequestId;.+result='(\\d+)' />");

    public String sid = "";
    public String Method = "";
    private String param1;

    public ORSMetric(String line, int fileID) {
        super(TableType.ORSMetric, line, fileID);
        FindSID();
    }

    ORSMetric(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.ORSMetric, m_MessageContents, fileID);
        FindSID();
    }

    ORSMetric(String line, String _Method, String _sid, int fileID) {
        super(TableType.ORSMetric, line, fileID);
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

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ORSSID, sid));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.METRIC, Method));
        stmt.setInt(8, getReqID());
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.METRIC_PARAM1, getParam1()));
        return true;

    }
}
