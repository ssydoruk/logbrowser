/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URSTargetSet extends Message {

    private static final Pattern ptTargetItem = Pattern.compile("^[\\(\\s]*(.+)@[^@]+\\.(\\w{1,2})[\\)\\s]*$");
    private String obj;
    private String objType;
    private String target;

    URSTargetSet(String target, String targetItem, int fileID) {
        this(target, fileID);
        Matcher m;
        boolean agentFound = false;
        if (targetItem != null) {
            if ((m = ptTargetItem.matcher(targetItem)).find()) {
                String type = m.group(2);
                if (type != null) {
                    objType = UrsParser.targetCode.get(type);
                    this.obj = m.group(1);
                    agentFound = true;
                }
            } else { // default object type
                String s = targetItem.trim();
                if (StringUtils.isNotBlank(s)) {
                    this.obj = s;
                    this.objType = UrsParser.targetCode.get("A");
                }
            }
        }
    }

    URSTargetSet(String target, int fileID) {
        this(fileID);
        this.target = target;
    }

    public URSTargetSet(int fileID) {
        super(TableType.URSTargetSet, fileID);
    }

    public String getObject() {
        return obj;
    }

    public String getObjectType() {
        return objType;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.URSTarget, getTarget()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.CfgObjName, getObject()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.CfgObjectType, getObjectType()));

        return true;

    }
}
