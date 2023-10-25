/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class URSStatTable extends DBTable {

    public URSStatTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "URSSTAT");
    }

    @Override
    public void InitDB() {


        addIndex("time");
        addIndex("FileId");
        addIndex("agentNameID");
        addIndex("placeNameID");
        addIndex("ssNameID");
        addIndex("statPrevID");
        addIndex("statNewID");
        addIndex("VoiceSwitchID");
        addIndex("VoiceDNID");
        addIndex("VoiceStatID");
        addIndex("ChatStatID");
        addIndex("EmailStatID");
        dropIndexes();

        String query = "create table if not exists URSSTAT (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",notifRef integer"
                + ",agentNameID integer"
                + ",placeNameID integer"
                + ",ssNameID integer" // reference to StatServer name
                + ",statPrevID integer"
                + ",statNewID integer"
                + ",VoiceSwitchID integer"
                + ",VoiceDNID integer"
                + ",VoiceStatID integer"
                + ",ChatStatID integer"
                + ",EmailStatID integer"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO URSSTAT VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
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
    public void FinalizeDB() throws SQLException {
        createIndexes();
    }


}
