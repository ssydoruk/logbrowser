package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.TableType;
import org.apache.logging.log4j.LogManager;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

public class TableQuery extends IQuery {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private final String tabName;
    private final MsgType msgType;
    private final ArrayList<String> whereFields = new ArrayList<>();
    private String idName;
    private String idsSubQuery;
    private String existsTable;
    private String orderBy = null;
    private boolean addAll;
    //    private Class<ILogRecord> resultClass;
    private Class<TLibEvent> myClass;
    private WhereType whereType = WhereType.WhereNone;

    public TableQuery(MsgType msgType, String _tabName, String _idName, String _addWhere) {
        this(msgType, _tabName, _addWhere);
        idName = _idName;
    }

    //
//    public void setResultClass(ILogRecord.class result1) {
//        this.resultClass = result;
//    }
    public TableQuery(MsgType msgType, String _tabName) {
        super();
        tabName = _tabName;
        this.msgType = msgType;
        addAll = true;
    }

    public TableQuery(MsgType msgType, String _tabName, String _addWhere) {
        this(msgType, _tabName);
        addWheres.addWhere(_addWhere);
    }

    TableQuery(MsgType msgType, TableType tableType, String connidid, String where) {
        this(msgType, tableType.toString(), connidid, where);
    }

    TableQuery(MsgType msgType, TableType tableType) {
        this(msgType, tableType.toString());
    }

    TableQuery(String tmpTable) {
        this(MsgType.UNKNOWN, tmpTable);
    }

    public String fldName(String fld) {
        return getTabAlias() + "." + fld;
    }

    public String getTabAlias() {
        return tabName.substring(0, 2) + "01";
    }

    public void setWhereType(WhereType whereType) {
        this.whereType = whereType;
    }

    public void setExistsTable(String existsTable) {
        this.existsTable = existsTable;
    }

    void setOrderBy(String string) {
        this.orderBy = string;
    }

    public void setAddAll(boolean b) {
        addAll = b;
    }

    FullTableColors getFullTable() throws SQLException {
        Execute();
        FullTableColors ret = new FullTableColors();
        ret.setMetaData(getMetaData());

        int columnCount = getMetaData().getColumnCount();
        int cnt = 0;
        ResultSet rs;
        while ((rs = GetNextRS()) != null) {
            ArrayList<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            QueryTools.DebugRec(rs);
            ret.addRow(row);
            cnt++;
        }
        inquirer.logger.debug("\tRetrieved " + cnt + " records");

        return ret;
    }

    void addHiddenField(String rowType) {

    }

    void setResultClass(Class<TLibEvent> aClass) {
        this.myClass = aClass;
    }

    public void addWhereField(String field) {
        whereFields.add(field);
    }

    private String makeWhereExists() {
        StringBuilder ret = new StringBuilder();
        ret.append("exists ( select id from ").append(existsTable).append(" where ");
        for (String whereField : whereFields) {
            if (ret.length() > 0) {
                ret.append(" and ");
            }
            ret.append(existsTable).append(".").append(whereField).append("=").append(getTabAlias()).append(".").append(whereField);
        }
        ret.append(" )");
        return ret.toString();
    }

    @Override
    public void Execute() throws SQLException {
        StringBuilder qString = new StringBuilder(512);

        inquirer.logger.debug("execute");
        StringBuilder callIdCondition = new StringBuilder(256);

        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        addFileFilters(getTabAlias(), "fileid");
        addDateTimeFilters(getTabAlias(), "time");
        switch (whereType) {
            case WhereExists:
                callIdCondition.append(addWheres.makeWhere(makeWhereExists(), "AND", false));
                break;

            default:
                if (idsSubQuery != null) {
                    addWheres.addWhere(idName + " in ( " + idsSubQuery + " ) ", "AND");
                }
                callIdCondition.append(addWheres.makeWhere(false));
        }

        if (callIdCondition.length() > 0) {
            callIdCondition.insert(0, " where ");
        }

        qString.append("SELECT ");
        int emptyLen = qString.length();

        if (addAll) {
            qString.append(stdFields(getTabAlias()));
        }

        qString.append(addedFieldString(qString.length() > emptyLen, false));
        if (msgType != MsgType.UNKNOWN) {
            if (qString.length() > emptyLen) {
                qString.append(",");
            }

            qString.append("files.id as files_id,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  app00.name as app, files.component as component \n");
        }

        qString.append(getRefFields())
                .append(getNullFields())
                .append("\nFROM ")
                .append(tabName)
                .append(" AS ")
                .append(getTabAlias())
                .append("\n")
                .append(getJoins());
        if (msgType != MsgType.UNKNOWN) {
            qString.append("\n\tINNER JOIN FILE_")
                    .append(alias)
                    .append(" AS files on ")
                    .append(getTabAlias())
                    .append(".fileId=files.id\ninner join app as app00 on files.appnameid=app00.id\n");
        }
        qString.append(getRefs())
                .append(callIdCondition);
        if (inquirer.getCr().isAddOrderBy()) {
            if (orderBy != null) {
                qString.append("\nORDER BY ").append(orderBy);
            } else {
                qString.append("\nORDER BY ").append(getTabAlias()).append(".time");
            }
        }
        qString.append("\n").append(getLimitClause());
        qString.append(";");

        m_resultSet = m_connector.executeQuery(this, qString.toString());
        recCnt = 0;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return m_resultSet.getMetaData();
    }

    public ResultSet GetNextRS() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            return m_resultSet;
        }
        doneExtracting(msgType);
        return null;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        ICalculatedFields calcFields = getCalcFields();
        ResultSet GetNextRS = GetNextRS();
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeInterruptException();
        }

        if (GetNextRS != null) {
            if (calcFields == null) {
                if (myClass == null) {
                    ILogRecord rec = null;
                    if (msgType == MsgType.TLIB) {
                        rec = new TLibEvent(GetNextRS);
                    } else {
                        rec = new TabRecord(msgType, GetNextRS);
                    }
                    recLoaded(rec);
                    return rec;
                } else {
                    try {
                        ILogRecord ret = myClass.newInstance();
                        ret.initRecord(GetNextRS, msgType);
                        recLoaded(ret);
                        return ret;
                    } catch (InstantiationException | IllegalAccessException ex) {
                        logger.error("fatal: ", ex);
                        throw new SQLException(ex);
                    }
                }

            } else {
                ILogRecord rec = new TabRecord(msgType, GetNextRS, calcFields);
                recLoaded(rec);
                return rec;
            }

        }
        return null;
    }

    @Override
    public void Reset() throws SQLException {
        if (m_resultSet != null) {
            m_resultSet.close();
        }
        m_connector.releaseConnector(this);
        m_connector = null;
    }

    /**
     * @return the idsSubQuery
     */
    public String getIdsSubQuery() {
        return idsSubQuery;
    }

    public void setIdsSubQuery(String idsSubQuery) {
        this.idsSubQuery = idsSubQuery;
    }

    /**
     * @param idsSubQuery the idsSubQuery to set
     */
    public void setIdsSubQuery(String idName, String idsSubQuery) {
        this.idName = idName;
        this.idsSubQuery = idsSubQuery;
    }

    @Override
    public String getQuery() {
        return query;
    }

    enum WhereType {

        WhereNone,
        WhereExists
    }

}
