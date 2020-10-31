/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import com.myutils.logbrowser.inquirer.ILogRecord;
import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

/**
 *
 * @author stepan_sydoruk
 */
public class JSRunner {

    private JSRunner() {
        condContext = Context.newBuilder("js").allowAllAccess(true).build();
        condContext.eval("js", "true"); // to test javascript engine init. 
    }

    public Context getCondContext() {
        return condContext;
    }

    public static JSRunner getInstance() {
        return JSRunnerHolder.INSTANCE;
    }

    private static class JSRunnerHolder {

        private static final JSRunner INSTANCE = new JSRunner();
    }

    private Context condContext = null;
    static final public String LogBrowser_CLASS = LogBrowser.class.getName();

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

        @HostAccess.Export
        public static String fieldName(String fld) {
            return fld + "++";
        }

        public static void setCurrentRec(ILogRecord record) {
            curRec = record;
        }
    }
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    public static String execString(String script, ILogRecord rec) {
        Context condContext = getInstance().getCondContext();
        condContext.getBindings("js").putMember("record", rec);
        Value eval = JSRunner.getInstance().getCondContext().eval("js", script);
        logger.debug("eval [" + eval + "] - result of [" + script + "]");
        return eval.asString();

    }

}
