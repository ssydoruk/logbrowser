/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;

/**
 *
 * @author ssydoruk
 */
public class ParserThreadState {

    private ParserThreadsProcessor.StateTransition transition;

    ParserThreadState(ParserThreadsProcessor.StateTransition transition, ParserThreadsProcessor.ParserState initialState) {
        this.parserState = initialState;
        msgLines = new ArrayList<>();
        this.transition = transition;
    }

    public ParserThreadsProcessor.StateTransitionResult stateTransition(String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
        Main.logger.trace("transitionForThread "
                + "sOrig[" + sOrig + "] "
                + "threadID[" + threadID + "] "
                + "parser[" + parser + "] "
                + "tps[" + this + "]"
        );
        return transition.stateTransition(this, sOrig, sParsedAndTruncated, threadID, parser);
    }

    @Override
    public String toString() {
        return "ParserThreadState{" + "waitNonthread=" + waitNonthread + ", msg=" + msg + ", msgLines=" + msgLines + ", filePos=" + filePos + ", bytes=" + bytes + ", parserState=" + parserState + ", headerOffset=" + headerOffset + '}';
    }

    private boolean waitNonthread;

    public void setWaitNonthread(boolean waitNonthread) {
        this.waitNonthread = waitNonthread;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) throws CloneNotSupportedException {
        this.msg = (Message) msg.clone();
//        Main.logger.info("src: "+msg.getClass()+" dst: "+this.msg.getClass());
        int i = 1;
    }

    public ArrayList<String> getMsgLines() {
        return msgLines;
    }

    public void setMsgLines(ArrayList<String> msgLines) {
        this.msgLines = msgLines;
    }

    public long getFilePos() {
        return filePos;
    }

    public void setFilePos(long filePos) {
        this.filePos = filePos;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    private Message msg;
    private ArrayList<String> msgLines;
    private long filePos;
    private long bytes;
    private ParserThreadsProcessor.ParserState parserState;
    private long headerOffset;

    public long getHeaderOffset() {
        return headerOffset;
    }

    public void setHeaderOffset(long headerOffset) {
        this.headerOffset = headerOffset;
    }

    public ParserThreadsProcessor.ParserState getParserState() {
        return parserState;
    }

    public void setParserState(ParserThreadsProcessor.ParserState parserState) {
        this.parserState = parserState;
    }

    void addString(String MetricClause) {
        msgLines.add(MetricClause);
    }

    boolean isWaitNonthread() {
        return this.waitNonthread;
    }

}
