package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.ReferenceType;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ORSMetricByCallIdQuery extends IQuery {

    private Integer[] SIDs = null;
    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;
    private DynamicTreeNode<OptionNode> orsMetrics = null;


    public ORSMetricByCallIdQuery() throws SQLException {
        addRef("metricid", "metric", ReferenceType.METRIC.toString(), FieldType.Mandatory);
        addRef("sidid", "sid", ReferenceType.ORSSID.toString(), FieldType.Mandatory);
        addRef("param1id", "param1", ReferenceType.METRIC_PARAM1.toString(), FieldType.Optional);
    }

    public ORSMetricByCallIdQuery(Integer[] SIDIDs) throws SQLException {
        this();
        SIDs = SIDIDs;
    }

    public ORSMetricByCallIdQuery(Integer[] SIDID, DynamicTreeNode<OptionNode> orsReportComponent) throws SQLException {
        this(SIDID);
        this.orsMetrics = orsReportComponent;
    }

    public ORSMetricByCallIdQuery(DynamicTreeNode<OptionNode> orsReportComponent) throws SQLException {
        this();
        this.orsMetrics = orsReportComponent;
    }

    public ORSMetricByCallIdQuery(Integer SIDID) throws SQLException {
        this();
        SIDs = new Integer[1];
        SIDs[0] = SIDID;

    }
    private String[] SIDbyCONNID(Integer[] ConnIDs) {
        
        return null;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

//        Integer[] sids = m_connector.getIDs(this, "select distinct sidid from orssess_logbr WHERE "+getWhere("uuidid", SIDs));
//        " WHERE " + 
        query = "SELECT orsm.*,files.id as files_id,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname, app00.name as app,"
                + "files.component as component "
                + getRefFields()
                + "\nFROM orsmetr_" + alias + " AS orsm "
                + "\nINNER JOIN FILE_"
                + alias + " AS files on fileId=files.id"
                + "\ninner join app as app00 on files.appnameid=app00.id"
                + "\n" + getRefs() + "\n"
                + myGetWhere() + ((inquirer.getCr().isAddOrderBy()) ? " ORDER BY orsm.time" : "")
                + "\n" + getLimitClause() + ";";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        on_error:
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            ILogRecord rec = new ORSMetric(m_resultSet);
            recLoaded(rec);
            return rec;
        }
        doneExtracting(MsgType.ORSM);
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

    private String myGetWhere() throws SQLException {
        if (DatabaseConnector.refTableExist(ReferenceType.METRIC)) {
            AddCheckedWhere("orsm.metricid", ReferenceType.METRIC, FindNode(orsMetrics, DialogItem.ORS_STRATEGY_METRIC, null, null), "AND");
        }
        AddCheckedWhere("orsm.param1id", ReferenceType.METRIC_PARAM1, FindNode(orsMetrics, DialogItem.ORS_STRATEGY_METRICPARAM1, null, null), "AND");
        if (SIDs != null && SIDs.length > 0) {
            addWhere(getWhere("orsm.sidid", SIDs), "AND");
        }
        addFileFilters("orsm", "fileid");
        addDateTimeFilters("orsm", "time");

        return addWheres.makeWhere(true);

    }

}
