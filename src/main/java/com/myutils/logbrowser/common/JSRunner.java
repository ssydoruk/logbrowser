/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.myutils.logbrowser.inquirer.ILogRecord;


/**
 * @author stepan_sydoruk
 */


public class JSRunner {

    static final public String LogBrowser_CLASS = LogBrowser.class.getName();
    static final boolean isDebug = System.getProperties().containsKey("debug");
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
	private static final HashMap<String, Source> sourceFiles = new HashMap<>();
    private Context condContext = null;

    private JSRunner() {
        try {
            init();
        } catch (IOException ex) {
            logger.error("exception in JSRunner: " + ex);
        }
    }

    public static JSRunner getInstance() {
        return JSRunnerHolder.INSTANCE;
    }

    /**
     * @param script
     * @param rec
     * @param scriptFields
     * @return true if record should be ignored (based on value of boolean
     * IGNORE_RECORD calculated by the script
     */
    public static boolean evalFields(String script, ILogRecord rec, ScriptFields scriptFields) {
        Context cont = getInstance().getCondContext();
        Value bindings = cont.getBindings("js");
        bindings.putMember("RECORD", rec);
        bindings.putMember("FIELDS", scriptFields);
        bindings.putMember("IGNORE_RECORD", false);
        try {
            cont.eval("js", script);
        } catch (Exception e) {
            logger.error("Exception evaluating script [" + script + "]", e);
        }
        boolean ret = bindings.getMember("IGNORE_RECORD").asBoolean();
        logger.trace("evalFields [" + scriptFields + "], ignored:[" + ret + "] - result of [" + script + "]");
        return ret;
    }


    public static RecordPrintout evalFullRecordPrintout(String script, ILogRecord rec) {
        RecordPrintout ret = new RecordPrintout(rec.getBytes());
        Context cont = getInstance().getCondContext();
        Value bindings = cont.getBindings("js");
        bindings.putMember("RECORD", rec);
        bindings.putMember("PRINTOUT", ret);

        try {
            cont.eval("js", script);
        } catch (Exception e) {
            logger.error("Exception evaluating script [" + script + "]", e);
        }
        return ret;
    }


    public static String execString(String script, ILogRecord rec) {
        Context cont = getInstance().getCondContext();
        cont.getBindings("js").putMember("RECORD", rec);
        Value eval = cont.eval("js", script);
        logger.trace("eval [" + eval + "] - result of [" + script + "]");
        return eval.asString();

    }

    public static boolean execBoolean(String script, ILogRecord rec) {
        Context cont = getInstance().getCondContext();
        cont.getBindings("js").putMember("RECORD", rec);
        Value eval = cont.eval("js", script);
        logger.trace("eval [" + eval + "] - result of [" + script + "]");
        return eval.asBoolean();

    }

    static public boolean isDebug() {
        return isDebug;
    }

    private static Source getSource(String fileName) throws IOException {
        if (isDebug()) {
            return Source.newBuilder("js", new File(fileName)).cached(false).build();
        } else {
            Source source = sourceFiles.get(fileName);
            if (source == null) {
                source = Source.newBuilder("js", new File(fileName)).build();
                sourceFiles.put(fileName, source);
            }
            return source;
        }
    }

    private void init() throws IOException {
        PipedInputStream pipedStdOutReader = new PipedInputStream();
        PipedInputStream pipedStdErrReader = new PipedInputStream();

        OutReaderThread stdInReader = new OutReaderThread(pipedStdOutReader, Level.INFO);
        OutReaderThread stdErrReader = new OutReaderThread(pipedStdErrReader, Level.ERROR);
        PipedOutputStream stdOut = new PipedOutputStream(pipedStdOutReader);
        PipedOutputStream stdErr = new PipedOutputStream(pipedStdErrReader);

        Handler logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                System.out.println("-- publish: " + record.getMessage());
            }

            @Override
            public void flush() {
                System.out.println("--flush");
            }

            @Override
            public void close() throws SecurityException {
                System.out.println("--close");
            }
        };

        logHandler.setLevel(java.util.logging.Level.FINEST);
        condContext = Context.newBuilder("js")
                .allowAllAccess(true)
                .err(stdErr)
                .out(stdOut)
                .logHandler(stdOut)
//                .option("inspect", "8123")
//                .option("inspect.WaitAttached", "true")
//                .option("inspect.Secure", "false")
                .build();
        stdInReader.start();
        stdErrReader.start();
        condContext.eval("js", "true"); // to test javascript engine init.

    }

    public Context getCondContext() {
        return condContext;
    }

    private static class JSRunnerHolder {

        private static final JSRunner INSTANCE = new JSRunner();
    }

    public static class LogBrowser {

        private static ILogRecord curRec;

        @HostAccess.Export
        public static String recordField(String fld) {
            if (curRec != null) {
                return curRec.getFieldValue(fld);
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

    class OutReaderThread extends Thread {

        private final PipedInputStream pipedIn;
        private final Level logLevel;

        private OutReaderThread(PipedInputStream pipedStdIn, Level level) {
            pipedIn = pipedStdIn;
            logLevel = level;
        }

        @Override
        public void run() {
            logger.debug("started JSreader for log level " + logLevel);
            try {
                String s;
                BufferedReader br = new BufferedReader(new InputStreamReader(pipedIn));
                while ((s = br.readLine()) != null) {
                    logger.log(logLevel, s);
                }
            } catch (IOException ex) {
                logger.error("Exception while reading js output", ex);
            }
            logger.debug("JSreader thread done");
        }

    }

}
