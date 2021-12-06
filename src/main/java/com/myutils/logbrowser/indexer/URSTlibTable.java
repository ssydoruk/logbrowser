/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 */
public class URSTlibTable extends DBTable {

    public URSTlibTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("URS_" + getM_dbAccessor().getM_alias());
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("UUIDID");
        addIndex("time");
        addIndex("thisDNID");
        addIndex("nameID");
        addIndex("seqno");
        addIndex("IXNIDID");
        addIndex("SourceID");
        addIndex("ServerHandle");
        addIndex("errMessageID");
        addIndex("referenceID");
        addIndex("attr1ID");
        addIndex("attr2ID");
        addIndex("DNISID");
        addIndex("agentID");
        addIndex("ANIID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "Inbound bit,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "seqno integer"
                + ",line int"
                + ",uuidid integer"
                + ",IXNIDID integer"
                + ",SourceID integer"
                + ",ServerHandle integer"
                + ",referenceID integer"
                + ",errMessageID integer"
                + ",attr1ID integer"
                + ",attr2ID integer"
                + ",DNISID integer"
                + ",ANIID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
    }

    @Override
    public void FinalizeDB() {
//            getM_dbAccessor().runQuery("create index if not exists URS_time_" + getM_dbAccessor().getM_alias() +" on URS_" + getM_dbAccessor().getM_alias() + " (time);");
        createIndexes();

        getM_dbAccessor().runQuery("create temp table tmp_urs_server as select distinct SourceID, ServerHandle from "
                + getTabName() + " where SourceID > 0 limit 1;");

        getM_dbAccessor().runQuery("update " + getTabName()
                + " set SourceID = (select SourceID from tmp_urs_server t where t.ServerHandle=ServerHandle) where SourceID<=0 ");

    }

    @Override
    public void AddToDB(Record rec) throws SQLException {
        UrsMessage theRec = (UrsMessage) rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 2, Main.getRef(ReferenceType.TEvent, theRec.GetMessageName()));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, rec.cleanDN(theRec.getThisDN())));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, rec.cleanDN(theRec.getOtherDN())));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.Agent, theRec.getHeaderTrim("AttributeAgentID")));
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, theRec.GetConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, theRec.GetTransferConnID()));
            stmt.setBoolean(8, theRec.isInbound());
            setFieldInt(stmt, 9, rec.getFileID());
            setFieldLong(stmt, 10, theRec.getM_fileOffset());
            setFieldLong(stmt, 11, theRec.getFileBytes());
            setFieldLong(stmt, 12, theRec.getAttributeHex("AttributeEventSequenceNumber"));
            setFieldInt(stmt, 13, theRec.getM_line());
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, theRec.NoQuotes(theRec.getAttributeTrim("AttributeCallUUID", 32))));
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.IxnID, theRec.NoQuotes(theRec.getIxnID())));
            setFieldInt(stmt, 16, Main.getRef(ReferenceType.App, theRec.getServer()));
            setFieldInt(stmt, 17, theRec.getServerHandle());
            setFieldInt(stmt, 18, theRec.getRefID());
            setFieldInt(stmt, 19, Main.getRef(ReferenceType.TLIBERROR, theRec.getErrorMessage(theRec.GetMessageName())));
            setFieldInt(stmt, 20, Main.getRef(ReferenceType.TLIBATTR1, theRec.getAttr1()));
            setFieldInt(stmt, 21, Main.getRef(ReferenceType.TLIBATTR2, theRec.getAttr2()));
            setFieldInt(stmt, 22, Main.getRef(ReferenceType.DN, rec.cleanDN(theRec.getDNIS())));
            setFieldInt(stmt, 23, Main.getRef(ReferenceType.DN, rec.cleanDN(theRec.getANI())));

                        }
        });
    }



}
