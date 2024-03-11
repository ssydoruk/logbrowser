/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

public class Ixn extends Message {

    private static final Pattern regMsgName = Pattern.compile("message '([^']+)");
    private static final Pattern regMsgNameSendingTo = Pattern.compile("Sending to .+ ([^:]+):\\s+'([^']+)' \\(");
    private static final Pattern regMsgNameReceivedfrom = Pattern.compile("Received from .+ ([^:]+):\\s+'([^']+)' \\(");
    private static final Pattern regMsgThirdParty = Pattern.compile("request to '([^']+)'");

    private static final Pattern regMsgAndClient = Pattern.compile("message '([^']+)'.+(to|from) (?:client|server) '([^']+)'");
    private static final Pattern regMsgAndClientProxy = Pattern.compile("through proxy '([^']+)'");

    //    private static final Pattern regGSW_RECORD_HANDLE = Pattern.compile("^\\s+\'GSW_RECORD_HANDLE\'.+= (\\d+)");
    private static final Pattern regIxnActor = Pattern.compile("^\\s+attr_actor_router_id .+ \"([^\"]+)\"$");
    private static final Pattern regIxnSubmitted = Pattern.compile("^\\s+attr_itx_submitted_by .+ \"([^\"]+)\"$");

    private static final Regexs AgentIDs = new Regexs(new Pair[]{
            new Pair<>("^\\s+attr_agent_id.+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+attr_actor_agent_id.+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+AttributeAgentID.+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+'Agent'.+ \"([^\"]+)\"$", 1)
    });

    private final static Message.Regexs IxnIDs = new Regexs(new Pair[]{
            new Pair<>("^\\s+attr_itx_id .+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+'InteractionId'.+= \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+'Id' .+\"([^\"]+)\"$", 1),
            new Pair<>("^\\s*'ORSI:.+:([\\w~]+)' \\[list\\]", 1)
    });

    private final static Message.Regexs parentIxnID = new Regexs(new Utils.Pair[]{
            new Pair<>("^\\s+attr_prnt_itx_id.+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+'attr_itx_prnt_itx_id'.+= \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+'Parent' .+\"([^\"]+)\"$", 1)
    });

    private static final Regexs PlaceIDs = new Regexs(new Utils.Pair[]{
            new Pair<>("^\\s+attr_place_id.+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+attr_actor_place_id.+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+AttributePlace.+ \"([^\"]+)\"$", 1)
    });

    private static final Regexs refIDs = new Regexs(new Pair[]{
            new Pair<>("^\\s+attr_ref_id.+ = (\\d+)", 1),
            new Pair<>("^\\s+attr_esp_server_refid.+ = (\\d+)", 1),
            new Pair<>("^\\s+AttributeReferenceID.+ = (\\d+)", 1),
            new Pair<>("^\\s+AttributeConnID .+ (\\w+)$", 1)
    });

    private static final Regexs reQueue = new Regexs(new Pair[]{
            new Pair<>("^\\s+(?:attr_queue|attr_itx_queue) .+ \"([^\"]+)\"$", 1),
            new Pair<>("^\\s+AttributeThisDN .+ (\\w+)$", 1),});

    private static final Regexs reMediaType = new Regexs(new Pair[]{
            new Pair<>("^\\s+(?:attr_media_type|attr_itx_media_type|attr_media_type_name) .+ \"([^\"]+)\"$", 1)}
    );

    private static final Regexs reConnID = new Regexs(new Pair[]{
            new Pair<>("^\\s+AttributeConnID .+ (\\w+)$", 1)}
    );
    private static final Pattern regService = Pattern.compile("^\\s+'Service'.+= \"([^\"]+)\"$");
    private static final Pattern regMethod = Pattern.compile("^\\s+'Method'.+= \"([^\"]+)\"$");
    private static final Pattern regRouteType = Pattern.compile("^\\s+AttributeRouteType.+ = (\\d+)");
    private final RegExAttribute reIxnID = new RegExAttribute(IxnIDs);
    private final RegExAttribute reAgentID = new RegExAttribute(AgentIDs);
    private final RegExAttribute placeID = new RegExAttribute(PlaceIDs);
    private final RegExAttribute parentIxnIDAttr = new RegExAttribute(parentIxnID);
    private final RegExAttribute refID = new RegExAttribute(refIDs);
    //    private RegExAttribute ChainID = new RegExAttribute(reChainIDs);
    private final RegExAttribute QueueID = new RegExAttribute(reQueue);
    //    private RegExAttribute ContactID = new RegExAttribute(reContact);
    private final RegExAttribute mediaType = new RegExAttribute(reMediaType);
    private final RegExAttribute connID = new RegExAttribute(reConnID);
    MessageAttributes attrs = new MessageAttributes();
    Attribute ixnID = new Attribute() {
        @Override
        String getValue() {
            return FindByRx(IxnIDs);
        }
    };
    Attribute attr1 = new Attribute() {
        @Override
        String getValue() {
            String retService = FindByRx(regService, 1, null);
            String retMethod = FindByRx(regMethod, 1, null);
            StringBuilder ret = new StringBuilder(50);
            if (retService != null) {
                ret.append(retService);
            }
            if (retMethod != null) {
                if (ret.length() > 0) {
                    ret.append("|");
                }
                ret.append(retMethod);
            }
            if (ret.length() > 0) {
                return ret.toString();
            }
            return null;
        }
    };
    private String clientName = null;
    private String messageName = null;

