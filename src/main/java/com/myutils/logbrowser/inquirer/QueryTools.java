/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.jidesoft.swing.MultilineLabel;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

import static com.myutils.logbrowser.inquirer.QueryTools.getWhere;
import static com.myutils.logbrowser.inquirer.QueryTools.uniqueInts;

enum IDType {
    UNKNOWN,
    UUID,
    ConnID,
    OCSSID,
    OCSRecordHandle,
    ORSSID,
    IxnID,
    OCSChainID,
    CG,
    DN,
    AGENT,
    CGDBID,
    PLACE,
    IxnQueue,
    OCSCampaign,
    OCSCampaignDBID,
    GMSSESSION,
    SIPCallID,
    TLibRefID,
    JSONID,
    SIPHandlerID,
    SIPTriggerID,
    TLIBID,
    SIPID,
    PEERIP,
    ORSCallID,
    MCPCallID,
    JSessionID,
    GWS_DeviceID,
    BrowserClientID,
    WWEUserID,
    ANYPARAM,
}

/**
 * @author ssydoruk
 */
class IxnIDs {

    IDType type;
    Integer[] ids;

    public IxnIDs() {
        type = IDType.UNKNOWN;
    }
}

class SearchIDs {

    final static int m_maxDepth = 5;

    private static final HashMap<Integer, Integer> tmpConnIDs = new HashMap();
    private final HashMap<IDType, Integer[]> CallIDs = new HashMap<>();

    private static Integer[] getCallIDs(Integer[] ids) throws SQLException {
        tmpConnIDs.clear();
        for (String tab : new String[]{"tlib_logbr", "ocs_logbr", "ors_logbr", "urs_logbr"}) {
            getRelatedConnIDs(tab, "connectionIdid", "TransferIdid", ids, 1);
            getRelatedConnIDs(tab, "TransferIdid", "connectionIdid", ids, 1);
        }
        return tmpConnIDs.keySet().toArray(new Integer[tmpConnIDs.size()]);
    }

    private static Integer[] getIxnIDs(Integer[] ids) throws SQLException {
        tmpConnIDs.clear();
        for (String tab : new String[]{"orsmm"}) {
            getRelatedConnIDs(tab, "IXNIDID", "PARENTIXNIDID", ids, 1);
            getRelatedConnIDs(tab, "PARENTIXNIDID", "IXNIDID", ids, 1);
        }
        return tmpConnIDs.keySet().toArray(new Integer[tmpConnIDs.size()]);
    }

    static private boolean addTmp(Integer[] ids) {
        boolean ret = false;
        for (int i = 0; i < ids.length; i++) {
            if (!tmpConnIDs.containsKey(ids[i])) {
                tmpConnIDs.put(ids[i], 0);
                ret = true;

            }
        }
        return ret;
    }

    private static void getRelatedConnIDs(String tab, String search, String ret, Integer[] ids, int depth) throws SQLException {
        if (depth >= m_maxDepth) {
            return;
        }

        if (addTmp(DatabaseConnector.getIDs("select distinct "
                + ret
                + " from "
                + tab
                + getWhere(search, ids, true)))) {
            getRelatedConnIDs(tab, search, ret, tmpConnIDs.keySet().toArray(new Integer[tmpConnIDs.size()]), depth++);
        }
        return;
    }

