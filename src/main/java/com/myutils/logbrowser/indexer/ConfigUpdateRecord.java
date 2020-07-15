/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.intOrDef;
import java.util.ArrayList;

public class ConfigUpdateRecord extends Message {

    private String objName = "";
    private String objectType = "";
    private String objectDBID = "";
    private String op = "";
    private String msg = null;

    public ConfigUpdateRecord(ArrayList messageLines) {
        super(TableType.ConfigUpdate, messageLines);
    }

    ConfigUpdateRecord(String line) {
        super(TableType.ConfigUpdate, line);
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

}
