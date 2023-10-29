package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ConnIdFromCallIdBlockQuery {

    private final ArrayList<Integer> m_callIds;
    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;

    public ConnIdFromCallIdBlockQuery(ArrayList<Integer> callIds) {
        m_callIds = callIds;
    }

    public void Attach(IQueryResults queryResult) {
    }

    public void Execute() throws SQLException {
        inquirer.logger.debug("**Execute in  " + this.getClass());
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        if (m_callIds == null || m_callIds.isEmpty()) {
            throw new SQLException("no conn ID defined");
        }
        Integer[] ids = m_callIds.toArray(new Integer[m_callIds.size()]);

        String ConnIDWhere = IQuery.getWhere("sip.callidid", m_callIds.toArray(new Integer[m_callIds.size()]), false);
        StringBuilder query = new StringBuilder(1024);
        boolean addUnion = false;
        if (DatabaseConnector.TableExist("sip_" + alias) && DatabaseConnector.TableExist("trigger_" + alias)
                && DatabaseConnector.TableExist("tlib_" + alias)) {
            query
                    .append("SELECT distinct connectionidid as connid FROM sip_")
                    .append(alias)
                    .append(" AS sip\ninner join file_logbr as sf on sf.id=sip.fileid\nINNER JOIN trigger_")
                    .append(alias)
                    .append(" AS trgr ON trgr.handlerId=sip.handlerid\ninner join file_logbr as tf on tf.id=trgr.fileid and tf.appnameid=sf.appnameid\nINNER JOIN handler_")
                    .append(alias)
                    .append(" AS handler ON handler.textid=trgr.textid \ninner join file_logbr as hf on hf.id=handler.fileid and hf.appnameid=sf.appnameid\nINNER JOIN tlib_")
                    .append(alias)
                    .append(" AS tlib ON handler.id=tlib.handlerid \ninner join file_logbr as tlf on tlf.id=tlib.fileid and tlf.appnameid=sf.appnameid\nWHERE sip.handlerid>0 and connectionidid>0 AND ")
                    .append(ConnIDWhere);
            addUnion = true;
        }
        if (DatabaseConnector.TableExist("sip_" + alias) && DatabaseConnector.TableExist("tlib_" + alias)) {
            if (addUnion) {
                query.append("\nUNION");
                addUnion = false;
            }
            query
                    .append("\nSELECT distinct connectionidid as connid FROM sip_")
                    .append(alias)
                    .append(" AS sip \ninner join file_logbr as sf on sf.id=sip.fileid\nINNER JOIN tlib_")
                    .append(alias)
                    .append(" AS tlib ON tlib.handlerId=sip.handlerId AND sip.handlerId>0"
                            + "\ninner join file_logbr as tf on tf.id=tlib.fileid and tf.appnameid=sf.appnameid\nWHERE ")
                    .append(ConnIDWhere);
            addUnion = true;
        }
        if (DatabaseConnector.TableExist("tlib_" + alias) && DatabaseConnector.TableExist("cireq_" + alias)) {
            if (addUnion) {
                query.append("\nUNION");
                addUnion = false;
            }
            query
                    .append("\nSELECT distinct connectionidid as connid from tlib_")
                    .append(alias)
                    .append(" AS tlib \ninner join file_logbr as sf on sf.id=tlib.fileid\nINNER JOIN cireq_")
                    .append(alias)
                    .append(" AS cireq ON cireq.nameid=tlib.nameid AND cireq.refid=tlib.referenceid AND cireq.thisdnid=tlib.thisdnid AND cireq.otherdnid=tlib.otherdnid \ninner join file_logbr as cif on cif.id=cireq.fileid and cif.appnameid=sf.appnameid\nINNER JOIN sip_")
                    .append(alias)
                    .append(" AS sip ON cireq.handlerid=sip.handlerid AND sip.handlerid>0\ninner join file_logbr as sif on sif.id=sip.fileid and sif.appnameid=sf.appnameid\nWHERE ")
                    .append(ConnIDWhere);
            addUnion = true;
        }
        if (DatabaseConnector.TableExist("sip_" + alias) && DatabaseConnector.TableExist("connid_" + alias)) {
            if (addUnion) {
                query.append("\nUNION");
                addUnion = false;
            }
            query
                    .append("\nSELECT distinct connectionidid as connid FROM connid_logbr AS cid"
                            + "\ninner join file_logbr as sf on sf.id=cid.fileid"
                            + "\nINNER JOIN sip_logbr AS sip ON sip.handlerId=cid.handlerId AND sip.handlerId>0 "
                            + "\ninner join file_logbr as sif on sif.id=sip.fileid and sif.appnameid=sf.appnameid"
                            + "\nWHERE ")
                    .append(ConnIDWhere);
            addUnion = true;
        }
        if (DatabaseConnector.TableExist("sip_" + alias) && DatabaseConnector.TableExist("trigger_" + alias)
                && DatabaseConnector.TableExist("connid_" + alias)) {
            if (addUnion) {
                query.append("\nUNION");
                addUnion = false;
            }
            query
                    .append("\nSELECT distinct connectionidid as connid  FROM sip_logbr AS sip"
                            + "\ninner join file_logbr as sf on sf.id=sip.fileid"
                            + "\nINNER JOIN trigger_logbr AS trgr ON trgr.handlerId=sip.handlerid"
                            + "\ninner join file_logbr as tf on tf.id=trgr.fileid and tf.appnameid=sf.appnameid"
                            + "\nINNER JOIN handler_logbr AS handler ON handler.textid=trgr.textid"
                            + "\ninner join file_logbr as hf on hf.id=handler.fileid and hf.appnameid=sf.appnameid"
                            + "\nINNER JOIN connid_logbr AS tlib ON handler.id=tlib.handlerid"
                            + "\ninner join file_logbr as tlf on tlf.id=tlib.fileid and tlf.appnameid=sf.appnameid"
                            + "\nWHERE ")
                    .append(ConnIDWhere)
                    .append(";");
        }

        m_resultSet = m_connector.executeQuery(this, query.toString());
        recCnt = 0;
    }

    public Integer GetNext() throws SQLException {
        if (m_resultSet != null) {
            while (m_resultSet.next()) {
                recCnt++;
                QueryTools.DebugRec(m_resultSet);
                Integer connId = m_resultSet.getInt("connid");
                if (connId != null) {
                    return connId;
                }
            }
        }
        inquirer.logger.debug("++ " + this.getClass() + ": extracted " + recCnt + " records");
        return null;
    }

    public void Reset() throws SQLException {
        if (m_resultSet != null) {
            m_resultSet.close();
        }
        m_connector.releaseConnector(this);
        m_connector = null;
    }
}
