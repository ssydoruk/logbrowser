package com.myutils.logbrowser.inquirer;

import Utils.*;
import com.myutils.logbrowser.common.*;
import com.myutils.logbrowser.inquirer.gui.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.*;
import org.w3c.dom.*;

public abstract class OutputSpecFormatter extends DefaultFormatter {

    final static String FILELINK = "filelink";
    final static String TIMESTAMP = "timestamp";
    final static String SIPNAME = "sipname";
    final static String TLIBNAME = "tlibname";
    final static String DIRARROW = "dirarrow";
    final static String DIRVERB = "dirverb";
    final static String CALLIDALIAS = "callidalias";
    final static String MSGSPECIFIC = "msgspecific";

    final static String ID_NULL = "id_none";

    final static String AttributeId = "id";
    final static String AttributeFormat = "format";

    final static int MAX_NAME_LENGTH = 20;
    final static int DEFAULT_FILE_LINK_LENGTH = 40;

    static private ArrayList<Element> getElementsChildByName(Element e, String name) {
        ArrayList<Element> ret = new ArrayList<>();
        NodeList nl = e.getChildNodes();
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) nl.item(i);
                    if (el.getNodeName().equalsIgnoreCase(name)) {
                        ret.add(el);
                    }
                }
            }
        }

        return ret;
    }

    static private int getCallID(String callId) {
        if (m_callIdHash.containsKey(callId)) {
            return (m_callIdHash.get(callId));
        } else {
            m_callIdHash.put(callId, m_callIdCount++);
            int ret = m_callIdCount - 1;
            return ret;
        }
    }

    private final XmlCfg cfg;

    private final HashMap<String, RecordLayout> outSpec = new HashMap<>();

    public OutputSpecFormatter(XmlCfg cfg,
            boolean isLongFileNameEnabled,
            HashSet<String> components) throws Exception {
        super(isLongFileNameEnabled, components);
        inquirer.logger.debug("OutputSpecFormatter " + cfg.getXmlFile());
        this.cfg = cfg;
        doRefreshLayout();

    }

    protected RecordLayout getLayout(MsgType GetType) {
        return outSpec.get(GetType.toString());
    }

    private Element getElementChildByName(Element e, String name) {
        NodeList nl = e.getChildNodes();
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) nl.item(i);
                    if (el.getNodeName().equalsIgnoreCase(name)) {
                        return el;
                    }
                }
            }
        }

        return null;
    }

    public String SubstituteEmbeddedFormats(String fromXml) {
        if (fromXml != null) {
            String result = "";
            if (inquirer.getCr().isPrintLogFileName()) {
                result = fromXml.replace(FILELINK, "%-" + (m_maxFileNameLength + 10) + "s");
            } else {
                result = fromXml;
            }
            result = result.replace(CALLIDALIAS, "%-6s");
            result = result.replace(DIRARROW, "%-2s");
            result = result.replace(DIRVERB, "%-3s");
            result = result.replace(SIPNAME, "%20s");
            result = result.replace(TLIBNAME, "%20s");
            result = result.replace(TIMESTAMP, "%s");
            result = result.replace(MSGSPECIFIC, "%s");
            return result;
        } else {
            return "";
        }
    }

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    @Override
    public void ProcessLayout() {
        for (RecordLayout lo : outSpec.values()) {
            lo.UpdateFormatString();
        }
    }

    private void doRefreshLayout() throws Exception {
        for (org.w3c.dom.Element el : cfg.getLayouts()) {
            String msgType = el.getAttribute("MsgType").toLowerCase();
            outSpec.put(msgType, new RecordLayout(el, msgType, cfg.getXmlFile()));
        }

    }

    @Override
    public void refreshFormatter() throws Exception {
        if (cfg.loadFile()) {
            outSpec.clear();
            doRefreshLayout();
        }
    }

    public static abstract class Parameter {

        private String m_ShortFormat;
        private final String m_Title;
        private boolean hidden;
        private final HashSet<RegexParam> m_match = new HashSet<>();
        private final HashSet<RegexParam> m_filter = new HashSet<>();
        private String m_format;
        private boolean isStatus;
        private String prevValue = "";
        private String cond; // condition assigned to parameter. If defined
        // condition is evaluated. If false, value is not evaluated

        Parameter(String title) {
            this.m_Title = title;
        }

        @Override
        public String toString() {
            return "Parameter{" + "m_Title=" + m_Title + ", hidden=" + hidden + '}';
        }

        public boolean isHidden() {
            return hidden;
        }

        public String getTitle() {
            return m_Title;
        }

        private boolean CheckFilter(String str) {
//
            for (RegexParam f : m_filter) {
                if (f.find(str)) {
                    return true;
                }
            }
            return false;
        }

        /* returns true if record should be ignored because status not changed*/
        private boolean CheckStatus(String str) {
            if (isStatus) {
                inquirer.logger.trace("new [" + str + "] old [" + prevValue + "]");
                if (prevValue.equals(str)) {
                    return true;
                } else {
                    prevValue = str;
                }
            }
            return false;
        }

        private String getMatchGroup(String str, ILogRecord record) throws SQLException {
            String ret = "";
            for (RegexParam f : m_match) {
                String[] gr = f.getFormatGroups(str, record);
                if (gr == null) // filtered out
                {
                    return null;
                }
                for (int j = 0; j < gr.length; j++) {
                    ret += gr[j];
                }
            }
            return ret;
        }

        private String getFormatGroup(String str, ILogRecord record) throws Exception {
            String ret = "";
            if (m_match != null && m_match.size() > 0) {
                ArrayList<String> arr = new ArrayList<>();
                for (RegexParam f : m_match) {
                    String[] gr = f.getFormatGroups(str, record);
                    if (gr == null || (gr.length == 1 && (gr[0] == null || gr[0].length() == 0))) {
                        return ret; //logic is this: if any pattern does not match - we print nothing
                    }
                    arr.addAll(Arrays.asList(gr));
                }
                try {
                    return String.format(m_format, arr.toArray());
                } catch (MissingFormatArgumentException e) {
                    throw new MissingFormatArgumentException("format: ["
                            + m_format + "] args: " + arr.size());
                }
            } else {
                ret = String.format(m_format, str);
            }
            return ret;
        }

        public void setHidden(boolean attribute) {
            this.hidden = attribute;
        }

        abstract public String getType();

        protected String GetFileBytes(ILogRecord record) throws Exception {
            if (inquirer.getCr().isAccessLogFiles()) {
                try {
                    return InquirerFileIo.GetFileBytes(record);
                } catch (java.io.FileNotFoundException e) {
                    inquirer.logger.info("Cannot get file bytes for " + record, e);
                    return "";
                }
            } else {
                return "";
            }
        }

        public boolean hasFormat() {
            return m_format != null && m_format.length() > 0;
        }

        public void SetFormat(String format) {
            m_format = format;
        }

        final protected void ParseParam(Element e) {
            ParseParam(e, false);
        }

        protected void ParseParam(Element e, boolean ignorePatternForDBFields) {

            if (e != null) {
                try {
                    isStatus = Boolean.parseBoolean(e.getAttribute("status"));
                    m_format = e.getAttribute("format");
                    m_ShortFormat = e.getAttribute("shortFormat");
                    String _cond = e.getAttribute("cond");
                    if (StringUtils.isNotBlank(_cond)) {
                        cond = _cond;
                    }
                } catch (Exception ex) {
                    logger.error("Cannot parse parameter", ex);
                }
                if (!ignorePatternForDBFields) {
                    ArrayList<Element> els = getElementsChildByName(e, "pattern");
                    if (els != null && els.size() > 0) {
                        for (Iterator<Element> iterator = els.iterator(); iterator.hasNext();) {
                            Element next = iterator.next();
                            SetMatch(next);
                        }
                    }
                }
                for (Iterator<Element> el = getElementsChildByName(e, "filter").iterator();
                        el != null && el.hasNext();) {
                    SetFilter(el.next());

                }
            }
        }

        public void SetMatch(Element patternElement) {
            if (patternElement != null) {
                m_match.add(new RegexParam(patternElement));
            }
        }

        public void SetFilter(Element patternElement) {
            if (patternElement != null) {
                m_filter.add(new RegexParam(patternElement));
            }
        }

        public String GetValueFilter(String str, ILogRecord record) throws SQLException {
            if (str != null && !str.isEmpty()) {
                if (CheckFilter(str)) {
                    return null;
                }
                if (m_match.size() > 0) {
                    String ret = getMatchGroup(str, record);
                    if (ret != null && CheckStatus(ret)) {
                        ret = null;
                    }
                    return ret;
                }
            }
            return str;
        }

        public String GetFormatFilter(String str, ILogRecord record) throws Exception {
            if (str != null && !str.isEmpty()) {
                String ret;
                if (CheckFilter(str)) {
                    /*
                    to implement - flag to ignore field or record
                    
                    if return == null - ignore record
                    if str ="" - ignore field if filter triggered
                     */
//                    return null;
                    str = "";
                }

                /**
                 * strange block, not sure what it means -->*
                 */
                if (m_match.size() > 0) {
                    ret = getFormatGroup(str, record);
                } else {
                    ret = String.format(m_format, str);
                }
                /*<---*/

//                ret = String.format(m_format, str);
                if (CheckStatus(ret)) {
                    ret = null;
                }
                return ret;
            }
            return "";
        }

        public String printValue(ILogRecord record) throws Exception {

            if (hasFormat()) {
                return FormatValue(record);
            } else {
                return GetValue(record);
            }
        }

        abstract public String GetValue(ILogRecord record) throws Exception;

        abstract public String FormatValue(ILogRecord record) throws Exception;

        private String evalValue(ILogRecord record) throws Exception {
            if (cond != null) {
                try {
                    if (JSRunner.execBoolean(cond, record) == false) {
                        return "";
                    }
                } catch (Exception e) {
                    logger.error("", e);
                } finally {
                    Inquirer.setCurrentRec(null);
                }

            }
            return (hasFormat()) ? FormatValue(record) : GetValue(record);

        }

        class RegexParam {

            private final int m_group;
            private final Pattern m_matchPattern;
            private ArrayList<Integer> m_groups = null;
            private String retAttribute = null;
            String expr = null;
            CallableStatement st;
            private final int iRetGroup;
            private final Pattern mFileMatch;

            public RegexParam(Element patternElement) {
                //            Element patternElement = getElementChildByName(e, "pattern");
                String m_match1 = patternElement.getAttribute("match");
                String groupAttr = patternElement.getAttribute("group");
                String groupsAttr = patternElement.getAttribute("groups");
                String ignoreCaseAttr = patternElement.getAttribute("ignorecase");
                String fileMatch = patternElement.getAttribute("filematch");
                String retGroup = patternElement.getAttribute("retgroup");

                expr = patternElement.getAttribute("expression");
                if (expr == null || expr.isEmpty()) {
                    expr = null;
//                    st = DatabaseConnector.prepareStatement(expr);
                } else {
                    int a = 1;
                }
                retAttribute = patternElement.getAttribute("return");
                inquirer.logger.trace("return attribute specified [" + retAttribute + "]");

                if (groupAttr != null && !groupAttr.isEmpty()) {
                    m_group = Integer.parseInt(groupAttr);
                } else {
                    m_group = 0;
                }
                if (retGroup != null && !retGroup.isEmpty()) {
                    iRetGroup = Integer.parseInt(retGroup);
                } else {
                    iRetGroup = 0;
                }

                if (groupsAttr != null && !groupsAttr.isEmpty()) {
                    String[] grps = groupsAttr.split("\\s*,\\s*");
                    if (grps.length > 0) {
                        m_groups = new ArrayList<>(grps.length);
                        for (String grp : grps) {
                            m_groups.add(Integer.parseInt(grp));
                        }
                    }
                }
                boolean m_ignoreCase = true;
                if (ignoreCaseAttr != null && !ignoreCaseAttr.isEmpty()) {
                    m_ignoreCase = Boolean.parseBoolean(ignoreCaseAttr);
                }

                int flags = 0;
                if (m_ignoreCase) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                flags |= Pattern.MULTILINE;

                m_matchPattern = Pattern.compile(m_match1, flags);

                if (fileMatch != null && !fileMatch.isEmpty()) {
                    mFileMatch = Pattern.compile(fileMatch, flags);
                } else {
                    mFileMatch = null;
                }
            }

            private String evalExpr(String s) throws SQLException {
                if (expr != null) {
//                    try {
                    return DatabaseConnector.getValue(expr, s);
//                    } catch (Exception ex) {
//                        logger.error("fatal: ",  ex);
//                    }
                }
                return s;
            }

//            String getMatchGroup(String value) throws Exception {
//                if (m_matchPattern != null) {
//                    Matcher matcher = m_matchPattern.matcher(value);
//                    if (matcher.find()) {
//                        if (retAttribute != null && retAttribute.length() > 0) {
//                            inquirer.logger.trace("returning [" + retAttribute + "]");
//                            return evalExpr(retAttribute);
//                        } else if (mFileMatch != null) {
//                            return evalFileMatch(record);
//                        } else {
//                            if (m_group == 0) {
//                                return evalExpr(matcher.group());
//
//                            } else {
//                                return evalExpr(matcher.group(m_group));
//                            }
//                        }
//                    } else {
//                        return "";
//                    }
//                }
//                return value;
//            }
//            String getFormatGroup(String value) {
//                if (m_matchPattern != null) {
//                    Matcher matcher = m_matchPattern.matcher(value);
//                    if (matcher.find()) {
//                        if (m_groups == null) {
//                            return String.format(m_format, matcher.group());
//                        } else {
//                            ArrayList <String> parm = new ArrayList<String>(m_groups.size());
//                            for (Integer grpID : m_groups) {
//                                parm.add(matcher.group(grpID));
//                            }
//                            return String.format(m_format, parm.toArray());
//                        }
//                    }
//                    else {
//                        return "";
//                    }
//                }
//                return value;
//            }
            String[] getFormatGroups(String value, ILogRecord record) throws SQLException {
                if (m_matchPattern != null) {
                    Matcher matcher = m_matchPattern.matcher(value);
                    if (matcher.find()) {
                        if (retAttribute != null && retAttribute.length() > 0) {
                            inquirer.logger.trace("returning [" + retAttribute + "]");
                            return new String[]{evalExpr(retAttribute)};
                        } else if (mFileMatch != null) {
                            return evalFileMatches(record);
                        } else {

                            if (m_groups == null) {
                                if (m_group > 0) {
                                    return new String[]{evalExpr(matcher.group(m_group))};
                                } else {
                                    return new String[]{evalExpr(matcher.group())};
                                }
                            } else {
                                String[] parm = new String[m_groups.size()];
                                int i = 0;
                                for (Integer grpID : m_groups) {
                                    parm[i++] = evalExpr(matcher.group(grpID));
                                }
                                return parm;
                            }
                        }
                    } else {
                        return new String[]{""};
                    }
                }
                return new String[]{value};
            }

            boolean find(String value) {
                return m_matchPattern.matcher(value).find();
            }

            private String evalFileMatch(ILogRecord record) {
                try {
                    String sRecord = GetFileBytes(record);
                    if (sRecord != null && !sRecord.isEmpty()) {
                        String[] split = sRecord.split("$");
                        StringBuilder ret = new StringBuilder();
                        for (String s : split) {
                            Matcher m;
                            if ((m = mFileMatch.matcher(s)).find()) {
                                if (ret.length() > 0) {
                                    ret.append(" | ");
                                }
                                ret.append(m.group(iRetGroup));
                            }
                        }
                        return ret.toString();
                    }
                } catch (Exception ex) {
                    logger.error("", ex);
                }
                return "";

            }

            private String[] evalFileMatches(ILogRecord record) {
                String[] ret = new String[1];
                ret[0] = evalFileMatch(record);
                return ret;
            }

        }
    }

    static class EmbeddedParameter extends Parameter {

        String m_id;
        private final boolean fileLink;

        private EmbeddedParameter(Element element, String defaultFieldName) throws Exception {
            super(defaultFieldName);
            ParseParam(element);
            m_id = element.getAttribute(AttributeId).toLowerCase();
            this.fileLink = m_id.equals(FILELINK);
        }

        public boolean isFileLink() {
            return fileLink;
        }

        @Override
        public String GetValue(ILogRecord record) throws Exception {
            String value = null;

            if (isFileLink()) {
                if (inquirer.getCr().isPrintLogFileName()) {
                    String file = record.GetFileName().toString();
//                    if (!m_isLongFileNameEnabled) {
//                        file = GetRelativePath(file);
//                    }
                    int line = record.GetLine();
                    value = file + "(" + line + "):";
                    if (record.IsMarked()) {
                        value += " *";
                    }
                } else {
                    value = "";
                }

            } else if (m_id.equals(TIMESTAMP)) {
                value = record.GetTime();

            } else if (m_id.equals(SIPNAME)) {
                String name = record.GetField("name");
//                if (m_isLongFileNameEnabled) {
                name = (record.IsInbound() ? "<-" + name : "->" + name);
                if (name.length() > MAX_NAME_LENGTH) {
                    name = name.substring(0, MAX_NAME_LENGTH);
                }

                value = name;
//                } else {
//                    value = "";
//                }
            } else if (m_id.equals(TLIBNAME)) {
                String name = GetValueFilter(record.GetField("name"), record);
                if (name != null) {
//                    if (name.length() > MAX_NAME_LENGTH) {
//                        String newName = name.substring(0, MAX_NAME_LENGTH - m_tlibTail);
//                        if (m_tlibTail != 0) {
//                            newName = newName.substring(0, newName.length() - 1)
//                                    + "~" + name.substring(name.length() - m_tlibTail);
//                        }
//                        name = newName;
//                    }
                    value = name;
                } else {
                    value = "";
                }
            } else if (m_id.equals(DIRARROW)) {
                value = (record.IsInbound() ? "<-" : "->");

            } else if (m_id.equals(DIRVERB)) {
                value = (record.IsInbound() ? "in " : "out");

            } else if (m_id.equals(CALLIDALIAS)) {
                if (record instanceof SipMessage) {
                    return String.format("Cid%03d", getCallID(((SipMessage) record).getCallId()));
                } else {
                    return "";
                }

            } else if (m_id.equals(MSGSPECIFIC)) {
                String name = record.GetField("name");
                if (name.equals("RequestPrivateService")
                        || name.equals("RequestNetworkPrivateService")
                        || name.equals("EventPrivateInfo")
                        || name.equals("EventNetworkPrivateInfo")) {
                    String msgid = record.GetField("msgid");
                    value = "msgid=" + msgid;
                } else if (name.equals("RequestRouteCall")
                        || name.equals("RequestMakeCall")
                        || name.equals("RequestInitiateTransfer")
                        || name.equals("RequestSingleStepTransfer")
                        || name.equals("RequestMuteTransfer")
                        || name.equals("RequestInitiateConference")
                        || name.equals("RequestSingleStepConference")
                        || name.equals("RequestGetAccessNumber")
                        || name.equals("RequestQueryLocation")) {
                    String location = record.GetField("location");
                    value = "loc=" + location;
                } else {
                    value = "";
                }
            } else {
                value = m_id;
            }

            return value;
        }

        @Override
        public String FormatValue(ILogRecord record) throws Exception {
            return GetFormatFilter(GetValue(record), record);
        }

        @Override
        public String getType() {
            return "abstract";
        }

    }

    static class DatabaseParameter extends Parameter {

        String m_id;

        DatabaseParameter(Element e, String defaultFieldName) throws SQLException {
            super(defaultFieldName);
            m_id = e.getAttribute(AttributeId).toLowerCase();
            ParseParam(e);
        }

        DatabaseParameter(String id, String alias) throws SQLException {
            super(alias);
            m_id = id;
        }

        @Override
        public String GetValue(ILogRecord record) throws SQLException {
            String value = record.GetField(m_id);

            return GetValueFilter(value, record);
        }

        @Override
        public String FormatValue(ILogRecord record) throws Exception {
            return GetFormatFilter(GetValue(record), record);
        }

        @Override
        public String getType() {
            return "database";
        }

    }

    static class ScriptParameter extends Parameter {

        String m_id;
        private String script;

        ScriptParameter(Element e, String defaultFieldName) throws SQLException {
            super(defaultFieldName);
            script = e.getTextContent();

            ParseParam(e);
        }

        ScriptParameter(String id, String alias) throws SQLException {
            super(alias);
            m_id = id;
        }

        @Override
        public String GetValue(ILogRecord rec) throws SQLException {
            return JSRunner.execString(script, rec);

        }

        @Override
        public String FormatValue(ILogRecord record) throws Exception {
            return GetValue(record);
        }

        @Override
        public String getType() {
            return "script";
        }

    }

    static class FileParameter extends Parameter {

        private FileParameter(Element e, String defaultFieldName) throws Exception {
            super(defaultFieldName);
            ParseParam(e);
        }

        @Override
        public String GetValue(ILogRecord record) throws Exception {

            return GetValueFilter(GetFileBytes(record), record);
        }

        @Override
        public String FormatValue(ILogRecord record) throws Exception {
            return GetFormatFilter(GetFileBytes(record), record);
        }

        @Override
        public String getType() {
            return "file";
        }
    }

    public class IgnoreRecordException extends Exception {
    }

    enum ParamType {

        embedded("embedded"),
        database("database"),
        file("file"),
        script("script");

        private final String name;

        ParamType(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equals(otherName);
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    class RecordLayout {

        ArrayList<Parameter> parameters;
        String formatString;
        String formatStringFromXml;
        private String initScript = null;
        private final HashMap<String, Object> scriptFields = new HashMap<>();
        private final String cfgFile;

        private RecordLayout(org.w3c.dom.Element el, String msgType, String cfgFile) throws Exception {
            // get format attribute, save format string
            this.cfgFile = cfgFile;
            formatStringFromXml = el.getAttribute("format");

//            if (formatAttr != null) {
//                formatStringFromXml = formatAttr.getValue();
//            } else {
//                formatStringFromXml = "";
//            }
// replace embedded format aliases like FILELINK
// with values meaningful for String.format
//            formatString = SubstituteEmbeddedFormats(formatStringFromXml);
// iterate through child elements, save parameters
            parameters = new ArrayList();

//            Iterator itr = (el.getChildren()).iterator();
//            while (itr.hasNext()) {
            NodeList nl = el.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element paramElement = (Element) nl.item(i);
                    if (paramElement.getTagName().equalsIgnoreCase("initScript")) {
                        addInitScript(paramElement);
                    } else {
                        Parameter parameter = null;

                        ParamType paramType = ParamType.valueOf(paramElement.getAttribute("type"));
                        String fieldName = getAttribute(paramElement, "name", TabResultDataModel.TableRow.colPrefix + i);

                        switch (paramType) {
                            case database:
                                parameter = new DatabaseParameter(paramElement, fieldName);
                                break;

                            case file:
                                parameter = new FileParameter(paramElement, fieldName);
                                break;

                            case embedded:
                                parameter = new EmbeddedParameter(paramElement, fieldName);
                                break;
                            case script:
                                parameter = new ScriptParameter(paramElement, fieldName);
                                break;

                        }
                        parameter.setHidden(getAttribute(paramElement, "hidden", false));

                        parameters.add(parameter);
                    }
                }
            }
        }

        private boolean getAttribute(Element e, String key, boolean defaultValue) {
            if (e.hasAttribute(key)) {
                String val = e.getAttribute(key);
                if (val != null) {
                    if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes")) {
                        return true;
                    } else if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("no")) {
                        return false;
                    } else {
                        inquirer.logger.error("Unsupported value for boolean key [" + key + "], element: [" + e + "]");
                    }
                }
            }
            return defaultValue;

        }

        private String getAttribute(Element e, String key, String defaultValue) {
            String attribute = null;
            if (e.hasAttribute(key)) {
                attribute = e.getAttribute(key);
            }
            if (attribute != null && attribute.length() > 0) {
                return attribute;
            }
            return defaultValue;
        }

        public void UpdateFormatString() {
            formatString = SubstituteEmbeddedFormats(formatStringFromXml);
        }

        public String PrintRecord(ILogRecord record, PrintStreams ps, IQueryResults qr) throws Exception {

            if (initScript != null) {
                scriptFields.clear();
                if (JSRunner.evalFields(initScript, record, scriptFields))// ignore record 
                {
                    ps.ignoreRecord();
                    return "";
                }
                if (!scriptFields.isEmpty()) {
                    for (Map.Entry<String, Object> entry : scriptFields.entrySet()) {
                        ps.addField(entry.getKey(), entry.getValue());
                    }
                }
            }
            for (Parameter param : parameters) {

                String s = param.evalValue(record);

                if (s == null) { //ignore record
                    return "";
                } else {
                    ps.addField(param, s);
                }
            }
            ArrayList<Parameter> addOutParams = qr.getAddOutParams(record.GetType());
            if (addOutParams != null) {
                for (Parameter param : addOutParams) {
                    String s = param.printValue(record);
                    if (s == null) { //ignore record
                        return "";
                    } else {
//                            paramValues.add(s);
                    }
                    ps.addField(param, s);
                }
            }
//                ps.println(outString);
//                return outString.toString();
            return null;
        }

        String PrintRecordFile(ILogRecord record, PrintStreams ps, IQueryResults qr) {
            StringBuilder outString = new StringBuilder(512);
//                ArrayList<String> paramValues = new ArrayList<>(parameters.size());
            try {
                if (isShouldPrintRecordType()) {
                    outString.append(excelQuote()).append(record.GetType().toString()).append(excelQuote());
                }
                for (Parameter param : parameters) {
                    if (!isShouldAccessFiles() && param instanceof FileParameter) {
                        continue;
                    }
                    if (!isPrintFileLine() && (param instanceof EmbeddedParameter && ((EmbeddedParameter) param).isFileLink())) {
                        continue;
                    }
                    addDelimiter(outString);
                    if (!param.hasFormat()) {
                        String s = param.GetValue(record);
                        if (s == null) { //ignore record
                            return "";
                        } else {
//                            paramValues.add(s);
                        }
                        outString.append(excelQuote()).append(s).append(excelQuote()); // param is field output parameters (hidden, etc). ignoring for now
//                        ps.addField(param, s);
                    } else {
                        String s1 = param.FormatValue(record);
                        if (s1 == null) {
                            return "";
                        }
//                        ps.addField(param, s1);
                        outString.append(excelQuote()).append(s1).append(excelQuote()); // param is field output parameters (hidden, etc). ignoring for now
                    }
                }
                ArrayList<Parameter> addOutParams = qr.getAddOutParams(record.GetType());
                if (addOutParams != null) {
                    for (Parameter param : addOutParams) {
                        addDelimiter(outString);
                        String s = param.printValue(record);
                        if (s == null) { //ignore record
                            return "";
                        } else {
//                            paramValues.add(s);
                        }
//                        ps.addField(param, s);
                        outString.append(excelQuote()).append(s).append(excelQuote()); // param is field output parameters (hidden, etc). ignoring for now
                    }
                }
                ps.println(outString);
//                return outString.toString();
                return null;
            } catch (Exception e) {
                inquirer.logger.error("error printing record type " + record.GetType().toString(), e);
            }
            return "";
        }

        private void addInitScript(Element paramElement) throws Exception {
            String src = paramElement.getAttribute("src");
            if (StringUtils.isNotBlank(src)) {

                List<String> readAllLines;

                File cf = new File(src);
                if (cf.isAbsolute()) {
                    readAllLines = Files.readAllLines(cf.toPath());
                } else {
                    Path cc = (new File(cfgFile)).toPath();
                    readAllLines = readFile((cc.getParent() == null) ? "" : cc.getParent().toString(), src);
                    if (readAllLines == null && Files.isSymbolicLink(cc)) {
                        Path symParent = Files.readSymbolicLink(cc).getParent();
                        readAllLines = readFile((symParent == null) ? "" : symParent.toString(), src);
                    }
                }
                if (readAllLines == null) {
                    throw new Exception("Not able to read JS source file " + src);
                } else {
                    this.initScript = (StringUtils.join(readAllLines, "\n"));
                }

            } else {
                String s = paramElement.getTextContent();
                if (StringUtils.isNotBlank(s)) {
                    s = StringUtils.trimToNull(s);
                }
                if (s != null) {
                    this.initScript = s;
                } else {
                    throw new Exception("initScript specified but body is empty");
                }
            }
        }

        List<String> readFile(String dir, String name) {
            Path get = Paths.get(dir, name);
            logger.debug("Reading file " + get.toString());
            if (Files.isReadable(get)) {
                try {
                    List<String> ret = Files.readAllLines(get);
                    setJSFile(get);
                    return ret;

                } catch (IOException ex) {
                    logger.error("Error reading file: ", ex);
                }
            }
            return null;
        }

        private Utils.FileWatcher jsFileWatcher = null;

        private void setJSFile(Path get) {
            try {
                if (jsFileWatcher != null) {
                    jsFileWatcher.stopThread();
                }
                jsFileWatcher = new FileWatcher(get.toFile(),100) {
                    @Override
                    public void doOnChange(File f) {
                        try {
                            logger.info("File changed "+f);
                            List<String> ret = Files.readAllLines(f.toPath());
                            if (ret != null) {
                                initScript = StringUtils.join(ret, "\n");
                            }
                        } catch (IOException ex) {
                            logger.error("Error rereading file " + f);
                        }
                    }
                };
                jsFileWatcher.watch();
            } catch (IOException ex) {
                Logger.getLogger(OutputSpecFormatter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static class Inquirer {

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
