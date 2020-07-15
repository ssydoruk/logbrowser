/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;

public class ORSMM extends Message {

    private String m_TserverSRC;
    private String m_MessageName;
    private boolean isTServerReq;

    public ORSMM(ArrayList messageLines) {
        super(TableType.ORSMMessage, messageLines);
    }

    ORSMM(String m_msgName, String m_TserverSRC, ArrayList m_MessageContents, boolean b) {
        super(TableType.ORSMMessage);
        m_MessageLines = m_MessageContents;
        m_MessageName = m_msgName;
        this.m_TserverSRC = m_TserverSRC;
        this.isTServerReq = b;
        if (m_MessageContents != null) {
            m_MessageLines.add(0, m_msgName);
        }
    }

    String GetMessageName() {
        return m_MessageName;
    }

    String GetIxnID() {
        String ret = getMMAttributeString("attr_itx_id");
        if (ret == null || ret.length() == 0) {
            ret = getMMUserDataString("InteractionId");
        }
        return ret;
    }

    String GetNewIxnID() {
        return getMMAttrEnvelopeString("NewInteractionId");
    }

    long getM_refID() {
        return getMMAttributeLong("attr_ref_id");
    }

    boolean isIsTServerReq() {
        return isTServerReq;
    }

    String GetParentIxnID() {
        String ret = getMMAttributeString("attr_itx_prnt_itx_id");
        return ret;
    }

}