    private static IDFound getRoutingIdType(String[] sIDs) throws SQLException {
        Integer[] tmpIDs = DatabaseConnector.getRefIDs(ReferenceType.ConnID, sIDs);
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.ConnID, tmpIDs[0]);
        }

        tmpIDs = DatabaseConnector.getRefIDs(ReferenceType.UUID, sIDs);
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.UUID, tmpIDs[0]);
        }

        tmpIDs = DatabaseConnector.getRefIDs(ReferenceType.ORSSID, sIDs);
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.ORSSID, tmpIDs[0]);
        }

        tmpIDs = DatabaseConnector.getRefIDs(ReferenceType.IxnID, sIDs);

        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.IxnID, tmpIDs[0]);
        }

        return null;
    }

    public static SearchIDs getRoutingIDs(String[] ids) {
        return null;
    }

    static private void getIDsByConnID(SearchIDs si, Integer[] ids) throws SQLException {
        if (ids != null) {
            si.AddIDs(IDType.UUID, getIDs(IDType.UUID, IDType.ConnID, ids));
            si.AddIDs(IDType.ORSSID, getIDs(IDType.ORSSID, IDType.UUID, si.getIDs(IDType.UUID)));
        }
    }

    public static SearchIDs getIxnIDs(String[] ids) throws SQLException {
        SearchIDs ret = new SearchIDs();

        IDFound idFound = getRoutingIdType(ids);
        if (idFound == null || (idFound.idType == IDType.UNKNOWN)) {
            return null;
        }

        ret.AddIDs(idFound.idType, new Integer[]{idFound.id});

        switch (idFound.idType) {
            case ConnID: {
                ret.AddIDs(idFound.idType, getCallIDs(ret.getIDs(IDType.ConnID)));
                getIDsByConnID(ret, ret.getIDs(IDType.ConnID));
                break;
            }

            case ORSSID: {
                ret.AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.ORSSID, ret.getIDs(IDType.ORSSID))); // ConnID in URS log
                ret.AddIDs(IDType.UUID, getIDs(IDType.UUID, IDType.ORSSID, ret.getIDs(IDType.ORSSID))); // for voice calls, we need UUID instead of ConnID to get session
                if (ret.getIDs(IDType.ConnID) != null) {
                    ret.AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.UUID, ret.getIDs(IDType.UUID))); // for voice calls there will be 2 ConnIDs
                }
                if (ret.getIDs(IDType.ConnID) != null) {
                    ret.AddIDs(IDType.ConnID, getCallIDs(ret.getIDs(IDType.ConnID)));
                    getIDsByConnID(ret, ret.getIDs(IDType.ConnID));
                }
                ret.AddIDs(IDType.IxnID, getIDs(IDType.IxnID, IDType.ORSSID, ret.getIDs(IDType.ORSSID)));
                break;
            }

            case UUID: {
                ret.AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.UUID, ret.getIDs(IDType.UUID))); // for voice calls there will be 2 ConnIDs
                if (ret.getIDs(IDType.ConnID) != null) {
                    ret.AddIDs(IDType.ConnID, getCallIDs(ret.getIDs(IDType.ConnID)));
                    getIDsByConnID(ret, ret.getIDs(IDType.ConnID));
                }
                ret.AddIDs(IDType.IxnID, getIDs(IDType.IxnID, IDType.ORSSID, ret.getIDs(IDType.ORSSID)));
                break;
            }

            case IxnID: {
                ret.AddIDs(idFound.idType, getIxnIDs(ret.getIDs(IDType.IxnID)));
                ret.AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.IxnID, ret.getIDs(IDType.IxnID)));
                ret.AddIDs(IDType.ORSSID, getIDs(IDType.ORSSID, IDType.IxnID, ret.getIDs(IDType.IxnID)));
                break;
            }

            case UNKNOWN:
            default:
                throw new SQLException("Not implemented");
