/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Pattern;

public class IxnNonIxn extends Message {

    private static final Pattern rxUserEventIDs = Pattern.compile("^\\s+AttributeUserEvent.+ = (\\d+)");

    private static final Pattern rxThisDN = Pattern.compile("^\\s+AttributeThisDN .+ \"([^\"]+)\"$");
    private final Ixn parentMsg;
    MessageAttributes attrs = new MessageAttributes();

    IxnNonIxn(TableType tableType, Ixn msg, int fileID) {
        super(tableType, fileID);
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
        setFieldInt(stmt, 19, Main.getRef(ReferenceType.TEvent, getUserEvent()));
        setFieldInt(stmt, 20, Main.getRef(ReferenceType.DN, cleanDN(getThisDN())));
        return true;
    }
}
