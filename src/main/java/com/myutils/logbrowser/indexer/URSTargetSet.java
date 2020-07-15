/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URSTargetSet extends Message {

    private String obj;

    private static final Pattern ptTargetItem = Pattern.compile("^[\\(\\s]*(.+)@[^@]+\\.(\\w{1,2})[\\)\\s]*$");
    private String objType;

    URSTargetSet(String target, String targetItem) {
        this(target);
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
            }
            if (agentFound == false) {
                Main.logger.info("Not an agent [" + targetItem + "]; target[" + target + "]");
                this.obj = targetItem;
            }
        }
    }

    URSTargetSet(String target) {
        this();
        this.target = target;
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
    private String target;

    public URSTargetSet() {
        super(TableType.URSTargetSet);
    }

}