//                return null;
        }
        return ret;
    }

    /*Returns array of TLibrary ConnIDs */
    public static SearchIDs getOutboundIDs(Object owner, String[] ids) throws SQLException {
        SearchIDs ret = new SearchIDs();

        IDFound idFound = getOutboundIDType(owner, ids);

        if (idFound == null || idFound.idType == IDType.UNKNOWN) {
            return null;
        }
        ret.AddIDs(idFound.idType, new Integer[]{idFound.id});

        switch (idFound.idType) {
            case OCSChainID: {
                Integer[] chainIDs = new Integer[]{idFound.id};
                ret.AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.OCSChainID, chainIDs));
                ret.AddIDs(IDType.OCSRecordHandle, getIDs(IDType.OCSRecordHandle, IDType.OCSChainID, chainIDs));
                ret.AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSChainID, chainIDs));
                ret.AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSRecordHandle, ret.getIDs(IDType.OCSRecordHandle)));
                break;
            }

            case OCSRecordHandle: {
                Integer[] HIDs = new Integer[]{Integer.parseInt(ids[0])};
                ret.AddIDs(IDType.OCSChainID, getIDs(IDType.OCSChainID, IDType.OCSRecordHandle, HIDs));
                ret.AddIDs(IDType.OCSRecordHandle, getIDs(IDType.OCSRecordHandle, IDType.OCSChainID, ret.getIDs(IDType.OCSChainID)));
                ret.AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.OCSChainID, ret.getIDs(IDType.OCSChainID)));
                ret.AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSRecordHandle, ret.getIDs(IDType.OCSRecordHandle)));
                ret.AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSChainID, ret.getIDs(IDType.OCSChainID)));
                break;
            }

            case ConnID: {
                Integer[] chainIDs = getIDs(IDType.OCSChainID, IDType.ConnID, new Integer[]{idFound.id});
                ret.AddIDs(IDType.OCSChainID, chainIDs);
                ret.AddIDs(IDType.OCSRecordHandle, getIDs(IDType.OCSRecordHandle, IDType.OCSChainID, chainIDs));
                ret.AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.OCSChainID, chainIDs));
                ret.AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSChainID, ret.getIDs(IDType.OCSChainID)));
                ret.AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSRecordHandle, ret.getIDs(IDType.OCSRecordHandle)));
                break;
            }

            default:
                throw new SQLException("Not implemented search by type " + idFound.idType);
        }
        return ret;
    }

    static Integer[] getIDs(IDType retIDType, IDType searchIDType, Integer[] searchIDs) throws SQLException {
        if (searchIDs == null) {
            return null;
        }
        switch (retIDType) {
            case IxnID:
                switch (searchIDType) {
                    case ORSSID:
                        return DatabaseConnector.getIDs(
                                "select distinct ixnid "
                                        + "from orssessixn "
                                        + getWhere("sidid",
                                        searchIDs, true));

                }
                break;

            case OCSSID:
                switch (searchIDType) {
                    case OCSRecordHandle:
                        return DatabaseConnector.getIDs("ocsscxmltr", "SessIDID", getWhere("recHandle", searchIDs, true));

                    case OCSChainID:
                        return DatabaseConnector.getIDs("ocsscxmltr", "SessIDID", getWhere("chID", searchIDs, true));

                    default:
                        throw new SQLException("Not implemented: retIDType: " + retIDType + " searchIDType: " + searchIDType);

                }

            case ConnID:
                switch (searchIDType) {
                    case OCSRecordHandle:
                        return DatabaseConnector.getIDs("ocs_logbr", "ConnectionIDID", getWhere("recHandle", searchIDs, true));

                    case OCSChainID:
                        return DatabaseConnector.getIDs("ocs_logbr", "ConnectionIDID", getWhere("chID", searchIDs, true));

                    case ORSSID: {
                        return DatabaseConnector.getIDs("ursconnidsid", "connidid", getWhere("sidid", searchIDs, true));
                    }

                    case UUID: {
                        Integer[] tmpIDs1 = DatabaseConnector.getIDs("ors_logbr", "connectionidid", getWhere("uuidid", searchIDs, true));
                        Integer[] tmpIDs2 = DatabaseConnector.getIDs("urs_logbr", "connectionidid", getWhere("uuidid", searchIDs, true));
                        return QueryTools.uniqueInts(tmpIDs1, tmpIDs2);
                    }
                    case IxnID: {
                        Integer[] tmpIDs1 = DatabaseConnector.getIDs("ursconnidixnid", "connid", getWhere("ixnid", searchIDs, true));
                        Integer[] tmpIDs2 = DatabaseConnector.getIDs("urs_logbr", "connectionidid", getWhere("ixnidid", searchIDs, true));
                        return QueryTools.uniqueInts(tmpIDs1, tmpIDs2);
                    }
                }
                break;

            case OCSRecordHandle:
                switch (searchIDType) {
                    case OCSChainID:
                        Integer[] tmpIDs1 = DatabaseConnector.getIDs("ocs_logbr", "recHandle", getWhere("chID", searchIDs, true));
                        Integer[] tmpIDs2 = DatabaseConnector.getIDs("ocsreccr", "recHandle", getWhere("chID", searchIDs, true));
                        return QueryTools.uniqueInts(tmpIDs1, tmpIDs2);

                }
                break;

            case OCSChainID:
                switch (searchIDType) {
                    case OCSRecordHandle:
                        Integer[] tmpIDs = DatabaseConnector.getIDs("ocs_logbr", "chID", getWhere("recHandle", searchIDs, true));
                        if (tmpIDs != null && tmpIDs.length > 0) {
                            return tmpIDs;
                        }
                        tmpIDs = DatabaseConnector.getIDs("ocsreccr", "chID", getWhere("recHandle", searchIDs, true));
                        if (tmpIDs != null && tmpIDs.length > 0) {
                            return tmpIDs;
                        }
                        return null;

                    case ConnID: {
                        return DatabaseConnector.getIDs("ocs_logbr", "recHandle", getWhere("connectionidid", searchIDs, true));
                    }

                }
                break;

            case UUID:
                switch (searchIDType) {
                    case ConnID:
                        Integer[] tmpIDs1 = DatabaseConnector.getIDs("ors_logbr", "uuidid", getWhere("connectionidid", searchIDs, true));
                        Integer[] tmpIDs2 = DatabaseConnector.getIDs("urs_logbr", "uuidid", getWhere("connectionidid", searchIDs, true));
                        Integer[] ret = QueryTools.uniqueInts(tmpIDs1, tmpIDs2);
                        tmpIDs1 = DatabaseConnector.getIDs("tlib_logbr", "uuidid", getWhere("connectionidid", searchIDs, true));
                        ret = QueryTools.uniqueInts(ret, tmpIDs1);
                        tmpIDs1 = DatabaseConnector.getIDs("ocs_logbr", "uuidid", getWhere("connectionidid", searchIDs, true));
                        ret = QueryTools.uniqueInts(ret, tmpIDs1);
                        return ret;

                    case ORSSID:
                        return DatabaseConnector.getIDs("orssess_logbr", "uuidid", getWhere("sidid", searchIDs, true));
                }
                break;

            case ORSSID:
                switch (searchIDType) {
                    case UUID:
                        return DatabaseConnector.getIDs("orssess_logbr", "sidid", getWhere("uuidid", searchIDs, true));

                    case IxnID: {
                        return DatabaseConnector.getIDs("orssessixn", "sidid", getWhere("ixnid", searchIDs, true));

                    }

                }
                break;

//            case ORSSID:
//                switch (searchIDType) {
//                    case UUID:
//                        return DatabaseConnector.getIDs("orssess_logbr", "uuidid", getWhere("sidid", searchIDs, true));
//                }
            default:
        }
        throw new SQLException("Not implemented: retIDType: " + retIDType + " searchIDType: " + searchIDType);
    }

    private static IDFound getOutboundIDType(Object owner, String[] sIDs) throws SQLException {
        Integer[] tmpIDs = QueryTools.getRefIDs(owner, ReferenceType.ConnID, sIDs);
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.ConnID, tmpIDs[0]);
        }
        tmpIDs = DatabaseConnector.getDatabaseConnector(owner).getIDs(owner, "ocs_logbr", "ConnectionIDID", getWhere("recHandle", sIDs, true));
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.OCSRecordHandle, Integer.parseInt(sIDs[0]));
        }

        /* Is this recHandle ---> */
        tmpIDs = DatabaseConnector.getDatabaseConnector(owner).getIDs(owner, "ocspaagi_logbr", "id", getWhere("recHandle", sIDs, true));
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.OCSRecordHandle, Integer.parseInt(sIDs[0]));
        }

        tmpIDs = DatabaseConnector.getDatabaseConnector(owner).getIDs(owner, "ocspaevi_logbr", "id", getWhere("recHandle", sIDs, true));
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.OCSRecordHandle, Integer.parseInt(sIDs[0]));
        }

        tmpIDs = DatabaseConnector.getDatabaseConnector(owner).getIDs(owner, "ocsreccr", "id", getWhere("recHandle", sIDs, true));
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.OCSRecordHandle, Integer.parseInt(sIDs[0]));
        }
        /* <--- Is this recHandle --- */

        /* Is this ChainID ---> */
        tmpIDs = DatabaseConnector.getDatabaseConnector(owner).getIDs(owner, "ocs_logbr", "ConnectionIDID", getWhere("chID", sIDs, true));
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.OCSChainID, Integer.parseInt(sIDs[0]));
        }

        tmpIDs = DatabaseConnector.getDatabaseConnector(owner).getIDs(owner, "ocsreccr", "id", getWhere("chID", sIDs, true));
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.OCSChainID, Integer.parseInt(sIDs[0]));
        }
        /* <--- Is this ChainID */

        tmpIDs = QueryTools.getRefIDs(owner, ReferenceType.OCSSCXMLSESSION, sIDs);
        if (tmpIDs != null && tmpIDs.length > 0) {
            return new IDFound(IDType.OCSSID, tmpIDs[0]);
        }

        return null;
    }

    public Integer[] getIDs(IDType idType) {
        Integer[] ret = CallIDs.get(idType);
        if (ret != null && ret.length > 0) {
            return ret;
        }
        return null;
    }

    private void AddIDs(IDType idType, Integer[] IDs) {
        /*should extend list by ID type */
        if (IDs != null && IDs.length > 0) {
            if (!CallIDs.containsKey(idType)) {
                CallIDs.put(idType, IDs);
            } else {
                CallIDs.put(idType, uniqueInts(CallIDs.get(idType), IDs));
            }
        }
    }

    private void AddIDs(IDType idType, String[] ids) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    boolean idFound() {
        return !CallIDs.isEmpty();
    }

    static class IDFound {

        public IDType idType;
        public int id;

        public IDFound(IDType idType, int id) {
            this.idType = idType;
            this.id = id;
        }
    }

}

