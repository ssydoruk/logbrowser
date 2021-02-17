/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;
import static Utils.Util.intOrDef;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class IxnGMS extends Message {

//    final private static Pattern regGSW_RECORD_HANDLE = Pattern.compile("^\\s*\'GSW_RECORD_HANDLE\'.+= (\\d+)");
    final private static Pattern regIxnActor = Pattern.compile("^\\s*attr_actor_router_id .+ \"([^\"]+)\"$");
    final private static Pattern regIxnSubmitted = Pattern.compile("^\\s*attr_itx_submitted_by .+ \"([^\"]+)\"$");

    private static final Regexs AgentIDs = new Regexs(new Pair[]{
        new Pair("^\\s*attr_agent_id.+ \"([^\"]+)\"$", 1),
        new Pair("^\\s*attr_actor_agent_id.+ \"([^\"]+)\"$", 1),
        new Pair("^\\s*AttributeAgentID.+ \"([^\"]+)\"$", 1)
    });

    private final static Message.Regexs IxnIDs = new Regexs(new Pair[]{
        new Pair("^\\s*attr_itx_id .+ \"([^\"]+)\"$", 1),
        new Pair("^\\s*'InteractionId'.+= \"([^\"]+)\"$", 1),
        new Pair("^\\s*'Id' .+\"([^\"]+)\"$", 1),
        new Pair("^\\s*'ORSI:.+:([\\w~]+)' \\[list\\]", 1)
    });

    private final static Message.Regexs parentIxnID = new Regexs(new Pair[]{
        new Pair("^\\s*attr_prnt_itx_id.+ \"([^\"]+)\"$", 1),
        new Pair("^\\s*'attr_itx_prnt_itx_id'.+= \"([^\"]+)\"$", 1),
        new Pair("^\\s*'Parent' .+\"([^\"]+)\"$", 1)
    });

    private static final Regexs PlaceIDs = new Regexs(new Pair[]{
        new Pair("^\\s*attr_place_id.+ \"([^\"]+)\"$", 1),
        new Pair("^\\s*attr_actor_place_id.+ \"([^\"]+)\"$", 1),
        new Pair("^\\s*AttributePlace.+ \"([^\"]+)\"$", 1)
    });

    private static final Regexs refIDs = new Regexs(new Pair[]{
        new Pair("^\\s*attr_ref_id.+ = (\\d+)", 1),
        new Pair("^\\s*attr_esp_server_refid.+ = (\\d+)", 1),
        new Pair("^\\s*AttributeReferenceID.+ = (\\d+)", 1),
        new Pair("^\\s*AttributeConnID .+ (\\w+)$", 1)
    });

//    private static final Regexs reChainIDs = new Regexs(new Pair[]{
//        new Pair("^\\s*\'GSW_CHAIN_ID\'.+= (\\d+)", 1),
//        new Pair("^\\s*\'chain_id\'.+= (\\d+)", 1),});
    private static final Regexs reQueue = new Regexs(new Pair[]{
        new Pair("^\\s*(?:attr_queue|attr_itx_queue) .+ \"([^\"]+)\"$", 1),
        new Pair("^\\s*AttributeThisDN .+ (\\w+)$", 1),});

    private static final Regexs reMediaType = new Regexs(new Pair[]{
        new Pair("^\\s*(?:attr_media_type|attr_itx_media_type|attr_media_type_name) .+ \"([^\"]+)\"$", 1)}
    );

    private static final Regexs reConnID = new Regexs(new Pair[]{
        new Pair("^\\s*AttributeConnID .+ (\\w+)$", 1)}
    );
    final private static Pattern regService = Pattern.compile("^\\s*'Service'.+= \"([^\"]+)\"$");
    final private static Pattern regMethod = Pattern.compile("^\\s*'Method'.+= \"([^\"]+)\"$");
    final private static Pattern regRouteType = Pattern.compile("^\\s*AttributeRouteType.+ = (\\d+)");
    private final String clientName = null;
    private String messageName = null;

//    private static final Regexs reContact = new Regexs(new Pair[]{
//        new Pair("^\\s*\'GSW_PHONE\'.+= \"([^\"]+)\"", 1),
//        new Pair("^\\s*\'contact_info\'.+= \"([^\"]+)\"", 1),});
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

    public IxnGMS(TableType t) {
        super(t);
        attrs.add(reIxnID);
        attrs.add(reAgentID);
        attrs.add(placeID);
        attrs.add(parentIxnIDAttr);
        attrs.add(refID);
        attrs.add(QueueID);
        attrs.add(mediaType);
        attrs.add(connID);

    }

    public IxnGMS(String fromTo, String msgName) {
        this(TableType.IxnGMS);
        this.messageName = msgName;
        SetInbound(fromTo != null && fromTo.toLowerCase().startsWith("from"));
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

    void setAttributes(ArrayList<String> m_MessageContents) {
        super.setMessageLines(m_MessageContents);
        attrs.parseAttributes();
    }

}
