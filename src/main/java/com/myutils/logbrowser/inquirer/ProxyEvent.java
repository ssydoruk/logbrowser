/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author ssydoruk
 */
public class ProxyEvent extends ILogRecord {

    public ProxyEvent(ResultSet rs) throws SQLException {
        super(rs, MsgType.PROXY);
        m_fields.put("name", rs.getString("name"));
        String connId = rs.getString("connid");
        m_fields.put("connid", connId == null ? "" : connId);
        String dn = rs.getString("thisdn");
        m_fields.put("thisdn", dn == null ? "" : dn);
        String odn = rs.getString("otherdn");
        m_fields.put("otherdn", odn == null ? "" : odn);
        String dest = rs.getString("destination");
        m_fields.put("destination", dest == null ? "" : dest);

        int type = rs.getInt("component");
        m_fields.put("comptype", new Integer(type));
        String nodeId = rs.getString("nodeid");
        String comp = "";
        String shortComp = "";
        switch (type) {
            case 0:
                comp = nodeId + " sessionController";
                shortComp = "SC";
                break;
            case 3:
                comp = nodeId + " interactionProxy";
                shortComp = "IP";
                break;
            case 4:
                comp = nodeId + " tController";
                shortComp = "TC";
                break;
            case 5:
                shortComp = "URS";
                break;
            case 7:
                shortComp = "STS";
                break;
            case 8:
                shortComp = "ICON";
                break;
        }
        m_fields.put("component", comp);
        m_fields.put("comp", shortComp);

        m_fields.put("tlib_id", new Integer(rs.getInt("tlib_id")));
        String refid = rs.getString("refid");
        m_fields.put("refid", refid == null ? "" : refid);
    }

}
