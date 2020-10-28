/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import com.myutils.logbrowser.inquirer.ILogRecord;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

/**
 *
 * @author stepan_sydoruk
 */
public class JSRunner {

    private JSRunner() {
        condContext = Context.newBuilder("js").allowAllAccess(true).build();
        condContext.eval("js", "true"); // to test javascript engine init. 
    }

    public static JSRunner getInstance() {
        return JSRunnerHolder.INSTANCE;
    }

    private static class JSRunnerHolder {

        private static final JSRunner INSTANCE = new JSRunner();
    }

    static private Context condContext = null;
    static final public String Inquirer_CLASS = LogBrowser.class.getName();

    public static class LogBrowser {

        private static ILogRecord curRec;

        @HostAccess.Export
        public static String recordField(String fld) {
            if (curRec != null) {
                return curRec.GetField(fld);
            } else {
                return "";
            }
        }

        public static void setCurrentRec(ILogRecord record) {
            curRec = record;
        }
    }

}
