/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static Utils.Util.StripQuotes;

/**
 * @author ssydoruk
 */
public class OCSTable extends DBTable {

    public OCSTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"OCS_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
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


    }

    @Override
    public String getInsert() {
        return "INSERT INTO OCS_logbr VALUES(NULL,?,?,?,?,?,"
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
                + ");";
    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }




}