abstract public class QueryTools {

    private static final ArrayList<String> queryMessages = new ArrayList<String>();
    private static final int SPLIT_ON = 300;
    private static final HashMap<String, String> tableTypeByFileType = initTabTypes();
    private static final HashMap<String, FileInfoType> timeDiffByFileType = initTimeDiffTypes();
    protected boolean printAlone = false; // should print records as they are extracted. Used for file output
    protected PrintStreams ps = null;
    private boolean limitQueryResults;
    private int maxQueryLines;
    private IQueryResults.ProgressNotifications progressCallback;

    public static ArrayList<Integer> sortedArray(Integer[] ids) {
        ArrayList<Integer> idsh = new ArrayList<>(Arrays.asList(ids));
        Collections.sort(idsh);
        return idsh;
    }

    public static ArrayList<Long> sortedArray(Long[] ids) {
        ArrayList<Long> idsh = new ArrayList<>(Arrays.asList(ids));
        Collections.sort(idsh);
        return idsh;
    }

    public static void DebugIDs(Integer[] ids) {
        if (inquirer.logger.isDebugEnabled()) {
            if (ids == null || ids.length == 0) {
                inquirer.logger.debug("ids: empty");
            } else {
                StringBuilder s = new StringBuilder();
                s.append("ids: ");
                for (Integer callid : sortedArray(ids)) {
                    s.append(callid).append(",");
                }
                inquirer.logger.debug(s);
            }
        }
    }

    static void showQueryMessages(Window parent) {
        ArrayList<String> queryMessages = QueryTools.getqueryMessages();
        if (queryMessages != null && !queryMessages.isEmpty()) {
            JTextArea textArea = new MultilineLabel();
            textArea.setColumns(50);
            textArea.setRows(20);
            for (String queryMessage : queryMessages) {
                textArea.append(queryMessage + "\n");
            }

            inquirer.showInfoPanel(parent, "Info", new JScrollPane(textArea), true);
            queryMessages.clear();
        }
    }

