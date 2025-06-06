/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ssydoruk
 */
public abstract class DBTable {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    private final SqliteAccessor m_dbAccessor;
    private final ArrayList<ArrayList<String>> idxFields;
    protected TableType m_type = TableType.UNKNOWN;
    private boolean tabCreated = false;
    private String tabName;
    private int recordsAdded = 0;
    private String fileIDField;

    protected DBTable(SqliteAccessor dbaccessor, TableType type, String tabName) {
        m_dbAccessor = dbaccessor;
        m_type = type;
        setTabName(tabName);
        idxFields = new ArrayList<>();
        initID();

    }

    protected DBTable(SqliteAccessor dbaccessor, TableType type) {
        this(dbaccessor, type, type.toString());
    }

    protected DBTable(SqliteAccessor dbaccessor, String tabName) {
        this(dbaccessor, TableType.UNKNOWN, tabName);
    }

    protected DBTable(SqliteAccessor dbaccessor) {
        this(dbaccessor, TableType.UNKNOWN);
    }
    //ID

    private void initID() {
        synchronized (m_dbAccessor) {
            String tabName1 = getTabName();
            try {
                if (tabName1 != null && m_dbAccessor.TableExist(tabName1)) {
                    int currentID = m_dbAccessor.getID("select max(id) from " + tabName1, tabName1, -1);
                    if (currentID > 0) {
                        Record.setID(m_type, currentID);
                    }
                }
            } catch (Exception e) {
                logger.fatal("Not able to init table", e);
            }
        }
    }

    public int getRecordsAdded() {
        return recordsAdded;
    }

    protected void addIndex(String name) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(name);
        idxFields.add(arrayList);
    }

    protected void addIndex(String[] name) {
        idxFields.add(new ArrayList<>(Arrays.asList(name)));
    }

    private String idxName(String tab, ArrayList<String> fields) {
        StringBuilder ret = new StringBuilder().append(tab).append("_");
        for (String field : fields) {
            ret.append(field);
        }
        return ret.toString();
    }

    protected void dropIndex(String tab, ArrayList<String> idxField) {
        getM_dbAccessor().runQuery("drop index if exists " + idxName(tab, idxField) + ";");
    }

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
        synchronized (getM_dbAccessor()) {
            try {
                if (Main.isDbExisted() && getM_dbAccessor().TableExist(tabName)) {
                    List<Long> filesToDelete = getM_dbAccessor().getFilesToDelete();
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
                    idxFields.forEach(idxField -> dropIndex(tabName, idxField));
                }
            } catch (Exception ex) {
                logger.error("error dropping table " + tabName, ex);
            }
        }
    }

    protected void createIndex(String t, ArrayList<String> idxField) {
        getM_dbAccessor().runQuery("create index if not exists " + idxName(t, idxField) + " on " + t + " (" + StringUtils.join(idxField, ",") + ")" + " where " + idxField + " is not null;");
    }

    protected void createIndexes() {
        idxFields.forEach(idxField -> createIndex(tabName, idxField));
    }

    public String getTabName() {
        return tabName;
    }

    protected void setTabName(String tab) {
        this.tabName = tab;
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

    public abstract void InitDB();

    public abstract String getInsert();

    public abstract void FinalizeDB() throws SQLException;

    public boolean isTabCreated() {
        return tabCreated;
    }

    void checkInit() {
        synchronized (m_dbAccessor.getM_conn()) {
            if (!tabCreated) {
                m_dbAccessor.Commit();
                InitDB();
                tabCreated = true;
            }
        }
    }

    public void addCnt(int cnt) {
        recordsAdded += cnt;
    }
}
