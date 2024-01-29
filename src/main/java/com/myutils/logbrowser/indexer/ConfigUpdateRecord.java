/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static Utils.Util.intOrDef;

public class ConfigUpdateRecord extends Message {

    private String objName = "";
    private String objectType = "";
    private String objectDBID = "";
    private String op = "";
    private String msg = null;

    public ConfigUpdateRecord(ArrayList<String> messageLines, int fileID) {
        super(TableType.ConfigUpdate, messageLines, fileID);
    }

    ConfigUpdateRecord(String line, int fileID) {
        super(TableType.ConfigUpdate, line, fileID);
    }

    public String GetOp() {
        return op;
    }

    String GetObjType() {
        return objectType;
    }

    int GetObjDBID() {
        return intOrDef(objectDBID, -1);
    }

    String getObjName() {
        return objName;
    }

    void setObjName(String FindByRx) {
        objName = FindByRx;
    }

    void setObjectType(String group) {
        objectType = group;
    }

    void setObjectDBID(String group) {
        objectDBID = group;
    }

    void setOp(String FindByRx) {
        op = FindByRx;
    }

    String getMsg() {
        String ret = "";
        if (msg != null) {
            return msg;
        }
        if (objectType != null && objectType.isEmpty() && !m_MessageLines.isEmpty()) {
            ret = m_MessageLines.get(0);
            if (ret != null && !ret.isEmpty()) {
                return ret.substring(0, (ret.length() < 40) ? ret.length() : 40);
            }
        }
        return ret;
    }

    void setMsg(String group) {
        msg = group;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setInt(1, getRecordID());
        stmt.setTimestamp(2, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(3, getFileID());
        stmt.setLong(4, getM_fileOffset());
        stmt.setLong(5, getM_FileBytes());
        stmt.setLong(6, getM_line());

        setFieldInt(stmt, 7, Main.getRef(ReferenceType.CfgOp, GetOp()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.CfgObjectType, GetObjType()));
        stmt.setInt(9, GetObjDBID());
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.CfgObjName, getObjName()));
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.CfgMsg, getMsg()));
        return true;
    }
}