    static String getIDString(String[] ids) {
        if (ids == null || ids.length == 0) {
            return ("<Empty>");
        } else {
            StringBuilder s = new StringBuilder();
            for (String callid : ids) {
                s.append(callid).append(",");
            }
            return s.toString();
        }

    }

    static public boolean queryMessagesAdd(String e) {
        return queryMessages.add(e);
    }

    static public void queryMessagesClear() {
        queryMessages.clear();
    }

    static public ArrayList<String> getqueryMessages() {
        return queryMessages;
    }

    public static boolean hasColumn(ResultSetMetaData rsmd, String columnName) throws SQLException {
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    static void DebugRec(ResultSet m_resultSet) {
        if (inquirer.logger.isTraceEnabled()) {
            try {
                ResultSetMetaData metadata = m_resultSet.getMetaData();
                StringBuilder s = new StringBuilder(256);
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    s.append(" [").append(metadata.getColumnName(i)).append("]: ").append(m_resultSet.getString(i));
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeInterruptException();
                } else {
                    inquirer.logger.trace(s.toString());
//                    Thread.currentThread().stop();
                }
            } catch (SQLException E) {
                inquirer.ExceptionHandler.handleException("\t error printing records\n", E);
            } catch (RuntimeInterruptException e) {
                System.out.println("e");
                inquirer.logger.error(e);
                throw e;
            }
        }
    }

    private static void DebugList(String s, Integer[] first) {
        if (inquirer.logger.isTraceEnabled()) {
            if (first == null) {
                inquirer.logger.trace("empty list");
            } else {
                StringBuilder buf = new StringBuilder(120);
                for (int i = 0; i < first.length; i++) {
                    Integer integer = first[i];
                    buf.append("[").append(i).append("] = '").append(integer).append("'");
                }
                inquirer.logger.trace(s + "cnt:" + first.length + " list items: " + buf);
            }
        }
    }

    private static HashMap<String, String> initTabTypes() {
        HashMap<String, String> ret = new HashMap<>();
        ret.put("TServer", "tlib_logbr");
        ret.put("RouterServer", "urs_logbr");
        ret.put("OrchestrationServer", "ors_logbr");
        ret.put("OCServer", "ocs_logbr");
        ret.put("InteractionServer", "Ixn");
        ret.put("StatServer", "ststevent_logbr");

        return ret;

    }

    static FileInfoType TimeDiffByFileType(String name) {
        return timeDiffByFileType.get(name);
    }

    private static HashMap<String, FileInfoType> initTimeDiffTypes() {
        HashMap<String, FileInfoType> ret = new HashMap<>();
        ret.put("TServer", FileInfoType.type_SessionController);
        ret.put("RouterServer", FileInfoType.type_URS);
        ret.put("OrchestrationServer", FileInfoType.type_ORS);
        ret.put("OCServer", FileInfoType.type_OCS);
        ret.put("InteractionServer", FileInfoType.type_IxnServer);
        ret.put("StatServer", FileInfoType.type_StatServer);
        ret.put("ApacheWeb", FileInfoType.type_ApacheWeb);
        ret.put("WWE", FileInfoType.type_WWE);

        return ret;
    }

    static String getLimitClause(boolean limitQueryResults, int maxQueryLines) {
        if (limitQueryResults) {
            return " LIMIT " + maxQueryLines + " ";
        }
        return "";
    }

    public static String getWhere(String Field, String[] ids) {
        return getWhere(Field, ids, false, false);
    }

    public static String getWhereRx(String Field, String[] ids, boolean addWhere) {
        return getWhere(Field, ids, addWhere, true);
    }

    public static String getWhere(String Field, String[] ids, boolean addWhere, boolean isRegex) {
        return IQuery.getWhere(Field, ids, addWhere, isRegex);
    }

    public static String getWhere(String Field, String[] ids, boolean addWhere) {
        return getWhere(Field, ids, addWhere, false);
    }

    public static String getWhere(String Field, ReferenceType rt, String[] ids, boolean addWhere, boolean isRegExp, boolean addNot) {
        if (ids != null && ids.length > 0) {
            StringBuilder ret = new StringBuilder(ids.length * 50 + Field.length() + 20);
            if (addWhere) {
                ret.append(" where ");
            }
            ret.append(" ").append(Field).append((addNot) ? " not " : "").append(" in ").append(getRefSubQuery(rt, ids, isRegExp));
            return ret.toString();
        }
        return "";
    }

    public static String getWhere(String Field, ReferenceType rt, String[] ids, boolean addWhere, boolean isRegExp) {
        return getWhere(Field, rt, ids, addWhere, isRegExp, false);
    }

    public static String getWhere(String Field, ReferenceType rt, String[] ids, boolean addWhere) {
        return getWhere(Field, rt, ids, addWhere, false);
    }

