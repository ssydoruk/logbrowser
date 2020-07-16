package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.inquirer.gui.TabResultDataModel;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.MissingFormatArgumentException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
            return ((Integer) m_callIdHash.get(callId)).intValue();
        } else {
            m_callIdHash.put(callId, m_callIdCount++);
            int ret = m_callIdCount - 1;
            return ret;
        }
    }
    private final XmlCfg cfg;

    private HashSet<String> m_filter;
    private HashMap<String, RecordLayout> outSpec = new HashMap<>();
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

    @Override
    public void ProcessLayout() {
        for (RecordLayout lo : outSpec.values()) {
            lo.UpdateFormatString();
        }
    }


    private void doRefreshLayout() throws Exception {
        for (org.w3c.dom.Element el : cfg.getLayouts()) {
            String msgType = el.getAttribute("MsgType").toLowerCase();
            outSpec.put(msgType, new RecordLayout(el, msgType));
        }

    }

    @Override
    public void refreshFormatter() throws Exception {
        if (cfg.loadFile()) {
            outSpec.clear();
            doRefreshLayout();
        }
    }

    public void SetTlibFilter(String filter) {
        m_filter = new HashSet<>();
        String[] fList = filter.split(",");
        m_filter.addAll(Arrays.asList(fList));
    }

    public static abstract class Parameter {

        private String m_ShortFormat;
        private String m_Title;
        private boolean hidden;
        private HashSet<RegexParam> m_match = new HashSet<>();
        private HashSet<RegexParam> m_filter = new HashSet<>();
        private String m_format;
        private boolean isStatus;
        private String prevValue = "";

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

        private String getFormatGroup(String str, ILogRecord record) throws MissingFormatArgumentException, Exception {
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

        protected void ParseParam(Element e) {
            ParseParam(e, false);
        }

        protected void ParseParam(Element e, boolean ignorePatternForDBFields) {
            
            if (e != null) {
                try {
                    isStatus = Boolean.parseBoolean(e.getAttribute("status"));
                    m_format = e.getAttribute("format");
                    m_ShortFormat = e.getAttribute("shortFormat");
                } catch (Exception ex) {
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

        class RegexParam {
            
            private int m_group;
            private Pattern m_matchPattern;
            private ArrayList<Integer> m_groups = null;
            private String retAttribute = null;
            String expr = null;
            CallableStatement st;
            private int iRetGroup;
            private Pattern mFileMatch;

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
//                        logger.log(org.apache.logging.log4j.Level.FATAL, ex);
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
                    Logger.getLogger(OutputSpecFormatter.class.getName()).log(Level.SEVERE, null, ex);
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
//        String m_match = null;
//        int m_group;
//        boolean m_ignoreCase;
//        Pattern m_matchPattern = null;

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
        file("file");
        
        private final String name;
        
        private ParamType(String s) {
            name = s;
        }
        
        public boolean equalsName(String otherName) {
            return (otherName == null) ? false : name.equals(otherName);
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

        private RecordLayout(org.w3c.dom.Element el, String msgType) throws Exception {
            // get format attribute, save format string
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
                
        }
        parameter.setHidden(getAttribute(paramElement, "hidden", false));
        
        parameters.add(parameter);
        
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
//                StringBuilder outString = new StringBuilder(512);
//                ArrayList<String> paramValues = new ArrayList<>(parameters.size());
for (Parameter param : parameters) {
    if (!param.hasFormat()) {
        String s = param.GetValue(record);
        if (s == null) { //ignore record
            return "";
        } else {
//                            paramValues.add(s);
        }
        ps.addField(param, s);
    } else {
        String s1 = param.FormatValue(record);
        if (s1 == null) {
            return "";
        }
        ps.addField(param, s1);
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
    }
}
