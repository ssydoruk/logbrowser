/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 */
public class TLibTable extends DBTable {

    public TLibTable(DBAccessor dbaccessor, TableType type) {
        super(dbaccessor, type);
    }

    @Override
    public void InitDB() {
        setTabName("tlib_" + getM_dbAccessor().getM_alias());
        addIndex("ConnectionIDID");
        addIndex("TransferIDID");
        addIndex("HandlerId");
        addIndex("time");
        addIndex("uuidID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("nameID");
        addIndex("FileId");
        addIndex("seqno");
        addIndex("agentIDID");
        addIndex("ReferenceId");
        addIndex("SourceID");
        addIndex("errMessageID");
        addIndex("line");
        addIndex("attr1ID");
        addIndex("attr2ID");
        addIndex("DNISID");
        addIndex("ANIID");
        addIndex("PlaceID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentIDID INTEGER,"
                + "aniID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER,"
                + "Inbound bit,"
                + "SourceID INTEGER,"
                + "MsgID varchar(16),"
                + "LocationID INTEGER,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER,"
                + "line int,"
                + "seqno integer"
                + ",uuidID INTEGER"
                + ",err integer"
                + ",calltype integer"
                + ",errMessageID integer"
                + ",attr1ID integer"
                + ",attr2ID integer"
                + ",DNISID integer"
                + ",PlaceID integer"
                + ");";

        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record aRec) {
        TLibMessage theRec = (TLibMessage) aRec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            generateID(stmt, 1, theRec);
            stmt.setTimestamp(2, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.TEvent, theRec.GetMessageName()));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, theRec.getThisDN()));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.DN, theRec.getAttributeDN("AttributeOtherDN")));
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.Agent, Record.NoQuotes(theRec.getAttributeTrim("AttributeAgentID"))));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.DN, Record.SingleQuotes(theRec.getAttribute("AttributeANI"))));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.ConnID, theRec.getConnID()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.ConnID, theRec.GetTransferConnID()));
            setFieldLong(stmt, 10, theRec.getRefID());
            stmt.setBoolean(11, theRec.isInbound());
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.App, theRec.getM_source()));
            setFieldString(stmt, 13, theRec.getAttributeTrim("AttributePrivateMsgID"));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.Switch, Record.NoQuotes(theRec.getAttributeTrim("AttributeLocation"))));
            setFieldInt(stmt, 15, TLibMessage.getFileId());
            setFieldLong(stmt, 16, theRec.m_fileOffset);
            setFieldLong(stmt, 17, theRec.GetFileBytes());
            setFieldInt(stmt, 18, (TLibMessage.m_handlerInProgress ? TLibMessage.m_handlerId : 0));
            Main.logger.trace("theRec.m_handlerId:" + TLibMessage.m_handlerId + " theRec.m_handlerInProgress :" + TLibMessage.m_handlerInProgress);

            setFieldInt(stmt, 19, theRec.m_line);
            setFieldLong(stmt, 20, theRec.getSeqNo());
            setFieldInt(stmt, 21, Main.getRef(ReferenceType.UUID, theRec.getUUID()));
            setFieldLong(stmt, 22, theRec.getErrorCode());
            setFieldLong(stmt, 23, theRec.getAttributeLong("AttributeCallType"));
            setFieldInt(stmt, 24, Main.getRef(ReferenceType.TLIBERROR, theRec.getErrorMessage(theRec.GetMessageName())));
            setFieldInt(stmt, 25, Main.getRef(ReferenceType.TLIBATTR1, theRec.getAttr1()));
            setFieldInt(stmt, 26, Main.getRef(ReferenceType.TLIBATTR2, theRec.getAttr2()));
            setFieldInt(stmt, 27, Main.getRef(ReferenceType.DN, theRec.getAttributeDN("AttributeDNIS")));
            setFieldInt(stmt, 28, Main.getRef(ReferenceType.Place, theRec.getAttributeDN("AttributePlace")));
            TLibMessage.SetTlibId(getCurrentID());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add message type " + getM_type() + ": " + e, e);
        }

    }

}
