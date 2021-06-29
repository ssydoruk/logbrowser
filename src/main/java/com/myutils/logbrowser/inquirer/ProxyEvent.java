/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author ssydoruk
 */
public class ProxyEvent extends ILogRecord {

    public ProxyEvent(ResultSet rs) throws SQLException {
        super(rs, MsgType.PROXY);


    }

    @Override
    void initCustomFields() {

    }

    @Override
    HashMap<String, Object> initCalculatedFields(ResultSet rs) throws SQLException {
        HashMap<String, Object> ret = new HashMap<>();
        int type = rs.getInt("component");
        ret.put("comptype", new Integer(type));
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
        ret.put("component", comp);
        ret.put("comp", shortComp);
        return ret;
    }

}
