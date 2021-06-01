/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Pattern;

public class IxnNonIxn extends Message {

    private static final Pattern rxUserEventIDs = Pattern.compile("^\\s+AttributeUserEvent.+ = (\\d+)");

    private static final Pattern rxThisDN = Pattern.compile("^\\s+AttributeThisDN .+ \"([^\"]+)\"$");
    private final Ixn parentMsg;
    MessageAttributes attrs = new MessageAttributes();

    IxnNonIxn(TableType tableType, Ixn msg) {
        super(tableType);
        parentMsg = msg;
//        setMessageLines(parentMsg.m_MessageLines);

    }

    public String GetSid() {
        return parentMsg.GetSid();
    }

    public String GetCall() {
        return parentMsg.GetCall();
    }

    String GetMessageName() {
        return parentMsg.GetMessageName();
    }

    String GetIxnID() {
        return parentMsg.GetIxnID();
    }

    String GetMedia() {
        return parentMsg.GetMedia();
    }

    String GetIxnQueue() {
        return parentMsg.GetIxnQueue();
    }

    long getM_refID() {
        return parentMsg.getM_refID();
    }

    String GetConnID() {
        return parentMsg.GetConnID();
    }

    String GetParentIxnID() {
        return parentMsg.GetParentIxnID();
    }

    String GetClient() {
        return parentMsg.GetClient();
    }

    String GetAgent() {
        return parentMsg.GetAgent();
    }

    String GetPlace() {
        return parentMsg.GetPlace();
    }

    String GetRouter() {
        return parentMsg.GetRouter();
    }

    String getAttr1() {
        return parentMsg.getAttr1();
    }

    String getAttr2() {
        return parentMsg.getAttr2();
    }

    String getUserEvent() {
        return Main.lookupConstant(GenesysConstants.TSERVER,
                FindByRx(parentMsg.m_MessageLines, rxUserEventIDs, 1, (Integer) null));
    }

    String getThisDN() {
        return FindByRx(parentMsg.m_MessageLines, rxThisDN, 1, (String) null);
    }

}
