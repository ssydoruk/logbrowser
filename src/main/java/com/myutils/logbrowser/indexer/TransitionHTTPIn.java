package com.myutils.logbrowser.indexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TransitionHTTPIn implements ParserThreadsProcessor.StateTransition {

    //        private static final Pattern regSessionStartParam = Pattern.compile("Session start param");
//        private static final Pattern regSessionStartMessage = Pattern.compile("HandleDataFromThread: Starting new session. SessionID=(\\w+)$");
//        private static final Pattern regFMSession = Pattern.compile("\\{FMSession:");
    private static final Pattern regHTTPINSessionCreate = Pattern.compile("\\s*OnRequestStart: Creating session. SessionID=([\\w~]+)$");
    private static final Pattern regSTATE_HTTPIN1 = Pattern.compile("\\+ORS_HTTPResponse\\[(.+)\\]$");
    private static final Pattern regSTATE_HTTPIN = Pattern.compile(" HandleRequest: |ORS_HTTPResponse");

    @Override
    public ParserThreadsProcessor.StateTransitionResult stateTransition(ParserThreadState threadState,
                                                                        String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
        Matcher m;
        OrsHTTP msg = (OrsHTTP) threadState.getMsg();

        switch (threadState.getParserState()) {
            case STATE_HTTPIN:

                if ((m = regSTATE_HTTPIN1.matcher(sOrig)).find()) {
                    msg.setHTTPResponseID(m.group(1));
                    return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED;
                } else if ((regSTATE_HTTPIN.matcher(sOrig)).find()) {
                    return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED;
                } else if ((m = regHTTPINSessionCreate.matcher(sParsedAndTruncated)).find()) {
                    msg.setSID(m.group(1));
                }
                msg.SetStdFieldsAndAdd(parser);
                return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED_CONTINUE;

            default:
                Main.logger.error("l:" + parser.getM_CurrentLine() + " unexpected state " + threadState.getParserState() + " s[" + sOrig + "]");

        }
        Main.logger.trace("SessionStartExtTransition "
                + "sOrig[" + sOrig + "] "
                + "threadID[" + threadID + "]"
                + "parser[" + parser + "]"
        );
        return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
    }

}
