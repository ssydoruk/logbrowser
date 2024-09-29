/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.StripQuotes;
import static Utils.Util.intOrDef;

@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation", "this-escape"})
public abstract class Message extends Record {

    private static final Pattern ptAttrInBrackets = Pattern.compile("\\[(.+)\\]$");
    private static final String udPref = "\t\t";
    private static final Pattern regLongAttribute = Pattern.compile("(\\d+)$");
    private static final Pattern regStringAttribute = Pattern.compile("\\\"(.+)\\\"$");
    private static final Pattern regAttributeInList = Pattern.compile("^\\s{2,}");
    private static final Pattern regListKeyStart = Pattern.compile("^\\s+'");
    private static final Pattern regListKeyEnd = Pattern.compile("^'\\s");
    private static final Pattern regHexAttribute = Pattern.compile("(\\w+)$");
    private static final Pattern SIPHeaderVal = Pattern.compile("^\\s*:\\s*(.+)");
    private static final Pattern regSIPDN = Pattern.compile("^([^@]+)");
    private static final Pattern regSIPURI = Pattern.compile("^(?:\\\")?(?:[^<\\\"]*)(?:\\\")?[ ]*(?:<)?(?:sip(?:s)?|tel):(?<userpart>[^@>]+)(?:@(?<host>[^;>]+)(?:>)?)?");
    private static final Pattern regSIPURIMediaService = Pattern.compile("media-service=(\\w+)");
    private static final Pattern regErrorMessage = Pattern.compile("^\\s+\\((\\w.+)\\)$");
    static long m_timezone;
    private final HashMap<String, Pattern> cfgAttrNames = new HashMap<>();

    protected ArrayList<String> m_MessageLines = new ArrayList<>();
    private boolean m_isInbound;
    private ArrayList<String> m_MMUserData = null;
    private ArrayList<String> m_MMEnvelope = null;

    protected Message(String type, int fileID) {
        super(type, fileID);
        m_MessageLines = new ArrayList<>(1); // by default store only one line

    }

    protected Message(TableType type, int fileID) {
        super(type, fileID);
        m_MessageLines = new ArrayList<>(1); // by default store only one line
    }

    protected Message(TableType type, String line, int fileID) {
        super(type, fileID);
        m_MessageLines = new ArrayList<>(1); // by default store only one line
        m_MessageLines.add(line);
    }

    protected Message(TableType type, ArrayList<String> m_MessageLines, int fileID) {
        super(type, fileID);
        setMessageLines(m_MessageLines);
    }

    public static JSONObject jsonOrDef(JSONObject _jsonBody, String name) throws JSONException {
        if (_jsonBody.has(name)) {
            return (JSONObject) _jsonBody.get(name);
        } else {
            return null;
        }
    }

    public static String jsonStringOrDef(JSONObject _jsonBody, String name, Object def) throws JSONException {
        if (_jsonBody.has(name)) {
            String ret = _jsonBody.getString(name);
            if (ret == null || ret.isEmpty()) {
                return (String) def;
            } else {
                return ret;
            }
        }
        return (String) def;
    }

    protected static String getGroup2Or3(Matcher m) {
        String ret = m.group(2);
        if (ret == null) {
            ret = m.group(3);
        }
        return ret;
    }

    public static void SetTimezone(long offset) {
        m_timezone = offset;
    }

    static String getRx(String s, Pattern reg, int i, String def) {
        Matcher m;

        if (s != null && !s.isEmpty() && (m = reg.matcher(s)).find()) {
            return m.group(i);
        }
        return def;

    }

    static String replaceElement(String s, Matcher m, int i, String replace) {
        return s.substring(0, m.start(i)) +
                replace +
                s.substring(m.end(i));
    }

