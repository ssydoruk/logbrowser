/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.StripQuotes;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 */
public class OCSTable extends DBTable {

    public OCSTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("OCS_" + getM_dbAccessor().getM_alias());
        addIndex("FileId");
        addIndex("cgDBID");
        addIndex("recHandle");
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("thisDNID");
        addIndex("time");
        addIndex("errMessageID");
        addIndex("chID");
        addIndex("otherDNID");
        addIndex("ReferenceId");

        dropIndexes();

        String query = "create table if not exists OCS_logbr (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentID char(32),"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER"
                + ",Inbound bit"
                + ",UUIDID INTEGER"
                + ",seqno integer"
                + ",sourceID INTEGER"
                + ",sourceIDb INTEGER"
                + ",IsTServerReq bit"
                + ",recHandle integer"
                + ",chID integer"
                + ",cgDBID integer"
                + ",errMessageID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OCS_logbr VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");

    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
    public void AddToDB(Record _rec) {
        OcsMessage rec = (OcsMessage) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OcsMessage.getFileId());
            setFieldLong(stmt, 3, rec.getM_fileOffset());
            setFieldLong(stmt, 4, rec.getM_FileBytes());
            setFieldInt(stmt, 5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, rec.GetMessageName()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.DN, Record.cleanDN(rec.getM_ThisDN())));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.DN, Record.cleanDN(rec.getOtherDN())));

            setFieldString(stmt, 9, "");//+ "agentID char(32),"
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.ConnID, rec.GetConnID()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.ConnID, rec.GetTransferConnID()));
            stmt.setInt(12, rec.getM_refID());
            stmt.setBoolean(13, rec.isInbound());
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, StripQuotes(rec.getAttributeTrim("AttributeCallUUID", 32))));
            setFieldLong(stmt, 15, rec.getAttributeHex("AttributeEventSequenceNumber"));
            setFieldInt(stmt, 16, Main.getRef(ReferenceType.App, rec.getM_TserverSRCp()));
            setFieldInt(stmt, 17, Main.getRef(ReferenceType.App, rec.getM_TserverSRCb()));
            stmt.setBoolean(18, rec.isIsTServerReq());
            setFieldLong(stmt, 19, rec.getRecordHandle());
            setFieldLong(stmt, 20, rec.getUData("GSW_CHAIN_ID", -1, true));
//            Main.logger.info("CHID: "+rec.getUData("GSW_CHAIN_ID", -1, true));
            setFieldLong(stmt, 21, rec.getUData("GSW_CAMPAIGN_GROUP_DBID", -1, true));
            setFieldInt(stmt, 22, Main.getRef(ReferenceType.TLIBERROR, rec.getErrorMessage(rec.GetMessageName())));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
