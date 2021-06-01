/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import org.apache.logging.log4j.LogManager;

import java.io.Serializable;

/**
 * @author stepan_sydoruk
 */
public class CustomField implements Serializable {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private final JSFieldSettings jsFieldSettings;
    private final RegexFieldSettings rxFieldSettings;
    private String name;

    public CustomField(String name, JSFieldSettings jsfs) {
        this.name = name;
        this.jsFieldSettings = jsfs;
        rxFieldSettings = null;
    }

    public CustomField(String name, RegexFieldSettings rxfs) {
        this.name = name;
        this.rxFieldSettings = rxfs;
        jsFieldSettings = null;
    }

    public boolean isJSField() {
        return jsFieldSettings != null;
    }

    public boolean isRXField() {
        return rxFieldSettings != null;
    }

    public JSFieldSettings getJsFieldSettings() {
        return jsFieldSettings;
    }

    public RegexFieldSettings getRxFieldSettings() {
        return rxFieldSettings;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getCustomValue(ILogRecord rec) {
        if (jsFieldSettings != null) {
            return jsFieldSettings.evalValue(rec);
        } else {
            logger.info("calling custom field without input. Allowed only on js custom field");
            return "";
        }
    }

    public String getCustomValue(String bytes) {
        if (jsFieldSettings != null) {
            return jsFieldSettings.evalValue(bytes);
        }
        if (rxFieldSettings != null) {
            return rxFieldSettings.evalValue(bytes);
        }
        return this.getClass().getCanonicalName();
    }
}
