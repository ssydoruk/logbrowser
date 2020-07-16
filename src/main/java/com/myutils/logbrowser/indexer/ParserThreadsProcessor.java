/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ssydoruk
 */
public class ParserThreadsProcessor {

    private boolean threadWaitsForNonthreadMessage = false;
    private ArrayList<StateTransition> stateTransitions;
    private HashMap<String, ParserThreadState> threadStates;
    public ParserThreadsProcessor() {
        stateTransitions = new ArrayList();
        threadStates = new HashMap<>();
    }
    boolean threadPending() {
        Main.logger.trace("threads pending: " + !threadStates.isEmpty());
        return !threadStates.isEmpty();
    }

    public void setThreadWaitsForNonthreadMessage(boolean threadWaitsForNonthreadMessage) {
        this.threadWaitsForNonthreadMessage = threadWaitsForNonthreadMessage;
    }

    public boolean isThreadWaitsForNonthreadMessage() {
        return threadWaitsForNonthreadMessage;
    }

    /**
     * public method to be called from ParseLine.
     *
     * @param sOrig - original string
     * @param sParsedAndTruncated - truncated string after date and thread cut
     * out
     * @param threadID - ID of the thread
     * @param parser - {@link Parser}
     * @return true - parsing done; caller should return null to the
     * {@link Parser.ParseFrom()} false - ParseLine should continue parsing the
     * line
     */
    boolean threadConsume(String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
        Main.logger.trace("threadConsume "
                + "sOrig[" + sOrig + "] "
                + "sParsedAndTruncated[" + sParsedAndTruncated + "] "
                + "threadID[" + threadID + "] "
                + "parser[" + parser + "] "
        );
        if (!threadPending()) {
            return false;
        }
        if (threadID != null) {
            ParserThreadState pts = threadStates.get(threadID);
            if (pts == null) {
                return false;
            } else {
                return transitionForThread(pts, sOrig, sParsedAndTruncated, threadID, parser);
            }
        } else { // if no threadID, checking if thread waits for non-thread message
            if (threadWaitsForNonthreadMessage) {
                for (Map.Entry<String, ParserThreadState> entry : threadStates.entrySet()) {
                    String theThreadID = entry.getKey();
                    ParserThreadState ss = entry.getValue();
                    if (ss.isWaitNonthread()) {
                        return transitionForThread(ss, sOrig, sParsedAndTruncated, theThreadID, parser);
                    }

                }

//                Main.logger.error("threadConsume: threadWaitsForNonthreadMessage [" + sOrig + "]" + "[" + threadID + "]");
            } else {
                // it is OK to receive non-thread message while waiting for thread message
                //Main.logger.error("threadConsume: non-threadWaitsForNonthreadMessage [" + sOrig + "]" + "[" + threadID + "]");

            }
            return false;
        }
    }

    private boolean transitionForThread(ParserThreadState pts,
            String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
        Main.logger.trace("transitionForThread "
                + "sOrig[" + sOrig + "] "
                + "threadID[" + threadID + "]"
                + "parser[" + parser + "]"
        );

        StateTransitionResult res = pts.stateTransition(sOrig, sParsedAndTruncated, threadID, parser);
        Main.logger.trace("threadConsume: res=" + res);

        threadWaitsForNonthreadMessage = false;
        pts.setWaitNonthread(false);
        switch (res) {
            case NON_STATE_LINE_WAITED:
                threadWaitsForNonthreadMessage = true;
                pts.setWaitNonthread(true);
                return true;

            case NON_STATE_LINE_WAITED_NOT_CONSUMED:
                threadWaitsForNonthreadMessage = true;
                pts.setWaitNonthread(true);
                return false;

            case NO_CHANGE:
                Main.logger.trace("NO_CHANGE returned by state transition: [" + sOrig + "]");
                return false;

            case STATE_CHANGED:
                return true;

            case FINAL_REACHED:
                threadStates.remove(threadID);
                return true;

            case FINAL_REACHED_CONTINUE:
                threadStates.remove(threadID);
                return false;

            case ERROR_STATE:
                Main.logger.debug("ERROR_STATE returned by state transition: [" + sOrig + "]");
                threadStates.remove(threadID);
                return false;

        }
        Main.logger.error("l:" + parser.getM_CurrentLine() + " - no transition for the state" + sParsedAndTruncated);
        threadStates.remove(threadID);
        return false;
    }
    public void addStateTransition(StateTransition stateTransition) {
        stateTransitions.add(stateTransition);
    }
    public void addThreadState(String threadID, ParserThreadState pts) {
        Main.logger.trace("addThreadState: " + threadID + " pts: " + pts.toString());
        threadStates.put(threadID, pts);
        setThreadWaitsForNonthreadMessage(pts.isWaitNonthread());
    }
    
//        boolean threadPending() {
//        for (ParserThreadsProcessor thi : this) {
//            if (thi.threadPending()) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * ThreadConsumeResult
     */
    public static enum ThreadConsumeResult {

    }

    /**
     * Returned by the state transition callback
     * <li> {@link #ERROR_STATE}</li>
     */
    public static enum StateTransitionResult {
        /**
         * Indicates that state transition did not consume line. Main parser
         * should continue parsing string
         */
        NO_CHANGE,
        /**
         * Final state reached; thread processing complete; line consumed
         */
        FINAL_REACHED,
        /**
         * same as previous, but continue processing the last line
         */
        FINAL_REACHED_CONTINUE,
        /**
         * STATE_CHANGED indicates that thread procedure consumed thread line
         * and is waiting for more messages. Line consumed
         */
        STATE_CHANGED,
        /**
         * NON_STATE_LINE_WAITED indicates that thread waits non-thread bound
         * message. Line consumed
         */
        NON_STATE_LINE_WAITED,
        /**
         * NON_STATE_LINE_WAITED_NOT_CONSUMED same as NON_STATE_LINE_WAITED, but
         * line not consumed
         */
        NON_STATE_LINE_WAITED_NOT_CONSUMED,
        /**
         * Unexpected line in the state. On this value state is cleaned. Line
         * not-consumed
         */
        ERROR_STATE
    };


    public static enum ParserState {
        STATE_HEADER,
        STATE_TMESSAGE,
        STATE_ORSMESSAGE,
        STATE_COMMENT,
        STATE_TMESSAGE_START,
        STATE_CLUSTER,
        STATE_HTTPIN,
        STATE_HTTPHANDLEREQUEST,
        STATE_ORSUS,
        STATE_TMESSAGE_REQUEST,
        STATE_EXTENSION_FETCH1,
        STATE_EXTENSION_FETCH2,
        STATE_ALARM,
        STATE_START_SESSION,
        STATE_START_SESSION1,
        STATE_SESSION_START,
        STATE_WAITING_PARENT_SID,
        STATE_HTTPOUT;
    };

    public interface StateTransition {
        
        abstract StateTransitionResult stateTransition(ParserThreadState threadState,
                String sOrig, String sParsedAndTruncated, String threadID, Parser parser);
    }
}
