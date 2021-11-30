package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author akolo
 */
public class StSTEventTable extends DBTable {

    String m_MessageName;

    public StSTEventTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void InitDB() {
        setTabName("STSTEVENT_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("switchID");
        addIndex("TServerID");
        addIndex("otherDNID");
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("ReferenceId");
        addIndex("errMessageID");
        dropIndexes();

        String query = "create table if not exists STSTEVENT_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "switchID INTEGER,"
                + "TServerID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER,"
                + "FileId INT,"
                + "FileOffset BIGINT,"
                + "FileBytes INT,"
                + "line INT,"
                + "errMessageID integer"
                + ""
                //                        ",dec_conn_id CHAR(32)" +
                //                        ",dec_transfer_id CHAR(32)" +
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO STSTEVENT_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?"
                + ",?,?"
                //                + ",?,?"
                + ",?,?,?,?,?,?,?);");
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {

        StSTEventMessage theRec = (StSTEventMessage) _rec;

         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 2, Main.getRef(ReferenceType.TEvent, theRec.GetMessageName()));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, theRec.SingleQuotes(theRec.getThisDN())));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.Switch, theRec.getSwitch()));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.App, theRec.getTServer()));
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.DN, theRec.cleanDN(theRec.getAttributeDN("AttributeOtherDN"))));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, theRec.getAgent()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.ConnID, theRec.GetConnID()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.ConnID, theRec.getAttributeTrim("AttributeFirstTransferConnID")));
            setFieldLong(stmt, 10, theRec.getAttributeLong("AttributeReferenceID"));
            stmt.setInt(11, StSTEventMessage.getFileId());
            stmt.setLong(12, theRec.getM_fileOffset());
            stmt.setLong(13, theRec.getFileBytes());
            stmt.setInt(14, theRec.getM_line());
            setFieldInt(stmt, 15, null);
//            setFieldString(stmt,10,theRec.GetDecConnID());
//            setFieldString(stmt,11,theRec.GetDecTransferConnID());

                        }
        });
    }



}
