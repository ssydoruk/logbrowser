/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author ssydoruk
 */
public abstract class DBTable {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private SqliteAccessor m_dbAccessor;
    protected int m_InsertStatementId;
//    protected Parser m_pParser=null;
    protected TableType m_type = TableType.UNKNOWN;
    private boolean tabCreated = false;
    private String tabName;
    private final ArrayList<ArrayList<String>> idxFields;
    private int recordsAdded = 0;

    public int getRecordsAdded() {
        return recordsAdded;
    }

    protected void setTabName(String tab) {
        this.tabName = tab;
    }

    protected void addIndex(String name) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add(name);
        idxFields.add(arrayList);
    }

    protected void addIndex(String[] name) {
        idxFields.add(new ArrayList<String>(Arrays.asList(name)));
    }

    private String idxName(String tab, ArrayList<String> fields) {
        StringBuilder ret = new StringBuilder();
        ret.append(tabName).append("_");
        for (String field : fields) {
            ret.append(field);
        }
        return ret.toString();
    }

    protected void dropIndex(String fld, ArrayList<String> idxField) {
        getM_dbAccessor().runQuery("drop index if exists " + idxName(tabName, idxField) + ";");
    }

    private String fileIDField;

    /**
     * Get the value of fileIDField
     *
     * @return the value of fileIDField
     */
    public String getFileIDField() {

        return (fileIDField != null) ? fileIDField : "FileId";
    }

    /**
     * Set the value of fileIDField
     *
     * @param fileIDField new value of fileIDField
     */
    public void setFileIDField(String fileIDField) {
        this.fileIDField = fileIDField;
    }

    protected void dropIndexes() {
        try {
            if (Main.isDbExisted() && getM_dbAccessor().TableExist(tabName)) {
                ArrayList<Long> filesToDelete = getM_dbAccessor().getFilesToDelete();
                if (filesToDelete != null) {
                    String fileIDField1 = getFileIDField();
                    if (fileIDField1 != null) { //if tables do not use file ID field, they should set this to null
                        for (Long long1 : filesToDelete) {
                            getM_dbAccessor().runQuery("delete from " + tabName + " where " + getFileIDField() + "=" + long1 + ";");

                        }
                    } else {
                        Main.logger.info("No fileID for table " + tabName);
                    }
                }
                for (ArrayList<String> idxField : idxFields) {
                    dropIndex(tabName, idxField);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DBTable.class.getName()).log(Level.INFO, "error dropping table " + tabName, ex);
        }
    }

    protected void createIndex(String t, ArrayList<String> idxField) {
        getM_dbAccessor().runQuery("create index if not exists " + idxName(t, idxField) + " on " + t + " (" + StringUtils.join(idxField, ",") + ")" + " where " + idxField + " is not null;");
    }

    protected void createIndexes() {
        for (ArrayList<String> idxField : idxFields) {
            createIndex(tabName, idxField);
        }
    }

    public String getTabName() {
        return tabName;
    }

    public DBTable(DBAccessor dbaccessor, TableType type) {
        m_dbAccessor = (SqliteAccessor) dbaccessor;
        m_type = type;
        setTabName(type.toString());
        idxFields = new ArrayList();
    }

    public DBTable(DBAccessor dbaccessor) {
        this(dbaccessor, TableType.UNKNOWN);
    }

    public TableType getM_type() {
        return m_type;
    }

    protected String db_alias() {
        return m_dbAccessor.getM_alias();
    }

    public SqliteAccessor getM_dbAccessor() {
        return m_dbAccessor;
    }

    /**
     * @return the m_pParser
     */
//    public Parser getM_pParser() {
//        return m_pParser;
//    }
    public abstract void InitDB();

    public abstract void FinalizeDB() throws Exception;

    public abstract void AddToDB(Record rec) throws Exception;

    ;
    
    public String StrOrEmpty(String param) {
        if (param == null) {
            return "";
        } else {
            return param;
        }
    }

    public boolean isTabCreated() {
        return tabCreated;
    }

    private int currentID; // primary key on the table. Implies that each table has to have field named
    //ID

    void checkInit() {
        if (!tabCreated) {
            InitDB();
            String tabName1 = getTabName();
            if (tabName1 != null) {
                try {
                    currentID = Main.getSQLiteaccessor().getID("select max(id) from " + tabName1, tabName1, -1);
                    if (currentID == -1) {
                        currentID = 0;
                    }
//                    Main.logger.info("Table " + tabName1 + ": currentID: " + currentID);
                } catch (Exception ex) {
                    currentID = 0;
                    logger.log(org.apache.logging.log4j.Level.FATAL, ex);
                }
            }
            tabCreated = true;
        }
    }

    protected void setFieldInt(PreparedStatement stmt, int i, Integer ref) throws SQLException {
        if (ref != null) {
            stmt.setInt(i, ref);
        } else {
//            stmt.setInt(i, 0);
            stmt.setNull(i, java.sql.Types.INTEGER);
        }
    }

    protected void setFieldLong(PreparedStatement stmt, int i, Long ref) throws SQLException {
        if (ref != null) {
            stmt.setLong(i, ref);
        } else {
            stmt.setNull(i, java.sql.Types.BIGINT);
        }
    }

    static public void setFieldString(PreparedStatement stmt, int i, String ref) throws SQLException {
        if (ref != null) {
            stmt.setString(i, ref);
        } else {
            stmt.setNull(i, java.sql.Types.CHAR);
        }
    }

    protected void generateID(PreparedStatement stmt, int i, Record _rec) throws SQLException {
        currentID++;
        Main.logger.debug("generate ID for table " + getTabName() + ": " + currentID + "; i:" + i + " rec: " + _rec.toString());
        stmt.setInt(i, currentID);
        _rec.setLastID(currentID);
    }

    public void setCurrentID(int currentID) {
        this.currentID = currentID;
    }

    public int getCurrentID() {
        return currentID;
    }

    void recordAdded() {
        recordsAdded++;
    }

}
