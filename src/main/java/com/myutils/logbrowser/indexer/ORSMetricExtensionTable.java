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
    public void AddToDB(Record rec) {
        ORSMetricExtension orsRec = (ORSMetricExtension) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);
//        Document JDOMDoc;
//        Element JDOMElement;
//        Reader m_InReader = new StringReader(orsRec.GetText());
//        String m_text=orsRec.GetText();

        try {
//            SAXBuilder m_saxBuilder = new SAXBuilder();
//            JDOMDoc = m_saxBuilder.build(m_InReader);
//            JDOMElement = JDOMDoc.getRootElement();

//            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            Document doc = db.parse(new InputSource(new StringReader(orsRec.GetText())));
//            System.out.println(doc.getFirstChild().getNodeName());
            try {
                stmt.setTimestamp(1, new Timestamp(orsRec.GetAdjustedUsecTime()));
                stmt.setInt(2, ORSMetric.getFileId());
                stmt.setLong(3, orsRec.m_fileOffset);
                stmt.setLong(4, orsRec.getM_FileBytes());
                stmt.setLong(5, orsRec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.ORSSID, orsRec.sid));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSNS, orsRec.getNameSpace()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.METRICFUNC, orsRec.getMethodFunc()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.METRIC_PARAM1, orsRec.getParam1()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.METRIC_PARAM2, orsRec.getParam2()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add trigger: " + e, e);
                throw e;
            }
        } catch (SQLException ex) {
            Main.logger.error("Error adding ORS metric " + ex.getMessage() + "\n", ex);
            //return;
        }

    }
}
