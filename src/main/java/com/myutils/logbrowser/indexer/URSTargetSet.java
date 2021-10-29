/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URSTargetSet extends Message {

    private static final Matcher ptTargetItem = Pattern.compile("^[\\(\\s]*(.+)@[^@]+\\.(\\w{1,2})[\\)\\s]*$").matcher("");
    private String obj;
    private String objType;
    private String target;

    URSTargetSet(String target, String targetItem) {
        this(target);
        Matcher m;
        boolean agentFound = false;
        if (targetItem != null) {
            if ((m = ptTargetItem.reset(targetItem)).find()) {
                String type = m.group(2);
                if (type != null) {
                    objType = UrsParser.targetCode.get(type);
                    this.obj = m.group(1);
                    agentFound = true;
                }
            }
            else { // default object type
                String s = targetItem.trim();
                if(StringUtils.isNotBlank(s)){
                    this.obj=s;
                    this.objType=UrsParser.targetCode.get("A");
                }
            }
        }
    }

    URSTargetSet(String target) {
        this();
        this.target = target;
    }

    public URSTargetSet() {
        super(TableType.URSTargetSet);
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

}
