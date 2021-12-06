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
public class OCSSCXMLTreatmentTable extends DBTable {

    public OCSSCXMLTreatmentTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists recHandle_OCSSCXMLTr;");
            getM_dbAccessor().runQuery("drop index if exists chID_OCSSCXMLTr;");
            getM_dbAccessor().runQuery("drop index if exists FileId_OCSSCXMLTr;");
            getM_dbAccessor().runQuery("drop index if exists cgdbid_OCSSCXMLTr;");
            getM_dbAccessor().runQuery("drop index if exists SessIDID_OCSSCXMLTr;");
            getM_dbAccessor().runQuery("drop index if exists SessIDID_unixtime;");

        }
        String query = "create table if not exists OCSSCXMLTr (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",cgdbid INTEGER"
                + ",recHandle INTEGER"
                + ",chID INTEGER"
                + ",SessIDID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OCSSCXMLTr VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        getM_dbAccessor().runQuery("create index if not exists recHandle_OCSSCXMLTr on OCSSCXMLTr (recHandle);");
        getM_dbAccessor().runQuery("create index if not exists chID_OCSSCXMLTr on OCSSCXMLTr (chID);");
        getM_dbAccessor().runQuery("create index if not exists FileId_OCSSCXMLTr on OCSSCXMLTr (FileId);");
        getM_dbAccessor().runQuery("create index if not exists cgdbid_OCSSCXMLTr on OCSSCXMLTr (cgdbid);");
        getM_dbAccessor().runQuery("create index if not exists SessIDID_OCSSCXMLTr on OCSSCXMLTr (SessIDID);");
        getM_dbAccessor().runQuery("create index if not exists SessIDID_unixtime on OCSSCXMLTr (time);");
    }

    @Override
    public void AddToDB(Record _rec) throws SQLException {
        OCSSCXMLTreatment rec = (OCSSCXMLTreatment) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getcgdbid());
            stmt.setInt(7, rec.getrecHandle());
            stmt.setInt(8, rec.getchID());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.OCSSCXMLSESSION, rec.getSessID()));

        }
    });
}


}