    /* last param - flag if comparison by start of literal */
    public static String getWhereLike(String Field, String[] ids, boolean addWhere) {
        StringBuilder ret = new StringBuilder(1024);

        if (ids != null && ids.length > 0) {
            if (addWhere) {
                ret.append(" where ");
            }
            boolean isLike = true;
            ret.append("(").append(Field).append((isLike ? " LIKE " : "=")).append("\"").append(IQuery.escapeString(ids[0], false))
                    .append((isLike ? "%" : "")).append("\"");
            for (int i = 1; i < ids.length; i++) {
                ret.append(" OR ").append(Field).append((isLike ? " LIKE " : "=")).append("\"")
                        .append(IQuery.escapeString(ids[i], false)).append((isLike ? "%" : "=")).append("\"");
            }
            ret.append(") ");
        }
        return ret.toString();
    }

    public static String TLibTableByFileType(String t) {
        return tableTypeByFileType.get(t);
    }

    public static String getWhere(String Field, String subQuery, boolean addWhere) {
        StringBuilder ret = new StringBuilder(128);
        if (addWhere) {
            ret.append(" where ");
        }
        ret.append(Field).append(" in (").append(subQuery).append(" )");
        return ret.toString();
    }

    public static String getWhere(String[] Fields, ArrayList<ArrayList<Long>> group, boolean addWhere) {
        StringBuilder ret = new StringBuilder(256);

        if (group != null && group.size() > 0 && Fields != null && Fields.length > 0) {
            if (addWhere) {
                ret.append(" where ");
            }
            ret.append("( ");
            for (int j = 0; j < group.size(); j++) {
                if (j > 0) {
                    ret.append(" or ");
                }
                ret.append("( ");
                for (int i = 0; i < Fields.length; i++) {
                    if (i > 0) {
                        ret.append(" and ");
                    }
                    ret.append(Fields[i]).append(" = ").append(group.get(j).get(i));
                }
                ret.append(" )");
            }
            ret.append(" )");
        }
        return ret.toString();
    }

    public static String getWhere(String[] Fields, IQuery.ANDOR fieldAndOr, Integer[] ids) {
        return IQuery.getWhere(Fields, fieldAndOr, ids);
    }

    public static String getWhere(String Field, Integer[] ids) {
        return IQuery.getWhere(Field, ids);
    }

