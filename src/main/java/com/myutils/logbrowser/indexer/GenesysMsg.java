/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenesysMsg extends Message {

    private static final Matcher regMsg = Pattern.compile("^\\s*(None|Debug|Trace|Interaction|Standard|Alarm|Unknown|Non|Dbg|Trc|Int|Std|Alr|Unk) (\\d{5})\\s*").matcher("");
    private static final Matcher regMsgSCS = Pattern.compile("^\\s*(None|Debug|Trace|Interaction|Standard|Alarm|Unknown|Non|Dbg|Trc|Int|Std|Alr|Unk)(?:\\s+(?:\\S+)){3}.+-(?:\\d{2})-(\\d{5})").matcher("");
    static private final Matcher ptThreadID = Pattern.compile("^(\\[\\S+\\])$").matcher("");
    static GenesysMsgMap msgMap = new GenesysMsgMap();
    private final String level;
    private final String msgID;
    //    private static final Pattern regMsgWords = Pattern.compile("([a-zA-Z]+)");
//    private static final int MAX_LOG_WORDS = 3;
    private String lastGenesysMsgLevel;
    private String lastGenesysMsgID;
    private boolean savedToDB = false;
    private boolean toIgnore = false;
    private FileInfoType msgType;
    private String line;

    public GenesysMsg(TableType t, String level, String msgID, String line) {
        super(t);
        this.level = level;
        this.msgID = msgID;
        this.line = line;
    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Pattern reg, Pattern ignoreMSGIDs) {
        return CheckGenesysMsg(dp, p, t, reg, ignoreMSGIDs, false);
    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Matcher reg, Matcher ignoreMSGIDs) {
        return CheckGenesysMsg(dp, p, t, reg, ignoreMSGIDs, false);
    }

    public static GenesysMsg postGenesysMsg(DateParsed dp, Parser p, TableType t, Pattern ignoreMSGIDs,
                                            String _lastGenesysMsgLevel, String _lastGenesysMsgID, String generatedMsgID, String generatedMsg,
                                            boolean saveToDB) {
        return postGenesysMsg(dp, p, t, ignoreMSGIDs.matcher(""),
                _lastGenesysMsgLevel, _lastGenesysMsgID, generatedMsgID, generatedMsg,
                saveToDB);
    }

    public static GenesysMsg postGenesysMsg(DateParsed dp, Parser p, TableType t, Matcher ignoreMSGIDs,
                                            String _lastGenesysMsgLevel, String _lastGenesysMsgID, String generatedMsgID, String generatedMsg,
                                            boolean saveToDB) {
        GenesysMsg msg;

        msg = new GenesysMsg(t, _lastGenesysMsgLevel, generatedMsgID, generatedMsg);

        msg.setM_Timestamp(dp);
        msg.SetOffset(p.getFilePos());
        msg.SetFileBytes(p.getEndFilePos() - p.getFilePos());
        msg.SetLine(p.getLineStarted());
        msg.setLastGenesysMsgID(_lastGenesysMsgID);
        if (ignoreMSGIDs != null && ignoreMSGIDs.reset(_lastGenesysMsgID).find()) {
            msg.setToIgnore(true);
        }
        if (!msg.isToIgnore() && saveToDB) {
            msg.AddToDB(p.m_tables);
            Main.logger.traceExit("Added log message " + t + ": " + msg);

        }
        return msg;
    }

    public static GenesysMsg postGenesysMsg(DateParsed dp, Parser p, TableType t, Pattern ignoreMSGIDs,
                                            String _lastGenesysMsgLevel, String _lastGenesysMsgID, String generatedMsgID, String generatedMsg) {
        return postGenesysMsg(dp, p, t, ignoreMSGIDs,
                _lastGenesysMsgLevel, _lastGenesysMsgID, generatedMsgID, generatedMsg, true);
    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Pattern reg, Pattern ignoreMSGIDs,
                                             boolean useFirstWord, boolean saveToDB) {
        return CheckGenesysMsg(dp, p, t, reg.matcher(""), ignoreMSGIDs.matcher(""),
                useFirstWord, true);

    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Matcher reg, Matcher ignoreMSGIDs,
                                             boolean useFirstWord, boolean saveToDB) {
        Matcher m;
        GenesysMsg msg = null;
        String _lastGenesysMsgLevel;
        String _lastGenesysMsgID;
        String generatedMsgID = null;
        String generatedMsg = null;

        if (dp != null && (m = reg.reset(dp.rest)).find()) {
            try {
                dp.rest = dp.rest.substring(m.end());
                _lastGenesysMsgLevel = m.group(1);
                _lastGenesysMsgID = m.group(2);
                if (useFirstWord) {
                    String[] split = StringUtils.split(dp.rest, " :,", 3);
                    if (split != null && split.length > 0) {
                        if (ptThreadID.reset(split[0]).find()) {
                            if (split.length > 1) {
                                generatedMsgID = split[1];
                                generatedMsg = (split.length > 2) ? split[2] : "";
                            }
                        } else {
                            generatedMsgID = split[0];
                            generatedMsg = split[1];
                        }
                    }
                } else {
                    generatedMsgID = _lastGenesysMsgID;
                    generatedMsg = dp.rest;
                }

                return postGenesysMsg(dp, p, t, ignoreMSGIDs, _lastGenesysMsgLevel, _lastGenesysMsgID, generatedMsgID, generatedMsg, saveToDB);
            } catch (Exception e) {
                Main.logger.error("Not added \"" + ((msg != null) ? msg.getM_type() : "[none]") + "\" record:" + e.getMessage(), e);
            }
        }

        return null;

    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Pattern reg, Pattern ignoreMSGIDs,
                                             boolean useFirstWord) {
        return CheckGenesysMsg(dp, p, t, reg, ignoreMSGIDs,
                useFirstWord, true);

    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Matcher reg, Matcher ignoreMSGIDs,
                                             boolean useFirstWord) {
        return CheckGenesysMsg(dp, p, t, reg, ignoreMSGIDs,
                useFirstWord, true);

    }

    public static GenesysMsg CheckGenesysMsgSCS(DateParsed dp, Parser p, TableType t, Pattern ignoreMSGIDs) {
        GenesysMsg ret = CheckGenesysMsg(dp, p, t, regMsgSCS, ignoreMSGIDs.matcher(""));
        if (ret != null) {
            return ret;
        } else {
            return CheckGenesysMsg(dp, p, t, regMsg, ignoreMSGIDs.matcher(""));
        }
    }

    public static GenesysMsg CheckGenesysMsgGMS(DateParsed dp, Parser p, TableType t, Pattern ignoreMSGIDs) {
        return CheckGenesysMsg(dp, p, t, regMsg, ignoreMSGIDs.matcher(""), true);
    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Pattern ignoreMSGIDs) {
        return CheckGenesysMsg(dp, p, t, regMsg, ignoreMSGIDs.matcher(""));
    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Matcher ignoreMSGIDs) {
        return CheckGenesysMsg(dp, p, t, regMsg, ignoreMSGIDs);
    }

    public static GenesysMsg CheckGenesysMsg(DateParsed dp, Parser p, TableType t, Pattern ignoreMSGIDs, boolean saveToDB) {
        return CheckGenesysMsg(dp, p, t, regMsg, ignoreMSGIDs.matcher(""), false, saveToDB);
    }

    static void updateMsg() {
        for (Map.Entry<String, ArrayList<String>> entry : msgMap.entrySet()) {
            StringBuilder s = new StringBuilder(120);
            s.append(entry.getKey());
            for (String string : entry.getValue()) {
                s.append(" ").append(string);
            }
            Main.getMain().tabRefs.updateRef(ReferenceType.LOGMESSAGE, entry.getKey(), s.toString());
        }
    }

    /**
     * Get the value of savedToDB
     *
     * @return the value of savedToDB
     */
    public boolean isSavedToDB() {
        return savedToDB;
    }

    /**
     * Set the value of savedToDB
     *
     * @param savedToDB new value of savedToDB
     */
    public void setSavedToDB(boolean savedToDB) {
        this.savedToDB = savedToDB;
    }

    public String getLastGenesysMsgLevel() {
        return lastGenesysMsgLevel;
    }

    public String getLastGenesysMsgID() {
        return lastGenesysMsgID;
    }

    public void setLastGenesysMsgID(String lastGenesysMsgID) {
        this.lastGenesysMsgID = lastGenesysMsgID;
    }

    /**
     * Get the value of toIgnore
     *
     * @return the value of toIgnore
     */
    public boolean isToIgnore() {
        return toIgnore;
    }

    /**
     * Set the value of toIgnore
     *
     * @param toIgnore new value of toIgnore
     */
    public void setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
    }

    public String getLevelString() {
        return level;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return super.toString() + "level:" + level + ";msgID=" + msgID + ";msgType=" + msgType + "line:[" + line + "]}";
    }

    int getLevel() {
        return Main.getRef(ReferenceType.MSGLEVEL, level);
    }

    int getMsgID() {
        msgMap.addMap(msgID, line);

        return Main.getRef(ReferenceType.LOGMESSAGE, msgID, 1);
    }

    //    int getMsgID() {
//        Matcher m;
//        StringBuilder buf = new StringBuilder(line.length() + msgID.length());
//
//        buf.append(msgID);
//
//        m = regMsgWords.matcher(line);
//        int i = 0;
//        while (m.find() && i < MAX_LOG_WORDS) {
//            buf.append(" ").append(m.group(1));
//            i++;
//        }
//
//        return Main.getRef(ReferenceType.LOGMESSAGE, buf.toString());
//    }
    static class GenesysMsgMap extends HashMap<String, ArrayList<String>> {

        private void addMap(String msgID, String line) {
            ArrayList<String> storedLine = get(msgID);
            if (storedLine == null) {
                put(msgID, new ArrayList(Arrays.asList(StringUtils.split(line))));
            } else {
                String[] split = StringUtils.split(line);
                HashSet<String> oldWords = new HashSet<>(storedLine);
                oldWords.retainAll(Arrays.asList(split));
                ArrayList<String> updatedStored = new ArrayList<>();
                for (String string : storedLine) {
                    if (oldWords.contains(string)) {
                        updatedStored.add(string);
                    }
                }
                put(msgID, updatedStored);
            }
        }

    }
}

class GenesysMsgs {

}
