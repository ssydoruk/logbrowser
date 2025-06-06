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
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class IxnGMS extends Message {

    //    private static final Pattern regGSW_RECORD_HANDLE = Pattern.compile("^\\s*\'GSW_RECORD_HANDLE\'.+= (\\d+)");
    private static final Regexs AgentIDs = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*attr_agent_id.+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*attr_actor_agent_id.+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*AttributeAgentID.+ \"([^\"]+)\"$"), 1));
        }
    });

    private final static Message.Regexs IxnIDs = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*attr_itx_id .+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*'InteractionId'.+= \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*'Id' .+\"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*'ORSI:.+:([\\w~]+)' \\[list\\]"), 1));
        }
    });

    private final static Message.Regexs parentIxnID = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*attr_prnt_itx_id.+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*'attr_itx_prnt_itx_id'.+= \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*'Parent' .+\"([^\"]+)\"$"), 1));
        }
    });

    private static final Regexs PlaceIDs = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*attr_place_id.+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*attr_actor_place_id.+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*AttributePlace.+ \"([^\"]+)\"$"), 1));
        }
    });

    private static final Regexs refIDs = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*attr_ref_id.+ = (\\d+)"), 1));
            add(new Pair<>(Pattern.compile("^\\s*attr_esp_server_refid.+ = (\\d+)"), 1));
            add(new Pair<>(Pattern.compile("^\\s*AttributeReferenceID.+ = (\\d+)"), 1));
            add(new Pair<>(Pattern.compile("^\\s*AttributeConnID .+ (\\w+)$"), 1));
        }
    });

    //    private static final Regexs reChainIDs = new Regexs(
//    new ArrayList<Pair<Pattern, Integer>> () {
//        {
    //        add(new Pair<>(Pattern.compile("^\\s*\'GSW_CHAIN_ID\'.+= (\\d+)"), 1));
    //        add(new Pair<>(Pattern.compile("^\\s*\'chain_id\'.+= (\\d+)"), 1));});
    private static final Regexs reQueue = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*(?:attr_queue|attr_itx_queue) .+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*AttributeThisDN .+ (\\w+)$"), 1));
        }
    }
    );

    private static final Regexs reMediaType = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*(?:attr_media_type|attr_itx_media_type|attr_media_type_name) .+ \"([^\"]+)\"$"), 1));
        }
    }
    );

    private static final Regexs reConnID = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*AttributeConnID .+ (\\w+)$"), 1));
        }
    }
    );
    private static final Regexs reService = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s+'Service'.+= \"([^\"]+)\"$"), 1));
        }
    });
    private static final Regexs reMethod = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s+'Method'.+= \"([^\"]+)\"$"), 1));
        }
    });

    private static final Regexs regIxnActor = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s+attr_actor_router_id .+ \"([^\"]+)\"$"), 1));
        }
    });
    private static final Regexs regIxnSubmitted = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s+attr_itx_submitted_by .+ \"([^\"]+)\"$"), 1));
        }
    });

    private static final Regexs regRouteType = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s+AttributeRouteType.+ = (\\d+)"), 1));
        }
    });
    private final String clientName = null;

    //    private static final Regexs reContact = new Regexs(
//    new ArrayList<Pair<Pattern, Integer>> () {
//        {
    //        new Pair<>("^\\s*\'GSW_PHONE\'.+= \"([^\"]+)\""), 1));
    //        new Pair<>("^\\s*\'contact_info\'.+= \"([^\"]+)\""), 1));});
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
    private final RegExAttribute attrRouteType = new RegExAttribute(regRouteType);
    private final RegExAttribute attrIxnActor = new RegExAttribute(regIxnActor);
    private final RegExAttribute attrIxnSubmitted = new RegExAttribute(regIxnSubmitted);
    private final RegExAttribute attrService = new RegExAttribute(reService);
    private final RegExAttribute attrMethod = new RegExAttribute(reMethod);

    private String messageName = null;

    public IxnGMS(TableType t, int fileID) {
        super(t, fileID);
    }

    public IxnGMS(String fromTo, String msgName, int fileID) {
        this(TableType.IxnGMS, fileID);
        this.messageName = msgName;
        SetInbound(fromTo != null && fromTo.toLowerCase().startsWith("from"));
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

    public String GetSid() {
        return GetHeaderValueNoQotes("session");
    }

    public String GetCall() {
        return GetHeaderValueNoQotes("call");
    }

    String GetMessageName() {
        if (messageName != null && !messageName.isEmpty()) {
            return messageName;
        }
        String attr = getAttr1();
        if (attr != null && !attr.isEmpty()) {
            return "ExternalServiceRequest";
        }
        return null;

    }

    String GetIxnID() {
        return reIxnID.getStringValue();
    }

    String GetMedia() {
        return mediaType.getStringValue();
    }

    String GetIxnQueue() {
        return QueueID.getStringValue();
    }

    long getM_refID() {
        return refID.getIntValue();
    }

    String GetConnID() {
        return connID.getStringValue();
    }

    String GetParentIxnID() {
        return parentIxnIDAttr.getStringValue();
    }

    String GetClient() {
        String ret = clientName;
        if (StringUtils.isEmpty(ret)) {
            ret = attrIxnActor.getStringValue();
        }
        if (StringUtils.isEmpty(ret)) {
            ret = attrIxnSubmitted.getStringValue();
        }
        return ret;
    }

    String GetAgent() {
        String ag = reAgentID.getStringValue();
        return ag;
    }

    String GetPlace() {
        return placeID.getStringValue();
    }

    String GetRouter() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    String getAttr1() {
        StringBuilder ret = new StringBuilder(StringUtils.defaultIfBlank(attrService.getStringValue(), ""));
        if (StringUtils.isNotBlank(ret)) {
            ret.append("|");
        }
        ret.append(StringUtils.defaultIfBlank(attrMethod.getStringValue(), ""));
        return StringUtils.isNotBlank(ret) ? ret.toString() : "";
    }

    String getAttr2() {
        String msgName = GetMessageName();
        if (msgName != null && msgName.contentEquals("RequestRouteCall")) {

            String ret = attrRouteType.getStringValue();
            if (ret != null) {
                return "RouteType=" + ret;
            }

        }
        return null;
    }

    void setAttributes(ArrayList<String> m_MessageContents) {
        parseAttributes(
                m_MessageContents,
                reIxnID,
                reAgentID,
                placeID,
                parentIxnIDAttr,
                refID,
                QueueID,
                mediaType,
                connID,
                attrService,
                attrMethod,
                attrIxnActor,
                attrIxnSubmitted,
                attrRouteType
        );
    }

}
