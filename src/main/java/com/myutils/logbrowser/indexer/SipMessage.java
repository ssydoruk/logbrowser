/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SipMessage extends SIPServerBaseMessage {

    private static final String _prefix = "\n";
    private static final String _suffix = "\r";
    private static final Pattern regSIPMessage = Pattern.compile("^(?:SIP[^ ]+ (.+)|(\\w+)\\s(\\S+)\\s)", Pattern.CASE_INSENSITIVE);
    private static final Pattern regSIPUserPart = Pattern.compile("([^@:]+)@");
    private static final Pattern regSIPResponse = Pattern.compile("(\\w+)$");
    private static final Pattern patternPeer
            = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
    private final static HashSet<String> nonCalls = new HashSet<String>(Arrays.asList("OPTIONS", "NOTIFY", "SUBSCRIBE", "REGISTER"));

    String m_Sendername;
    String m_Receivername;
    String m_Name = null;
    String peerIp = null;
    private String m_RequestURIDN = null;
    private String theFrom = null;
    private String cSeq = null;
    private String peerPort = null;
    private String via;
    private String sipURI = null;

    public SipMessage(ArrayList<String> contents, TableType t,  int fileID) {
        this(contents, t, false, 0, fileID);
    }

    public SipMessage(ArrayList<String> contents, TableType t,boolean m_handlerInProgress, int m_handlerId,  int fileID) {
        super(t,  m_handlerInProgress,  m_handlerId, fileID);
        m_MessageLines.clear();
        for (Object content : contents) {
            String s = (String) content;
            if (s.length() == 0 || (s.length() == 1 && s.charAt(0) == '\r')) {
                m_MessageLines.add("");
            } else {
                if (s.charAt(s.length() - 1) == '\r') {
                    m_MessageLines.add(s.substring(0, s.length() - 2));
                } else {
                    m_MessageLines.add(s);
                }
            }
        }
        m_MessageLines = contents;
        if (contents.size() == 1) {
            Main.logger.info("One line SIP message???" + contents.get(0));
        }
        Main.logger.trace("len: " + contents.get(0).length() + " last: " + contents.get(0).indexOf('\r'));
        if (m_MessageLines.size() > 0) {
            String s = m_MessageLines.get(0);
            if (s.startsWith(_prefix)) {
                m_MessageLines.set(0, s.substring(_prefix.length()));
            }
            s = m_MessageLines.get(m_MessageLines.size() - 1);
            if (s.equals(_suffix)) {
                m_MessageLines.set(m_MessageLines.size() - 1, "");
            } else if (s.endsWith(_suffix)) {
                m_MessageLines.set(m_MessageLines.size() - 1, s.substring(0, s.length() - _suffix.length() - 1));
                m_MessageLines.add("");
            }

        }

    }

    Integer getHandlerInProgress() {
        return (isM_handlerInProgress() ? getM_handlerId() : 0);
    }

    public String getSipURI() {
        return transformSIPURL(sipURI);
    }

    public void setSipURI(String sipURI) {
        this.sipURI = sipURI;
        Matcher m;
        if ((m = regSIPUserPart.matcher(sipURI)).find()) {
            m_RequestURIDN = m.group(1);
        }
    }

    @Override
    public String toString() {
        return "SipMessage{" + "m_Sendername=" + m_Sendername + ", m_Receivername=" + m_Receivername + ", m_Name=" + m_Name + ", m_isRequest=" + isInbound() + '}' + super.toString();
    }

    void SetName(String newValue) {
        m_Name = newValue;
    }

    public String GetFrom() {
        if (theFrom == null) {
            theFrom = GetSIPHeaderURL("From", "f");
        }
        return theFrom;
    }

    public String GetFromUserpart() {
        Matcher m;
        String from = GetFrom();
        if (from == null) {
            return null;
        }
        if ((m = regSIPUserPart.matcher(from)).find()) {
            return m.group(1);
        }
        return null;
    }

    public void SetReceiverName(String newValue) {
        m_Receivername = newValue;
    }

    public void SetSenderName(String newValue) {
        m_Sendername = newValue;
    }

    public void InsertHeader(String header) {
        m_MessageLines.add(0, header);
        m_Name = header;
    }

    public void HaName() {
        String msgName = getM_Name();
        if (msgName.startsWith("INFO") && GetFrom().startsWith("<sip:gsipsync>")) {
            int msg_type = 0;
            String s_type = GetHeaderValue("1=");
            if (s_type != null) {
                msg_type = Integer.parseInt(s_type);
            }

            String id = GetHeaderValue("2=");
            String linked_id = GetHeaderValue("400=");

            int sig = 0;
            String s_sig = GetHeaderValue("4=");
            if (s_sig != null) {
                sig = Integer.parseInt(s_sig);
            }

            String dlg_type = "";
            switch (sig & 0xFE00) {
                case 0x200:
                    dlg_type = "RP ";
                    break;
                case 0x400:
                    dlg_type = "TREATMENT ";
                    break;
                case 0x800:
                    dlg_type = "SM ";
                    break;
                case 0x1000:
                    dlg_type = "XFRD2RP ";
                    break;
                case 0x2000:
                    dlg_type = "SMEMU ";
                    break;
                case 0x4000:
                    dlg_type = "MCU ";
                    break;
                case 0x8000:
                    dlg_type = "TRT ";
                    break;
                case 0x10000:
                    dlg_type = "MOH ";
                    break;
            }

            switch (msg_type) {
                case 1:
                    msgName = "sync REGISTER " + dlg_type + "dialog ";
                    break;
                case 2:
                    msgName = "sync UNREGISTER " + dlg_type + "dialog ";
                    break;
                case 3:
                    msgName = "FULL sync " + dlg_type + "dialog ";
                    break;
                case 4:
                    msgName = "sync " + dlg_type + " dialog DATA ";
                    break;
                case 5:
                    msgName = "sync " + dlg_type + "dialog LINK ";
                    break;
            }
            msgName += id;
            if (linked_id != null && linked_id.length() > 0) {
                msgName += "->" + linked_id;
            }
        }
        setM_Name(msgName);
    }

    public String GetName() {
        Matcher m;
        String n = getM_Name();
        if (n == null) {
            return "null";
        }

        if (Character.isDigit(n.charAt(0))) {
            String cSeq = GetCSeq();
            if (cSeq != null && (m = regSIPResponse.matcher(cSeq)).find()) {
                return n + " " + m.group(1);
            }
            return n;
        } else {
            return n;
        }
    }

    public String getM_Name() {
        if (m_Name == null) {
            Matcher m;
            if ((m = regSIPMessage.matcher(m_MessageLines.get(0))).find()) {
                m_Name = m.group(1);
                if (m_Name == null) {
                    m_Name = m.group(2);
                    setSipURI(m.group(3));
                }
            }
        }

        return m_Name;
    }

    public void setM_Name(String m_Name) {
        this.m_Name = m_Name;
    }

    public String GetCallId() {
        return GetSIPHeader("Call-ID", "i");
    }

    public String GetCSeq() {
        if (cSeq == null) {
            cSeq = GetSIPHeader("CSeq", null);
        }
        return cSeq;
    }

    public String GetTo() {
        String method = getM_Name();
        if (method != null && method.toUpperCase().startsWith("REFER")) {
            return GetSIPHeaderURL("refer-to", null);
        } else {
            return GetSIPHeaderURL("To", "t");
        }
    }

    /*
const char* GSIP_HDRNAME_UNKNOWN				="Unknown-Header";
const char* GSIP_HDRNAME_ACCEPT					="Accept";
const char* GSIP_HDRNAME_ACCEPT_ENCODING		="Accept-Encoding";
const char* GSIP_HDRNAME_ACCEPT_LANGUAGE		="Accept-Language";
const char* GSIP_HDRNAME_ALERT_INFO				="Alert-Info";
const char* GSIP_HDRNAME_ALLOW					="Allow";
const char* GSIP_HDRNAME_AUTHENTICATION_INFO	="Authentication-Info";
const char* GSIP_HDRNAME_AUTHORIZATION			="Authorization";
const char* GSIP_HDRNAME_CALL_ID				="Call-ID";	
const char* GSIP_HDRNAME_CALL_ID_SHORT			="i";
const char* GSIP_HDRNAME_CALL_INFO				="Call-Info";
const char* GSIP_HDRNAME_CONTACT				="Contact";
const char* GSIP_HDRNAME_CONTACT_SHORT			="m";
const char* GSIP_HDRNAME_CONTENT_DISPOSITION	="Content-Disposition";
const char* GSIP_HDRNAME_CONTENT_ENCODING_SHORT	="e";
const char* GSIP_HDRNAME_CONTENT_ENCODING		="Content-Encoding";
const char* GSIP_HDRNAME_CONTENT_LANGUAGE		="Content-Language";
const char* GSIP_HDRNAME_CONTENT_LENGTH_SHORT	="l";
const char* GSIP_HDRNAME_CONTENT_LENGTH			="Content-Length";
const char* GSIP_HDRNAME_CONTENT_TYPE_SHORT		="c";
const char* GSIP_HDRNAME_CONTENT_TYPE			="Content-Type";
const char* GSIP_HDRNAME_CSEQ					="CSeq";
const char* GSIP_HDRNAME_SIPDATE				="LocalDateTime";
const char* GSIP_HDRNAME_ERROR_INFO				="Error-Info";
const char* GSIP_HDRNAME_EXPIRES				="Expires";
const char* GSIP_HDRNAME_FROM					="From";
const char* GSIP_HDRNAME_FROM_SHORT				="f";
const char* GSIP_HDRNAME_IN_REPLY_TO			="In-Reply-To";
const char* GSIP_HDRNAME_MAX_FORWARDS			="Max-Forwards";
const char* GSIP_HDRNAME_MIME_VERSION			="MIME-Version";
const char* GSIP_HDRNAME_MIN_EXPIRES			="Min-Expires";
const char* GSIP_HDRNAME_ORGANIZATION			="Organization";
const char* GSIP_HDRNAME_PRIORITY				="Priority";
const char* GSIP_HDRNAME_PROXY_AUTHENTICATE		="Proxy-Authenticate";
const char* GSIP_HDRNAME_PROXY_AUTHORIZATION	="Proxy-Authorization";
const char* GSIP_HDRNAME_PROXY_REQUIRE			="proxy-require";
const char* GSIP_HDRNAME_RECORD_ROUTE			="Record-Route";
const char* GSIP_HDRNAME_REPLY_TO				="Reply-To";
const char* GSIP_HDRNAME_REQUIRE				="Require";
const char* GSIP_HDRNAME_RETRY_AFTER			="Retry-After";
const char* GSIP_HDRNAME_ROUTE					="Route";
const char* GSIP_HDRNAME_SERVER					="Server";
const char* GSIP_HDRNAME_SUBJECT				="Subject";
const char* GSIP_HDRNAME_SUBJECT_SHORT			="s";
const char* GSIP_HDRNAME_SUPPORTED				="Supported";
const char* GSIP_HDRNAME_TIMESTAMP				="Timestamp";
const char* GSIP_HDRNAME_TO						="To";
const char* GSIP_HDRNAME_TO_SHORT				="t";
const char* GSIP_HDRNAME_UNSUPPORTED			="Unsupported";
const char* GSIP_HDRNAME_USER_AGENT				="User-Agent";
const char* GSIP_HDRNAME_VIA					="Via";
const char* GSIP_HDRNAME_VIA_SHORT				="v";
const char* GSIP_HDRNAME_WARNING				="Warning";
const char* GSIP_HDRNAME_WWW_AUTHENTICATE		="WWW-Authenticate";

const char* GSIP_HDRNAME_REFER_TO				="Refer-To";
const char* GSIP_HDRNAME_REFER_TO_SHORT			="r";
const char* GSIP_HDRNAME_REFERRED_BY			="Referred-By";
const char* GSIP_HDRNAME_REFERRED_BY_SHORT		="b";
const char* GSIP_HDRNAME_ALLOW_EVENTS			="Allow-Events";
const char* GSIP_HDRNAME_ALLOW_EVENTS_SHORT		="u";
const char* GSIP_HDRNAME_SUBSCRIPTION_STATE		="Subscription-State";

const char* GSIP_HDRNAME_SESSION_EXPIRES		="Session-Expires";
const char* GSIP_HDRNAME_SESSION_EXPIRES_SHORT  ="x";
const char* GSIP_HDRNAME_MIN_SE					="Min-SE";

const char* GSIP_HDRNAME_RACK					="RAck";
const char* GSIP_HDRNAME_RSEQ					="RSeq";

const char* GSIP_HDRNAME_REPLACES				="Replaces";

     */
    private String getVia() {
        if (via == null) {
            via = GetSIPHeader("Via", "v");
        }
        return via;
    }

    public String GetViaUri() {
        return null;
        /*no need to save Via URI*/
//        return transformSIPURL(getVia());

//        if (tmp != null && tmp.length() > 0) {
//            int idx = tmp.indexOf(';');
//            if (idx >= 0) {
//                return tmp.substring(0, idx);
//            }
//        }
//        return "";
    }

    public String GetViaBranch() {
        String tmp = getVia();

        if (tmp != null) {
            int indexOfBranch = tmp.toLowerCase().indexOf("branch=");
            if (indexOfBranch != -1) {
                int indexOfComma = tmp.toLowerCase().indexOf(';', indexOfBranch);
                if (indexOfComma == -1) {
                    indexOfComma = tmp.length();
                }
                return tmp.substring(indexOfBranch + 7, indexOfComma);
            }
        }
        return "";
    }

    public String getPeerIp() {
        return peerIp;
    }

    public void setPeerIp(String peerIp) {
        this.peerIp = peerIp;
    }

    private void parsePeer() {
        if (peerIp == null) {

            String peer = isInbound() ? m_Sendername : m_Receivername;
            if (peer == null) {
                peerIp = "";
                peerPort = "";
            } else {
                Matcher matcher = patternPeer.matcher(peer);
                if (matcher.find()) {
                    peerIp = matcher.group(1);
                    peerPort = matcher.group(2);
                }
            }

        }
    }

    public String GetPeerIp() {
        parsePeer();

        return peerIp;
    }

    public int GetPeerPort() {
        parsePeer();

        if (peerPort != null && !peerPort.isEmpty()) {
            try {
                return Integer.parseInt(peerPort);
            } catch (NumberFormatException e) {
                Main.logger.error("Error parsing [" + peerPort + "]", e);
                return 0;
            }
        }
        return 0;

    }

    public String getPeerPort() {
        return peerPort;
    }

    public void setPeerPort(String peerPort) {
        this.peerPort = peerPort;
    }

    boolean isCallRelated() {
        String cSeq = GetCSeq();
        if (cSeq != null) {
            String[] split = StringUtils.split(cSeq);
            if (split != null && split.length > 1) {
                return !nonCalls.contains(split[1].toUpperCase());
            }
        }
        return true;
    }

    String getRequestURIDN() {
        return m_RequestURIDN;
    }

    String getUUID() {
        String ret = GetSIPHeader("X-Genesys-CallUUID", null);
        if (ret != null && ret.length() == 32) {
            return ret;
        }
        return null;
    }

}