    public static String getWhere(String Field, Collection<Long> ids, boolean addWhere) {
        if (ids != null) {
            return IQuery.getWhere(Field, ids.toArray(new Long[ids.size()]), addWhere, true);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static String getWhereNot(String Field, Collection<Long> ids, boolean addWhere) {
        return IQuery.getWhere(Field, ids.toArray(new Long[ids.size()]), addWhere, false);
    }

    public static String getWhere(String Field, Integer[] ids, boolean addWhere) {
        return IQuery.getWhere(Field, ids, addWhere);
    }

    public static String getWhere(String Field, Long[] ids, boolean addWhere) {
        return IQuery.getWhere(Field, ids, addWhere);
    }

    public static String appEqualWhere(String trigger_logbr, String handler_logbr) {
        StringBuilder s = new StringBuilder();
        s.append(trigger_logbr).append(".");
        return s.toString();
    }

    public static String getWhere(String Field, Integer[] ids, String addCond, boolean addWhere) {
        String ret = getWhere(Field, ids, addWhere);
        if (addCond != null && addCond.length() > 0) {
            return ret + " " + addCond;
        } else {
            return ret;
        }
    }

    public static String getWhere(String Field, Long[] ids, String addCond, boolean addWhere) {
        String ret = getWhere(Field, ids, addWhere);
        if (addCond != null && addCond.length() > 0) {
            return ret + " " + addCond;
        } else {
            return ret;
        }
    }

    public static String MakeQueryID(String retField, String queryTab, String Where) {
        return "SELECT DISTINCT " + retField + " FROM " + queryTab + " " + Where;
    }

    public static Integer[] getIDSubquery(Object obj, String tab, String retField, String subquery) throws SQLException {
        return DatabaseConnector.getDatabaseConnector(obj).getIDSubquery(obj, tab, retField, subquery);
    }

    public static Integer[] getRefIDs(Object owner, ReferenceType rt, String[] names) throws SQLException {
        return getRefIDs(rt, names, false);
    }

    public static Integer[] getRefIDs(ReferenceType rt, String[] names, boolean isRegEx) throws SQLException {
        if (DatabaseConnector.refTableExist(rt)) {
            String wh;
            if (!isRegEx) {
                wh = getWhere("name", names, true);
            } else {
                wh = getWhereRx("name", names, true);
            }
            return DatabaseConnector.getIDs(rt.toString(), "id", wh);
        }
        return new Integer[0];
    }

    public static String[] getRefNames(Object owner, ReferenceType rt) throws SQLException {
        return getRefNames(owner, rt, null, null);
    }

    public static ArrayList<NameID> getRefNamesNameID(Object owner, ReferenceType rt) throws SQLException {
        return QueryTools.getRefNamesNameID(owner, rt, null, null);
    }

    public static ArrayList<NameID> getRefNamesNameID(Object owner, ReferenceType rt, String CheckTab, String CheckIDField) throws SQLException {
        return getRefNamesNameID(owner, rt, CheckTab, CheckIDField, null);
    }

    public static String[] getRefNames(Object owner, ReferenceType rt, String CheckTab, String CheckIDField) throws SQLException {
        return getRefNames(owner, rt, CheckTab, CheckIDField, null);
    }

    public static String[] getRefNames(Object owner, ReferenceType rt, String CheckTab, String CheckIDField, String CheckIDField1) throws SQLException {
        if (DatabaseConnector.refTableExist(rt) && (CheckTab == null || DatabaseConnector.TableExist(CheckTab))) {
            return DatabaseConnector.getRefNames(owner, rt.toString(), CheckTab, CheckIDField, CheckIDField1);
        } else {
            return new String[0];
        }
    }

    public static ArrayList<NameID> getRefNamesNameID(ReferenceType rt, String CheckTab, String CheckIDField) throws SQLException {
        return getRefNamesNameID(null, rt, CheckTab, CheckIDField);
    }

    public static ArrayList<NameID> getRefNamesNameID(Object owner, ReferenceType rt, String CheckTab, String CheckIDField, String CheckIDField1) throws SQLException {
        if (DatabaseConnector.refTableExist(rt) && (CheckTab == null || DatabaseConnector.TableExist(CheckTab))) {
            return DatabaseConnector.getRefNamesNameID(owner, rt.toString(), CheckTab, CheckIDField, CheckIDField1);
        } else {
            return null;
        }
    }

    public static ArrayList<NameID> getRefNameIDs(Object owner, ReferenceType rt, String subQuery, boolean addOrderBy) throws SQLException {
        if (DatabaseConnector.refTableExist(rt)) {
            return DatabaseConnector.getRefNameIDs(owner, rt.toString(), subQuery, addOrderBy);
        } else {
            return null;
        }
    }

    public static ArrayList<NameID> getRefNameIDs(Object owner, ReferenceType rt, String subQuery) throws SQLException {
        return getRefNameIDs(owner, rt, subQuery, false);
    }

    public static ArrayList<NameID> getRefNameIDs(Object owner, ReferenceType rt) throws SQLException {
        return getRefNameIDs(owner, rt, null);
    }

    public static Integer[] getRefIDsLike(Object owner, ReferenceType rt, String[] names) throws SQLException {
        if (DatabaseConnector.refTableExist(rt)) {
            return DatabaseConnector.getDatabaseConnector(owner).getIDs(owner, rt.toString(), "id", getWhereLike("name", names, true));
        }
        return new Integer[0];
    }

    public static Integer[] getIDs(Object obj, String tab, String retField, String where) throws SQLException {
        return DatabaseConnector.getDatabaseConnector(obj).getIDs(obj, tab, retField, where);
    }

    public static String getRefSubQuery(ReferenceType rt, String[] names) {
        return " ( select id from " + rt.toString() + getWhere("name", names, true) + " ) ";
    }

    public static String getRefSubQuery(ReferenceType rt, String[] names, boolean isRegExp) {
        return " ( select id from " + rt.toString() + getWhere("name", names, true, isRegExp) + " ) ";
    }

    public static String getRefSubQuery(Object owner, ReferenceType rt, ArrayList<String> names) {
        return " ( select id from " + rt.toString() + getWhere("name", names.toArray((new String[0])), true) + " ) ";
    }

    public static String getRefSubQuery(ReferenceType rt, ArrayList<String> names) {
        return " ( select id from " + rt.toString() + getWhere("name", names.toArray((new String[0])), true) + " ) ";
    }

    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static Integer[] uniqueInts(Integer[] first, Integer[] second) {

        Set<Integer> set = new HashSet<>();
        if (first != null && first.length > 0) {
            Collections.addAll(set, first);
            DebugList("first", first);
        }
        if (second != null && second.length > 0) {
            Collections.addAll(set, second);
            DebugList("second", second);
        }
        DebugList("result", set.toArray(new Integer[set.size()]));

        return set.toArray(new Integer[set.size()]);
    }

    public static Integer[] uniqueInts(Integer[] first, Integer[] second, Integer[] third) {

        Set<Integer> set = new HashSet<>();
        if (first != null && first.length > 0) {
            DebugList("first", first);
            Collections.addAll(set, first);
        }
        if (second != null && second.length > 0) {
            DebugList("second", second);
            Collections.addAll(set, second);
        }

        if (third != null && third.length > 0) {
            DebugList("third", third);
            Collections.addAll(set, third);
        }
        DebugList("result", set.toArray(new Integer[set.size()]));

        return set.toArray(new Integer[set.size()]);
    }

    protected static boolean isChecked(DynamicTreeNode<OptionNode> node) {
        return node != null && ((OptionNode) node.getData()).isChecked();
    }

    protected static DynamicTreeNode<OptionNode> FindNode(DynamicTreeNode<OptionNode> root, String rootCh, String rootChCh, String rootChChCh) {
        if (root != null && rootCh != null) {
            DynamicTreeNode<OptionNode> Ch = getChildByName(root, rootCh);
            if (Ch != null && ((OptionNode) Ch.getData()).isChecked()) {
                if (rootChCh == null) {
                    return Ch;
                } else {
                    DynamicTreeNode<OptionNode> ChCh = getChildByName(Ch, rootChCh);

                    if (ChCh != null && ((OptionNode) ChCh.getData()).isChecked()) {
                        if (rootChChCh == null) {
                            return ChCh;
                        } else {
                            DynamicTreeNode<OptionNode> ChChCh = getChildByName(ChCh, rootChChCh);

                            if (ChChCh != null && ((OptionNode) ChChCh.getData()).isChecked()) {
                                return ChChCh;
                            }

                        }
                    }
                }
            }
        }
        return null;
    }

    protected static DynamicTreeNode<OptionNode> FindNode(DynamicTreeNode<OptionNode> root, DialogItem rootCh, DialogItem rootChCh, DialogItem rootChChCh, boolean ignoreChecked) {
        if (root != null && rootCh != null) {
            DynamicTreeNode<OptionNode> Ch = getChildByName(root, rootCh);
            if (Ch != null && (ignoreChecked || ((OptionNode) Ch.getData()).isChecked())) {
                if (rootChCh == null) {
                    return Ch;
                } else {
                    DynamicTreeNode<OptionNode> ChCh = getChildByName(Ch, rootChCh);

                    if (ChCh != null && (ignoreChecked || ((OptionNode) ChCh.getData()).isChecked())) {
                        if (rootChChCh == null) {
                            return ChCh;
                        } else {
                            DynamicTreeNode<OptionNode> ChChCh = getChildByName(ChCh, rootChChCh);

                            if (ChChCh != null && (ignoreChecked || ((OptionNode) ChChCh.getData()).isChecked())) {
                                return ChChCh;
                            }

                        }
                    }
                }
            }
        }
        return null;
    }

    protected static DynamicTreeNode<OptionNode> FindNode(DynamicTreeNode<OptionNode> root, DialogItem rootCh, DialogItem rootChCh, DialogItem rootChChCh) {
        return FindNode(root, rootCh, rootChCh, rootChChCh, false);
    }

    protected static DynamicTreeNode<OptionNode> getChildByName(DynamicTreeNode<OptionNode> parent, String reportName) {
        for (DynamicTreeNode<OptionNode> ch : parent.getChildren()) {
            if (((OptionNode) ch.getData()).getName().equals(reportName)) {
                return ch;
            }
        }
        return null;
    }

    protected static DynamicTreeNode<OptionNode> getChildByName(DynamicTreeNode<OptionNode> parent, DialogItem reportName) {
        if (parent != null && reportName != null) {
            for (DynamicTreeNode<OptionNode> ch : parent.getChildren()) {
                if (((OptionNode) ch.getData()).getDialogItem() == reportName) {
                    return ch;
                }
            }
        }
        return null;
    }

    public boolean isLimitQueryResults() {
        return limitQueryResults;
    }

    public int getMaxQueryLines() {
        return maxQueryLines;
    }

    void setQueryLimit(boolean limitQueryResults, int maxQueryLines) {
        this.limitQueryResults = limitQueryResults;
        this.maxQueryLines = maxQueryLines;
    }

    public void setPrintAlone(boolean printAlone) {
        this.printAlone = printAlone;
    }

    public void setProgressCallback(IQueryResults.ProgressNotifications progressCallback) {
        this.progressCallback = progressCallback;
    }

    protected void tellProgress(String s) {
        inquirer.logger.debug("Progress: " + s);
        if (progressCallback != null) {
            progressCallback.sayProgress(s);
        }
    }

    int LowestID(HashMap m_callIdHash) {
        List sortedKeys = new ArrayList(m_callIdHash.keySet());
        Collections.sort(sortedKeys);
        return (Integer) sortedKeys.get(0);
    }

    public Integer[] getRecHandles(IxnIDs ConnIDs) throws SQLException {
        return DatabaseConnector.getDatabaseConnector(this).getIDs(this, "ocs_logbr", "recHandle", getWhere("ConnectionIDID", ConnIDs.ids, true));
    }

    boolean allChildrenChecked(DynamicTreeNode<OptionNode> node, DialogItem di) {
        return allChildrenChecked(FindNode(node, di, null, null));
    }

    boolean allChildrenChecked(DynamicTreeNode<OptionNode> node) {
        boolean ret = true;
        if (isChecked(node)) {
            return OptionNode.AllChildrenChecked(node);

        }
        return ret;
    }

    void setPrintStreams(PrintStreams ps) {
        this.ps = ps;
    }

}