    static Pair<String, String> getLiteralReplace(String s, String searchString, String replaceString) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    static Pair<String, String> getRxReplace(String s, Pattern reg, int i, String replace) {
        Matcher m = null;

        try {
            if (s != null && !s.isEmpty() && (m = reg.matcher(s)).find() && m.group(i) != null && !m.group(i).contains("<")
                    && !m.group(i).contains(">")) {
                return new Pair<>(m.group(i), replaceElement(s, m, i, replace));
            } else {
                return null;
            }
        } catch (Exception e) {
            Main.logger.error("s[" + s + "] reg[" + reg.toString() + "] i:" + i + " replace[" + replace + "] m:" + ((m == null) ? "<null>" : m.groupCount() + " st:" + m.start(i) + " e:" + m.end(i)));
            return null;
        }
    }

    static ArrayList<String> getAllExtentions(ArrayList<String> m_MessageContents) {
        return getAllExtendedAttribute(m_MessageContents, "AttributeExtensions");
    }

    static ArrayList<String> getAllExtendedAttribute(ArrayList<String> m_MessageContents, String attrName) {
        ArrayList<String> ret = null;
        if (m_MessageContents != null) {
            int idx = lookupAttributeIdx("AttributeExtensions", m_MessageContents);
            if (idx >= 0) {
                ret = new ArrayList<>();
                for (int i = idx; i < m_MessageContents.size(); i++) {
                    String str = m_MessageContents.get(i);
                    if (str != null && !str.isEmpty()) {
                        ret.add(str);
                    }
                }
                if (ret.isEmpty()) {
                    ret = null;
                }
            }
        }
        return ret;
    }

