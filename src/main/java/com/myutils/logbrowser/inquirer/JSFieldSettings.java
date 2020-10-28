/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.io.Serializable;

/**
 *
 * @author stepan_sydoruk
 */
public class JSFieldSettings implements Serializable {

    private static final long serialVersionUID = 1L;
    private String jsScript;

    private boolean perLine;



    public JSFieldSettings( String jsScript, boolean perLine) {
        this.jsScript = jsScript;
        this.perLine = perLine;

    }

    public JSFieldSettings(JSFieldSettings currentRegexSetting) {
        this.jsScript = currentRegexSetting.getJsScript();
        this.perLine = currentRegexSetting.isPerLine();

    }

    public String getJsScript() {
        return jsScript;
    }

    public boolean isPerLine() {
        return perLine;
    }

    void updateParams(String jsScript, boolean perLine) {
        this.jsScript = jsScript;
        this.perLine = perLine;
    }

    public String evalValue(String bytes) {
        return "hello world";
    }
}
