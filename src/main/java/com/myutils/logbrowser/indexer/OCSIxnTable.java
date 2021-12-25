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
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class OCSIxnTable extends DBTable {

    public OCSIxnTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists OCSIxnDBfileid;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnnameID ;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnunixtime;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnIxnID ;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnMediaTypeID;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnIxnQueueID ;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnPhoneID ;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnRecHandle ;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnRefId ;");
            getM_dbAccessor().runQuery("drop index if exists OCSIxnChainID ;");

        }
        String query = "create table if not exists OCSIxn (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameID INTEGER"
                + ",IxnID INTEGER"
                + ",MediaTypeID INTEGER"
                + ",IxnQueueID INTEGER"
                + ",PhoneID INTEGER"
                + ",RecHandle INTEGER"
                + ",RefId INTEGER"
                + ",ChainID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO OCSIxn VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        getM_dbAccessor().runQuery("create index if not exists OCSIxnDBfileid on OCSIxn (FileId);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnnameID  on OCSIxn (nameID);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnunixtime on OCSIxn (time);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnIxnID  on OCSIxn (IxnID);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnMediaTypeID on  OCSIxn (MediaTypeID);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnIxnQueueID  on OCSIxn (IxnQueueID);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnPhoneID  on OCSIxn (PhoneID);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnRecHandle  on OCSIxn (RecHandle);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnRefId  on OCSIxn (RefId);");
        getM_dbAccessor().runQuery("create index if not exists OCSIxnChainID  on OCSIxn (ChainID);");
    }


}
