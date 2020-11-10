/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import com.myutils.logbrowser.inquirer.ILogRecord;
import java.util.HashMap;
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

    /**
     *
     * @param script
     * @param rec
     * @param scriptFields
     * @return true if record should be ignored (based on balue of boolean
     * IGNORE_RECORD calculated by the script
     */
    public static boolean evalFields(String script, ILogRecord rec, HashMap<String, String> scriptFields) {
        Context cont = getInstance().getCondContext();
        Value bindings = cont.getBindings("js");
        bindings.putMember("RECORD", rec);
        bindings.putMember("FIELDS", scriptFields);
        bindings.putMember("IGNORE_RECORD", false);
        cont.eval("js", script);
        boolean ret = bindings.getMember("IGNORE_RECORD").asBoolean();
        logger.debug("evalFields [" + scriptFields + "], ignored:[" + ret + "] - result of [" + script + "]");
        return ret;
    }

    public static String execString(String script, ILogRecord rec) {
        Context cont = getInstance().getCondContext();
        cont.getBindings("js").putMember("RECORD", rec);
        Value eval = cont.eval("js", script);
        logger.debug("eval [" + eval + "] - result of [" + script + "]");
        return eval.asString();

    }

    public static boolean execBoolean(String script, ILogRecord rec) {
        Context cont = getInstance().getCondContext();
        cont.getBindings("js").putMember("RECORD", rec);
        Value eval = cont.eval("js", script);
        logger.debug("eval [" + eval + "] - result of [" + script + "]");
        return eval.asBoolean();

    }

}