    public static double parseOrDef(String s, Double def) {
        if (s != null) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                //
            }
        }
        return def;

    }

    public static Integer FindByRx(List<String> msgLines, Pattern rx, int groupId, Integer def) {
        return intOrDef(FindByRx(msgLines, rx, groupId, (String) null), def);
    }

    public static String FindByRx(List<String> msgLines, Pattern rx, int groupId, String def) {
        Matcher m;

        if (msgLines != null) {
            synchronized (msgLines) {
                for (String s : msgLines) {
                    if (s != null && (m = rx.matcher(s)).find()) {
                        return m.group(groupId);
                    }
                }
            }
        }
        return def;
    }

    public static Matcher FindMatcher(List<String> msgLines, Pattern rx) {
        Matcher m;

        if (msgLines != null) {
            for (String s : msgLines) {
                if (s != null && (m = rx.matcher(s)).find()) {
                    return m;
                }
            }
        }
        return null;
    }

    public static String FindByRx(List<String> msgLines, List<Pair<Pattern, Integer>> ixnIDs) {
        Matcher m;

        if (msgLines != null) {
            for (String s : msgLines) {
                if (s != null && !s.isEmpty()) {
                    for (Pair<Pattern, Integer> ixnID : ixnIDs) {
                        if ((m = ixnID.getKey().matcher(s)).find()) {
                            return m.group(ixnID.getValue());
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String GetSIPHeader(String header, String headerLong, String headerShort) {
        String ret = null;

        if (headerLong != null && headerLong.length() > 0) {
            ret = getVal(header, headerLong.length(), headerLong);
            if (ret != null) {
                return ret;
            }
        }
        if (headerShort != null && headerShort.length() > 0) {
            ret = getVal(header, headerShort.length(), headerShort);
            if (ret != null) {
                return ret;
            }
        }

        return ret;
    }

    /**
     * getVal. Header names should come without colon!!!
     *
     * @param header
     * @param hlLen
     * @param hl
     * @return
     */
    static private String getVal(String header, int hlLen, String hl) {
        Main.logger.trace("\tgetVal h[" + header + "] len[" + hlLen + "] hl[" + hl + "]");

        if (header.length() > hlLen && header.substring(0, hlLen).equalsIgnoreCase(hl)) {
            Matcher m;
            if ((m = SIPHeaderVal.matcher(header.substring(hlLen))).find()) {
                Main.logger.trace("\t\tret[" + m.group(1).trim() + "]");
                return m.group(1).trim();
            }
        }
        Main.logger.trace("\t\tret[NULL]");
        return null;
    }

    public static String transformDN(String u) {
        if (u != null && !u.isEmpty()) {
            if (u.startsWith("msml=")) {
                return "msml:msml";
            }
            if (u.startsWith("record=")) {
                return "msml:record";
            }
            Matcher m = regSIPDN.matcher(u);
            if (m.find()) {
                return m.group();
            }

        }
        return u;
    }

    public static String transformSIPURL(String u) {
        if (u != null && !u.isEmpty()) {
            Matcher m;
            if ((m = regSIPURI.matcher(u)).find()) {
                if (m.group(2) == null) { // no user part in URI
                    return "@" + m.group(1);
                }
                StringBuilder s = new StringBuilder(m.end());
                String DN = m.group(1);
                if (DN.startsWith("msml")) {
                    s.append("msml");
                    Matcher m1 = regSIPURIMediaService.matcher(u);
                    if (m1.find()) {
                        s.append("(").append(m1.group(1)).append(")");
                    }
                } else {
                    s.append(DN);
                }
                s.append("@").append(m.group(2));
                return s.toString();
            }
        }
        return u;
    }

    public static String GetHeaderValue(String s, Character Sep, List<String> m_MessageLines) {
        if (m_MessageLines != null && s != null && !"".equals(s)) {
            s = s.toLowerCase();
            int iSLen = s.length();
            for (String header : m_MessageLines) {
                if (header != null && header.toLowerCase().startsWith(s)
                        && header.length() > iSLen //removing = changes order in reports!!!
                        && ((Sep == null)
                        ? Character.isWhitespace(header.charAt(iSLen))
                        : Sep.equals(header.charAt(iSLen)))) {
                    String ret = header.substring(iSLen + 1).trim();
                    return (ret.isEmpty()) ? null : ret;
                }
            }
        }
        return null;
    }

    static int lookupAttributeIdx(String sTmp, ArrayList<String> lst) {
        String s = '\t' + sTmp;
        if (lst != null && !lst.isEmpty()) {
            for (int i = 0; i < lst.size(); i++) {
                String m_MessageLine = lst.get(i);
                if (m_MessageLine != null && m_MessageLine.startsWith(s)) {
                    return i;
                }

            }
        }
        return -1;
    }

    /**
     * @param uri
     * @return Pair<SessionID, URIRequest>
     */
    public static Pair<String, String> parseORSURI(String uri) {
        String[] split = StringUtils.split(uri, '/');
        int idx = getSessionIdx(split);
        if (idx >= 0) {
            return new Pair<>(split[idx], glueArray(split, idx + 1));
        }
        if ((idx = getKeysIdx(split, new String[]{"genesys", "1"})) >= 0) {
            return new Pair<>(null, glueArray(split, idx + 1));
        }
        if ((getKeysIdx(split, new String[]{"scxml", "session", "start"})) >= 0) {
            return new Pair<>(null, "scxml/session/start");
        }
        Main.logger.info("Not parsed URI: [" + uri + "]");
        return null;
    }

    private static int getKeysIdx(String[] split, String[] keys) {
        int j = 0;
        for (int i = 0; i < split.length; i++) {
            if (split[i].equalsIgnoreCase(keys[j])) {
                j++;
            } else if (j > 0) {
                return -1;
            }
            if (j == keys.length) {
                return i;
            }
        }
        return -1;

    }

    private static String glueArray(String[] split, int idx) {
        StringBuilder ret = new StringBuilder();
        for (int i = idx; i < split.length; i++) {
            if (ret.length() > 0) {
                ret.append('/');
            }
            ret.append(split[i]);

        }
        return ret.toString();
    }

    private static int getSessionIdx(String[] split) {
        int i = getKeysIdx(split, new String[]{"scxml", "session"});
        if (i >= 0 && split.length > i + 1 && Parser.isSessionID(split[i + 1])) {
            return i + 1;
        }
        return -1;
    }

    protected void checkDiffer(String oldS, String newS, String prop, int line) {
        if ((oldS == null || oldS.isEmpty())
                || (oldS != null && oldS.equals(newS))) {
            return;
        }

        if ((oldS != null || oldS.isEmpty()) && (newS == null || newS.isEmpty())) {
            Main.logger.error("l:" + line + " " + getM_type() + " " + prop + "deleting established value old[" + ((oldS == null) ? "null" : oldS) + "] new[" + ((newS == null) ? "null" : newS) + "]");
        } else {
            Main.logger.debug("l:" + line + " " + getM_type() + " " + prop + " old[" + ((oldS == null) ? "null" : oldS) + "] new[" + ((newS == null) ? "null" : newS) + "]");
        }
    }

    public String cfgGetAttrObjType(String cfgAttrName) {
        String ret = cfgGetAttr(cfgAttrName);
        if (ret != null) {
            Matcher m;
            if ((m = ptAttrInBrackets.matcher(ret)).find()) {
                return m.group(1);
            }
        }
        return null;
    }

    public String cfgGetAttr(String cfgAttrName) {
        Pattern attrNameRX = cfgAttrNames.get(cfgAttrName);
        if (attrNameRX == null) {
            attrNameRX = Pattern.compile("\\s+attr:\\s+" + cfgAttrName + "\\s+value:\\s+(.+)$");

        }
        for (String s : m_MessageLines) {
            if (s != null && !s.isEmpty()) {
                Matcher m;
                if ((m = attrNameRX.matcher(s)).find()) {
                    return NoQuotes(m.group(1));
                }
            }
        }
        return null;
    }

    public String FindByRx(Regexs parentIxnID) {
        return FindByRx(parentIxnID.getRegs());
    }

    private String getIxnAttribute(String[] attrs, Pattern valuePattern) {
        for (String str : m_MessageLines) {
            for (String attr : attrs) {
                String ret = strStartRX(str, attr, valuePattern, 1);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    /**
     * strStartRX will check if the line starts with start and then apply rx to
     * the end of the line if it matches, then return groupIdx from match
     *
     * @param line     - String
     * @param start    - String
     * @param rx       - Pattern
     * @param groupIdx - index of the group expression in Pattern
     * @return null if the line does not begin with start; empty string if RX
     * does not match
     */
    private String strStartRX(String line, String start, Pattern rx, int groupIdx) {
        if (line != null && line.startsWith(start)) {
            String s = line.substring(start.length());
            Matcher m;
            if ((m = rx.matcher(s)).find()) {
                return m.group(groupIdx);
            } else {
                return "";
            }
        }
        return null;
    }

    private String getIxnAttribute(String attr, Pattern valuePattern) {
        for (String str : m_MessageLines) {
            String ret = strStartRX(str, attr, valuePattern, 1);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    protected String getIxnAttributeLong(String attr) {
        return getIxnAttribute(attr, regLongAttribute);
    }

    protected String getIxnAttributeHex(String[] attrs) {
        for (String attr : attrs) {
            String ixnAttributeHex = getIxnAttributeHex(attr);
            if (ixnAttributeHex != null && !ixnAttributeHex.isEmpty()) {
                return ixnAttributeHex;
            }
        }
        return null;
    }

    protected String getIxnAttributeHex(String attr) {
        return getIxnAttribute(attr, regHexAttribute);
    }

    protected String getIxnAttributeString(String attr) {
        return getIxnAttribute(attr, regStringAttribute);
    }

    protected String getGMSAttributeString(String[] attrs) {
        String[] bufAttr = new String[attrs.length];
        for (int i = 0; i < attrs.length; i++) {
            bufAttr[i] = adjustGMSString(attrs[i]);
        }
        return getIxnAttribute(bufAttr, regStringAttribute);
    }

    private String adjustGMSString(String attr) {
        return "\t" + '\'' + attr + '\'';
    }

    protected String getGMSAttributeString(String attr) {
        return getIxnAttribute(adjustGMSString(attr), regStringAttribute);
    }

    protected void setMessageLines(ArrayList<String> mls) {
        if (mls != null) {
            if (m_MessageLines == null) {
                this.m_MessageLines = new ArrayList<>();
            }
            for (String m_MessageLine : mls) {
                this.m_MessageLines.add(m_MessageLine);
            }
        }

    }

    protected void addMessageLines(ArrayList<String> lines) {
        m_MessageLines.clear();
        m_MessageLines.addAll(lines);
    }

    @Override
    public String toString() {
        return "msg[" + super.toString() + "inb:" + m_isInbound
                + ((Main.logger.isTraceEnabled()) ? (";lines=<" + m_MessageLines) : "")
                + ">]";
    }

    public Integer getIntOrDef(String n, Integer def) {
        return getIntOrDef(n, def, false);
    }

    public Long getIntOrDef(String n, Long def) {
        return getLongOrDef(n, def, false);
    }

    public Long getLongOrDef(String n, Long def, boolean printError) {
        try {
            return Long.parseLong(n);
        } catch (NumberFormatException e) {
            if (printError) {
                Main.logger.error("error parsing int [" + n + "]", e);
            }
            return def;
        }
    }

    public Integer getIntOrDef(String n, Integer def, boolean printError) {
        try {
            return Integer.parseInt(n);
        } catch (NumberFormatException e) {
            if (printError) {
                Main.logger.error("error parsing int [" + n + "]", e);
            }
            return def;
        }
    }

    public Integer FindByRx(Pattern rx, int groupId, int def) {
        return intOrDef(FindByRx(rx, groupId, null), def);
    }

    public String getRXValue(Pattern rx, int groupId, int idx, String def) {
        if (m_MessageLines != null && m_MessageLines.size() > idx) {
            Matcher m;
            if ((m = rx.matcher(m_MessageLines.get(idx))).find()) {
                return m.group(groupId);
            }
        }
        return def;
    }

    public String FindByRx(Pattern rx, String s, int groupId, String def) {
        Matcher m;

        if (s != null && (m = rx.matcher(s)).find()) {
            return m.group(groupId);
        }
        return def;
    }

    public String FindByRx(List<Pair<Pattern, Integer>> ixnIDs) {
        return FindByRx(m_MessageLines, ixnIDs);
    }

    public String FindByRx(Pattern rx, int groupId, String def) {
        return FindByRx(m_MessageLines, rx, groupId, def);
    }


    public int toInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public Matcher FindRx(Pattern rx) {
        Matcher m;

        if (m_MessageLines != null) {
            for (String s : m_MessageLines) {
                if (s != null && (m = rx.matcher(s)).find()) {
                    return m;
                }
            }
        }
        return null;
    }

    public void SetInbound(boolean isInbound) {
        setM_isInbound(isInbound);
    }

    public long getFileBytes() {
        return getM_FileBytes();
    }

    /**
     * @param s - string with SIP URI
     * @return matcher if SIP URI matches.
     */
    Matcher parseSIPURI(String s) {
        Matcher m;
        if ((m = regSIPURI.matcher(s)).find()) {
            return m;
        } else {
            return null;
        }
    }

    public String GetSIPHeaderURL(String headerLong, String headerShort) {
        return transformSIPURL(GetSIPHeader(headerLong, headerShort));
    }

    public String GetSIPHeader(String headerLong, String headerShort) {
        String ret = null;

        if ((headerLong != null && headerLong.length() > 0
                || headerShort != null && headerShort.length() > 0)
                && m_MessageLines != null) {
            for (Iterator<String> i = m_MessageLines.iterator(); i.hasNext(); ) {
                ret = GetSIPHeader( i.next(), headerLong, headerShort);
                if (ret != null && !ret.isEmpty()) {
                    break;
                }
            }
        }
        return ret;
    }

    public String GetHeaderValue(String s, Character Sep) {
        return GetHeaderValue(s, Sep, m_MessageLines);
    }

    public String GetHeaderValue(String[] lst) {
        for (String s : lst) {
            String ret = GetHeaderValue(s, null);
            if (ret != null && !ret.isEmpty()) {
                return ret;
            }
        }
        return null;
    }

    public String GetHeaderValue(String s) {
        return GetHeaderValue(s, null);
    }

    public String GetHeaderValueNoQotes(String s) {
        return StripQuotes(GetHeaderValue(s));
    }

    public long GetAdjustedUsecTime() {
        DateParsed m_TimestampDP1 = getM_TimestampDP();
        if (m_TimestampDP1 != null) {
            return m_TimestampDP1.getUTCms();
        }
        return 0;
    }

    private String lookupAttribute(String s) {
        if (m_MessageLines != null) {
            synchronized (m_MessageLines) {
                if (s != null && !s.isEmpty() && m_MessageLines != null && !m_MessageLines.isEmpty()) {
                    for (String m_MessageLine : m_MessageLines) {
                        if (m_MessageLine != null && m_MessageLine.startsWith(s) &&
                                (m_MessageLine.length() > s.length() + 1 && (Character.isWhitespace(m_MessageLine.charAt(s.length()))))) {
                                return m_MessageLine.substring(s.length() + 1);


                        }
                    }
                }
            }
        }
        return null;
    }

    public String getAttribute(String[] lst) {
        for (String s : lst) {
            String ret = getAttribute(s);
            if (ret != null && ret.length() > 0) {
                return ret;
            }
        }
        return null;
    }

    public String getAttributeDN(String attr) {
        String attribute = SingleQuotes(getAttribute(attr));
        if (attribute != null && attribute.equals("''")) {
            return null;
        }
        return attribute;
    }

    /* gets TServer attribute; first character is tabulation */
    public String getAttribute(String attr) {

        if (attr.length() > 1) {
            if (attr.charAt(0) == '\t') {
                if (Character.isAlphabetic(attr.charAt(1))) {
                    return lookupAttribute(attr);
                }
            } else if (Character.isAlphabetic(attr.charAt(0))) {
                return lookupAttribute('\t' + attr);
            }
        }
        return null;
    }

    String getUData(String key, String def) {
        String ret;

        if (key.startsWith(udPref)) {
            ret = lookupAttribute(CheckQuotes(key));
        } else {
            ret = lookupAttribute(udPref + CheckQuotes(key));
        }
        if (ret == null || ret.length() == 0) {
            return def;
        }
        return ret;
    }

    /*
    This is for stupid OCS clients that may put integer GSW_RECORD_HANDLE as string
     */
    long getUData(String key, long def, boolean CanHaveQuote) {
        String ret;
        if (key.startsWith(udPref)) {
            ret = lookupAttribute(CheckQuotes(key));
        } else {
            ret = lookupAttribute(udPref + CheckQuotes(key));
        }
        if (ret == null || ret.length() == 0) {
            return def;
        }
        if (CanHaveQuote) {
            ret = StripQuotes(ret);
        }
        try {
            return Long.parseLong(ret);
        } catch (NumberFormatException numberFormatException) {
            Main.logger.debug("error parsing int", numberFormatException);
            return def;
        }
    }

    public Long getAttributeLong(String attr) {
        String s = getAttribute(attr);
        if (s == null) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public long getMMAttributeLong(String attr) {
        Matcher m;
        String s = getAttribute(attr);
        if (StringUtils.isNotBlank(s) && ((m = regLongAttribute.matcher(s)).find())) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }

        }
        return -1;
    }

    public String getMMAttributeString(String attr) {
        Matcher m;
        String s = getAttribute(attr);
        if (StringUtils.isNotBlank(s) && ((m = regStringAttribute.matcher(s)).find())) {
                return m.group(1);

        }
        return "";
    }

    private ArrayList<String> getListKey(String key) {
        ArrayList<String> ret = new ArrayList<>();
        String s = checkTab(key);
        boolean keyFound = false;

        if (StringUtils.isNotBlank(s) && m_MessageLines != null) {
            for (String m_MessageLine : m_MessageLines) {
                if (keyFound) {

                    if (regAttributeInList.matcher(m_MessageLine).find()) {
                        ret.add(m_MessageLine);
                    } else {
                        break;
                    }
                } else {
                    if (m_MessageLine.startsWith(s)
                            && (m_MessageLine.length() > s.length() + 1)
                            && (Character.isWhitespace(m_MessageLine.charAt(s.length())))) {
                                keyFound = true;


                    }
                }
            }
        }
        return (ret.isEmpty())?null:ret;
    }

    private ArrayList<String> getMMUserData() {
        if (m_MMUserData == null) {
            m_MMUserData = getListKey("attr_user_data");
        }
        return m_MMUserData;
    }

    private ArrayList<String> getMMEnvelope() {
        if (m_MMEnvelope == null) {
            m_MMEnvelope = getListKey("attr_envelope");
        }
        return m_MMEnvelope;
    }

    private String getListValue(ArrayList<String> list, String s, String def) {
        Matcher m;
        if (StringUtils.isNotBlank(s) && list != null && !list.isEmpty()) {
            for (String m_MessageLine : list) {
                if ((m = regListKeyStart.matcher(m_MessageLine)).find()) {
                    int idx = m_MessageLine.indexOf(s, m.end(0));
                    if (idx > 0) {
                        String ret = m_MessageLine.substring(idx + s.length());
                        if ((m = regListKeyEnd.matcher(ret)).find()) {
                            ret = ret.substring(m.end(0));
                            if (!ret.isEmpty() && ((m = regStringAttribute.matcher(ret)).find())) {
                                    return m.group(1);

                            }
                        }
                    }
                }
            }
        }
        return def;

    }

    public String getMMUserDataString(String key) {
        return getListValue(getMMUserData(), key, "");
    }

    public String getMMAttrEnvelopeString(String key) {
        return getListValue(getMMEnvelope(), key, "");
    }

    public int getAttributeInt(String attr) {
        String s;
        s = getAttribute(attr);

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Long getAttributeHex(String attr) {
        String s = getAttribute(attr);

        if (StringUtils.isNotBlank(s)) {
            try {

                return Long.parseLong(s, 16);
            } catch (NumberFormatException e) {
                Main.logger.debug("Error parsing long  for [" + attr + "]: " + e.getMessage());

            }
        }
        return null;
    }

    public int getHeaderHex(String attr) {
        String s;
        s = GetHeaderValue(attr);
        Main.logger.info("getHeaderHex [" + attr + "]: " + s);

        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long getHeaderLong(String attr) {
        String s;
        s = GetHeaderValue(attr);

        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getAttributeTrim(String attr) {
        String ret = getAttribute(attr);
        if (StringUtils.isNotBlank(ret)) {
            return ret.trim();
        } else {
            return null;
        }
    }

    public String getAttributeTrim(String attr, int minLengh) {
        String ret = getAttributeTrim(attr);
        return (ret != null && ret.length() >= minLengh) ? ret : "";
    }

    public String getHeaderTrim(String attr) {
        String ret = GetHeaderValue(attr);
        if (StringUtils.isNotBlank(ret)) {
            return ret.trim();
        } else {
            return null;
        }
    }

    public String getHeaderTrimQuotes(String attr, int minLengh) {
        String ret = getHeaderTrimQuotes(attr);
        return (ret != null && ret.length() >= minLengh) ? ret : "";
    }

    public String getHeaderTrimQuotes(String attr) {
        String ret = GetHeaderValue(attr);
        if (ret != null) {
            return StripQuotes(ret.trim());
        } else {
            return "";
        }
    }

    public String getAttributeTrimQuotes(String attr) {
        String ret = getAttribute(attr);
        if (StringUtils.isNotBlank(ret)) {
            return StripQuotes(ret.trim());
        } else {
            return null;
        }
    }

    @Override
    void PrintMsg() {
        super.PrintMsg();
        if (m_MessageLines == null || m_MessageLines.isEmpty()) {
            Main.logger.error("Empty contents");
        } else {
            Main.logger.info("::");
            for (Object content : m_MessageLines) {
                Main.logger.info("\t[" + content + "]");
            }
        }
    }

    /**
     * @return the m_FileBytes
     */
    @Override
    public long getM_FileBytes() {
        return m_FileBytes;
    }

    /**
     * @return the m_isInbound
     */
    public boolean isInbound() {
        return m_isInbound;
    }

    /**
     * @param m_isInbound the m_isInbound to set
     */
    public void setM_isInbound(boolean m_isInbound) {
        this.m_isInbound = m_isInbound;
    }


    private String checkTab(String key) {
        if (key != null && key.length() > 0) {
            if (key.charAt(0) == '\t') {
                if (key.length() > 1 && Character.isAlphabetic(key.charAt(1))) {
                    return key;
                }
            } else if (Character.isAlphabetic(key.charAt(0))) {
                return '\t' + key;
            }
        }
        return null;
    }

    String getErrorMessage(String TEventName) {
        if (TEventName != null && TEventName.equals("EventError")) {
            String attribute = getAttributeTrimQuotes("AttributeErrorMessage");
            if (StringUtils.isBlank(attribute)) {
                attribute = StripQuotes(FindByRx(regErrorMessage, 1, ""));
            }
            String uData = StripQuotes(getUData("FaultString", null));
            if (StringUtils.isNotBlank(uData)) {
                if (StringUtils.isNotBlank(attribute)) {
                    return StringUtils.join(new String[]{attribute, uData}, "|");
                } else {
                    return uData;
                }
            } else {
                return attribute;
            }
        } else {
            return null;
        }
    }

    public static class Regexs {

        private final ArrayList<Pair<Pattern, Integer>> regs;

        public Regexs(Pair<String, Integer> ... string) {
            regs = new ArrayList<>(string.length);
            for (Pair<String, Integer> string1 : string) {
                regs.add(new Pair<>(Pattern.compile(string1.getKey()), string1.getValue()));
            }
        }

        public List<Pair<Pattern, Integer>> getRegs() {
            return regs;
        }
    }

    public abstract static class RegExAttribute1 extends Attribute {

        abstract String getValue(Regexs rx);

        @Override
        String getValue() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    public abstract static class Attribute {

        private boolean valueParsed = false;
        private String value = null;

        abstract String getValue();

        @Override
        public String toString() {
            if (!valueParsed) {
                value = getValue();
                valueParsed = true;
            }
            return value;
        }
    }

    public class RegExAttribute {

        private final Regexs re;
        String attrValue = null;
        private boolean matched;

        public RegExAttribute(Regexs re) {
            this.re = re;
            matched = false;
        }

        private boolean matched() {
            return this.matched;
        }

        private void tryMatch(String s) {
            for (Pair<Pattern, Integer> ixnID : re.getRegs()) {
                Matcher m;
                if ((m = ixnID.getKey().matcher(s)).find()) {
                    attrValue = m.group(ixnID.getValue());
                    matched = true;
                }
            }
        }

        @Override
        public String toString() {
            return attrValue;
        }

    }

    public class MessageAttributes extends ArrayList<RegExAttribute> {

        private static final long serialVersionUID = 1L;

        public void parseAttributes() {
            if (m_MessageLines != null) {
                for (String s : m_MessageLines) {
                    if (s != null && !s.isEmpty()) {
                        for (RegExAttribute rea : this) {
                            if (!rea.matched()) {
                                rea.tryMatch(s);
                            }
                        }
                    }
                }
            }

        }
    }
}
