/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSSCXMLScript extends Message {

    //-> SCXML : 0000017F-040206B5-0001  METRIC <extension name="set_flex_attr" namespace="http://genesyslabs.com/schemas/ocs/treatments" />
//
//-> SCXML : 0000017F-040206B5-0001  1 SetFlexAttrAction.Execute()
    private static final Matcher regMetric = Pattern.compile("^\\s*METRIC <(\\w+)").matcher("");
    private final String sessID;
    private final String rest;

    OCSSCXMLScript(String sessID, String rest) {
        super(TableType.OCSSCXMLScript);
        this.sessID = sessID;
        this.rest = rest;
    }

    String getSessID() {
        return sessID;
    }

    String getMetric() {
        return FindByRx(regMetric, rest, 1, "");
    }

}