    public Ixn(TableType t, ArrayList<String> messageLines, int fileID) {
        super(t, messageLines, fileID);
        attrs.add(reIxnID);
        attrs.add(reAgentID);
        attrs.add(placeID);
        attrs.add(parentIxnIDAttr);
        attrs.add(refID);
        attrs.add(QueueID);
        attrs.add(mediaType);
        attrs.add(connID);

        attrs.parseAttributes();
    }

    public Ixn(ArrayList<String> messageLines, int fileID) {
        this(TableType.Ixn, messageLines, fileID);
        this.messageName = parseMessageName();
    }

    public String GetSid() {
        return GetHeaderValueNoQotes("session");
    }

    public String GetCall() {
        return GetHeaderValueNoQotes("call");
    }

    private String parseMessageName() {
        String ret = "";
//        SetInbound(true);
        if (!m_MessageLines.isEmpty()) {
            Matcher m;
            Main.logger.debug("[" + m_MessageLines.get(0) + "]");
            if ((m = regMsgNameReceivedfrom.matcher(m_MessageLines.get(0))).find()) {
                clientName = m.group(1);
                SetInbound(true);
                Main.logger.debug("[-1-]");
                return m.group(2);
            }

            if ((m = regMsgNameSendingTo.matcher(m_MessageLines.get(0))).find()) {
                clientName = m.group(1);
                SetInbound(false);
                Main.logger.debug("[-2-]");
                return m.group(2);
            }


            if ((m = regMsgAndClient.matcher(m_MessageLines.get(0))).find()) {
                clientName = m.group(3);
                String msg = m.group(1);

                if (m.group(2).startsWith("f")) {
                    Main.logger.debug("[-3-]");

                    SetInbound(true);
                } else {
                    Main.logger.debug("[-4-]");

                    SetInbound(false);
                }
                if ((m = regMsgAndClientProxy.matcher(m_MessageLines.get(0).substring(m.end()))).find()) {
                    clientName = m.group(1);
                }

                return msg;
            }
            ret = FindByRx(regMsgName, m_MessageLines.get(0), 1, null);
            if (ret != null) {
                SetInbound(!ret.startsWith("Event"));
                return ret;
            }

            String r = FindByRx(regMsgThirdParty, m_MessageLines.get(0), 1, null);
            if (r != null) {
                SetInbound(false);
                clientName = r;
                ret = getAttr1();
                if (ret != null && !ret.isEmpty()) {
                    return ret;
                } else {
                    return "ExternalServiceRequest";
                }
            }

        }
        return ret;
    }

    String GetMessageName() {
        if (messageName != null && !messageName.isEmpty()) {
            return messageName;
        }
        String attr = attr1.toString();
        if (attr != null && !attr.isEmpty()) {
            return "ExternalServiceRequest";
        }
        return null;

    }

    String GetIxnID() {
        return ixnID.toString();
    }

    String GetMedia() {
        return mediaType.toString();
    }

    String GetIxnQueue() {
        return QueueID.toString();
    }

    long getM_refID() {
        return intOrDef(refID.toString(), 0);
    }

    String GetConnID() {
        return connID.toString();
    }

    String GetParentIxnID() {
        return parentIxnIDAttr.toString();
    }

    String GetClient() {
        String ret = clientName;
        if (ret == null || ret.length() == 0) {
            ret = FindByRx(regIxnActor, 1, "");
        }
        if (ret == null || ret.length() == 0) {
            ret = FindByRx(regIxnSubmitted, 1, "");
        }
        return ret;
    }

    String GetAgent() {
        return reAgentID.toString();
    }

    String GetPlace() {
        return placeID.toString();
    }

    String GetRouter() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    String getAttr1() {
        return attr1.toString();
    }

    String getAttr2() {
        String msgName = GetMessageName();
        if (msgName != null && msgName.contentEquals("RequestRouteCall")) {

            String ret = FindByRx(regRouteType, 1, null);
            if (ret != null) {
                return "RouteType=" + ret;
            }

        }
        return null;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, GetIxnID()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.IxnMedia, GetMedia()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, cleanDN(GetIxnQueue())));
        stmt.setLong(10, getM_refID());
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.ConnID, GetConnID()));
        stmt.setBoolean(12, isInbound());
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.IxnID, GetParentIxnID()));
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.App, GetClient()));
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.Agent, GetAgent()));
        setFieldInt(stmt, 16, Main.getRef(ReferenceType.Place, GetPlace()));
        setFieldInt(stmt, 17, Main.getRef(ReferenceType.TLIBATTR1, getAttr1()));
        setFieldInt(stmt, 18, Main.getRef(ReferenceType.TLIBATTR2, getAttr2()));
        return true;
    }
}
