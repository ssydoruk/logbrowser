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
public class ORSMetricExtensionTable extends DBTable {

    public ORSMetricExtensionTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("ORSmetrExt_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("sidid");
        addIndex("nsid");
        addIndex("funcid");
        addIndex("param1id");
        addIndex("param2id");
        dropIndexes();

        String query = "CREATE TABLE if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "sidid INTEGER"
                + ",nsid INTEGER"
                + ",funcid INTEGER"
                + ",param1id INTEGER"
                + ",param2id INTEGER"
                + ");";

        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record rec) throws SQLException {
        ORSMetricExtension orsRec = (ORSMetricExtension) rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
                stmt.setTimestamp(1, new Timestamp(orsRec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileID());
                stmt.setLong(3, orsRec.getM_fileOffset());
                stmt.setLong(4, orsRec.getM_FileBytes());
                stmt.setLong(5, orsRec.getM_line());

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.ORSSID, orsRec.sid));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSNS, orsRec.getNameSpace()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.METRICFUNC, orsRec.getMethodFunc()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.METRIC_PARAM1, orsRec.getParam1()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.METRIC_PARAM2, orsRec.getParam2()));
            }
        });

    }
}
