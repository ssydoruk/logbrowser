package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.sortedArray;

public final class IDsFinder extends QueryTools {

    private static final Matcher regCampName = Pattern.compile("^([^@]+)@").matcher("");
    private final HashSet<Integer> m_callIdHash = new HashSet<>();
    private final Integer[] peerIDs = null;
    //    boolean initOutbound() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    private final HashMap<IDType, Integer[]> CallIDs = new HashMap<>();
    private final HashSet<Integer> m_connIdHash = new HashSet<>();
    private final int MAX_DEPTH = 10;
    ArrayList<Integer> searchApps = null;
    UTCTimeRange timeRange = null;
    private Integer[] refIDs;
    private int queryLevel;
    private boolean maxLevelSet = false;
    private String selection;
    private boolean regex;
    private SelectionType selectionType;
    private boolean queriesRun = false;
    private Integer[] dnIDs = null;
    private Integer[] SIPdnIDs = null;
    private Integer[] peerIPs = null;
    private Integer[] agentIDs = null;
    private boolean idsFound = false;
    private SearchType searchType = SearchType.UNKNOWN;
    IDsFinder(QueryDialog dlg, SelectionType key, String searchID) throws SQLException {
        selection = searchID;
        regex = false;
        selectionType = key;
        searchApps = dlg.getSearchApps();
        timeRange = dlg.getTimeRange();
        setQueryLevel(dlg.getRequestLevel());
    }

    IDsFinder(QueryDialog dlg) throws SQLException {
        selection = dlg.getSelection();
        regex = dlg.isRegex();
        selectionType = dlg.getSelectionType();
        searchApps = dlg.getSearchApps();
        timeRange = dlg.getTimeRange();
        setQueryLevel(dlg.getRequestLevel());
    }

    public IDsFinder() {
    }

    private static int arrLen(Object[] arr) {
        if (arr != null) {
            return arr.length;
        } else {
            return 0;
        }
    }

    public int getQueryLevel() {
        return queryLevel;
    }

    private void setQueryLevel(RequestLevel selectedLevel) {
        switch (selectedLevel) {
            case Level0:
                queryLevel = 0;
                break;
            case Level1:
                queryLevel = 1;
                break;
            case Level2:
                queryLevel = 2;
                break;
            case Level3:
                queryLevel = 3;
                break;
            case Level4:
                queryLevel = 4;
                break;
            case Level5:
                queryLevel = 5;
                break;
            case Level6:
                queryLevel = 6;
                break;
            case Level7:
                queryLevel = 7;
                break;
            case Level8:
                queryLevel = 8;
                break;
            case LevelMax:
                queryLevel = MAX_DEPTH;
                maxLevelSet = true;
                break;
        }
    }

    //    private Object m_callIdHash;
    public String getSelection() {
        return selection;
    }

    public Integer[] getAgentIDs() {
        return agentIDs;
    }

    public Integer[] getSearchApps() {
        return searchApps.toArray(new Integer[searchApps.size()]);
    }

    public int getObjectsFound() {
        int ret = 0;
        if (!m_connIdHash.isEmpty()) {
            ret += m_connIdHash.size();
        }

        if (!m_callIdHash.isEmpty()) {
            ret += m_connIdHash.size();
        }

        ret += arrLen(dnIDs);
        ret += arrLen(SIPdnIDs);
        ret += arrLen(peerIPs);
        ret += arrLen(agentIDs);

        if (ret == 0) {
            for (Integer[] entry : CallIDs.values()) {
                ret += arrLen(entry);

            }
        }

        return ret;
    }

    public UTCTimeRange getTimeRange() throws SQLException {
        return timeRange;
    }

    private boolean getOutboundIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ConnID, getRefIDs(ReferenceType.ConnID, sIDs, regex));
            if (IDsFound(IDType.ConnID)) {
                return true;
            } else if (selectionType == SelectionType.CONNID) {
                return false;
            }
        }
        if (selectionType == SelectionType.RecordHandle || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.OCSRecordHandle, DatabaseConnector.getIDs("ocs_logbr", "recHandle", getWhere("recHandle", sIDs, true, regex)));
            if (IDsFound(IDType.OCSRecordHandle)) {
                return true;
            }

            /* Is this recHandle ---> */
            AddIDs(IDType.OCSRecordHandle, DatabaseConnector.getIDs("ocspaagi_logbr", "recHandle", getWhere("recHandle", sIDs, true, regex)));
            if (IDsFound(IDType.OCSRecordHandle)) {
                return true;
            }

            AddIDs(IDType.OCSRecordHandle, DatabaseConnector.getIDs("ocspaevi_logbr", "recHandle", getWhere("recHandle", sIDs, true, regex)));
            if (IDsFound(IDType.OCSRecordHandle)) {
                return true;
            }

            AddIDs(IDType.OCSRecordHandle, DatabaseConnector.getIDs("ocsreccr", "recHandle", getWhere("recHandle", sIDs, true, regex)));
            if (IDsFound(IDType.OCSRecordHandle)) {
                return true;
            } else if (selectionType == SelectionType.RecordHandle) {
                return false;
            }

            /* <--- Is this recHandle --- */
        }

        if (selectionType == SelectionType.ChainID || selectionType == SelectionType.GUESS_SELECTION) {
            /* Is this ChainID ---> */
            AddIDs(IDType.OCSChainID, DatabaseConnector.getIDs("ocs_logbr", "chID", getWhere("chID", sIDs, true, regex)));
            if (IDsFound(IDType.OCSChainID)) {
                return true;
            }

            AddIDs(IDType.OCSChainID, DatabaseConnector.getIDs("ocsreccr", "chID", getWhere("chID", sIDs, true, regex)));
            if (IDsFound(IDType.OCSChainID)) {
                return true;
            }
            if (selectionType == SelectionType.ChainID) {
                return false;
            }
            /* <--- Is this ChainID */
        }

        if (selectionType == SelectionType.OCSSID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.OCSSID, getRefIDs(ReferenceType.OCSSCXMLSESSION, sIDs, regex));
            if (IDsFound(IDType.OCSSID)) {
                return true;
            }

            if (selectionType == SelectionType.OCSSID) {
                return false;
            }
        }
        if (selectionType == SelectionType.CG || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.CG, DatabaseConnector.getIDs(ReferenceType.OCSCG.toString(), "id", getWhere("name", sIDs, true, regex)));

            if (IDsFound(IDType.CG)) {
                return true;
            }

            if (selectionType == SelectionType.CG) {
                return false;
            }
        }

        if (selectionType == SelectionType.AGENT || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] _refIDs = getRefIDs(ReferenceType.Agent, sIDs, regex);
            AddIDs(IDType.AGENT, _refIDs);

            if (_refIDs != null && _refIDs.length > 0) {
                AddIDs(IDType.DN, DatabaseConnector.getIDs("ocsstatev_logbr", "DNID", getWhere("agentNameID", _refIDs, true)));
                return true;
            }

            if (selectionType == SelectionType.AGENT) {
                return false;
            }
        }

        if (selectionType == SelectionType.DialedNumber || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.DN, getRefIDs(ReferenceType.DN, sIDs, regex));
            if (IDsFound(IDType.DN)) {
                return true;
            } else if (selectionType == SelectionType.DialedNumber) {
                return false;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        if (selectionType == SelectionType.NO_SELECTION) {
            return selectionType.toString();
        } else {
            return "[" + selection + "] t[" + selectionType + "] rx[" + regex + "]";
        }
    }

    private boolean getStatServerIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ConnID, getRefIDs(ReferenceType.ConnID, sIDs, regex));
            if (IDsFound(IDType.ConnID)) {
                return true;
            } else if (selectionType == SelectionType.CONNID) {
                return false;
            }
        }
        if (selectionType == SelectionType.DN || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.DN, getRefIDs(ReferenceType.DN, sIDs, regex));
            if (IDsFound(IDType.DN)) {
                return true;
            } else if (selectionType == SelectionType.DN) {
                return false;
            }
        }
        if (selectionType == SelectionType.AGENT || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.AGENT, getRefIDs(ReferenceType.Agent, sIDs, regex));
            if (IDsFound(IDType.AGENT)) {
                return true;
            } else if (selectionType == SelectionType.AGENT) {
                return false;
            }
        }
        if (selectionType == SelectionType.PLACE || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.PLACE, getRefIDs(ReferenceType.Place, sIDs, regex));
            if (IDsFound(IDType.PLACE)) {
                return true;
            } else if (selectionType == SelectionType.PLACE) {
                return false;
            }
        }

        return false;
    }

    private boolean getIxnServerIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ConnID, getRefIDs(ReferenceType.ConnID, sIDs, regex));
            if (IDsFound(IDType.ConnID)) {
                return true;
            } else if (selectionType == SelectionType.CONNID) {
                return false;
            }
        }

        if (selectionType == SelectionType.IXN || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.IxnID, getRefIDs(ReferenceType.IxnID, sIDs, regex));
            if (IDsFound(IDType.IxnID)) {
                return true;
            } else if (selectionType == SelectionType.IXN) {
                return false;
            }
            /* <--- Is this recHandle --- */
        }

        if (selectionType == SelectionType.QUEUE || selectionType == SelectionType.GUESS_SELECTION) {
            if (DatabaseConnector.TableExist(ReferenceType.DN.toString())) {
                AddIDs(IDType.IxnQueue, getRefIDs(ReferenceType.DN, sIDs, regex));
                if (IDsFound(IDType.IxnQueue)) {
                    return true;
                }

                if (selectionType == SelectionType.QUEUE) {
                    return false;
                }
            }
        }

        if (selectionType == SelectionType.AGENT || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] _refIDs = getRefIDs(ReferenceType.Agent, sIDs, regex);
            AddIDs(IDType.AGENT, _refIDs);

            if (_refIDs != null && _refIDs.length > 0) {
                AddIDs(IDType.DN, DatabaseConnector.getIDs("ocsstatev_logbr", "DNID", getWhere("agentNameID", _refIDs, true)));
                return true;
            }

            if (selectionType == SelectionType.AGENT) {
                return false;
            }
        }

        return false;
    }

    public Integer[] findIDs(IDType idType) {
        if (CallIDs.containsKey(idType)) {
            return CallIDs.get(idType);
        }
        return null;
    }

    public Integer[] getIDs(IDType idType) throws SQLException {
        if (CallIDs.containsKey(idType)) {
            return CallIDs.get(idType);
        } else {
            LoadType(idType);
            if (CallIDs.containsKey(idType)) {
                return CallIDs.get(idType);
            }
        }
        return null;
    }

    public boolean IDsFound(IDType idType) {
        if (!CallIDs.containsKey(idType)) {
            return false;
        }
        Integer[] get = CallIDs.get(idType);
        boolean ret = (get != null && get.length > 0);
        return ret;
    }

    private void ModifyIDs(IDType idType, Integer[] iDs) {
        if (iDs != null && iDs.length > 0) {
            if (CallIDs.containsKey(idType)) {
                Integer[] get = CallIDs.get(idType);
                CallIDs.put(idType, uniqueInts(get, iDs));
            } else {
                inquirer.logger.error("ModifyIDs: cannot modify " + idType + " with IDs: " + listIDs(iDs));
            }
        }
    }

    private void LoadOutboundType(IDType givenType, IDType theSearchType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadOutboundType givenType: " + givenType + " theSearchType: " + theSearchType + " level: " + level + " ids: " + listIDs(ids));

        Integer[] iDs = getIDs(theSearchType, givenType, ids);
        if (AddIDs(theSearchType, iDs)) {
            LoadOutboundType(theSearchType, findIDs(theSearchType), level);
        }
    }

    private void LoadWWEType(IDType givenType, IDType theSearchType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadWWEType givenType: " + givenType + " theSearchType: " + theSearchType + " level: " + level + " ids: " + listIDs(ids));

        Integer[] iDs = getIDs(theSearchType, givenType, ids);
        if (AddIDs(theSearchType, iDs)) {
            LoadWWEType(theSearchType, findIDs(theSearchType), level);
        }
    }

    private void LoadGMSType(IDType givenType, IDType theSearchType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadGMSType givenType: " + givenType + " theSearchType: " + theSearchType + " level: " + level + " ids: " + listIDs(ids));

        Integer[] iDs = getIDs(theSearchType, givenType, ids);
        if (AddIDs(theSearchType, iDs)) {
            LoadGMSType(theSearchType, findIDs(theSearchType), level);
        }
    }

    private void LoadOutboundType(IDType idGivenType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadOutboundType idGivenType[" + idGivenType + "] level[" + level + "]");
        if (level <= 0) {
            if (maxLevelSet) {
                inquirer.logger.error("Max level reached");
            }
            return;
        }
        level--;

        LoadOutboundType(idGivenType, IDType.OCSChainID, ids, level);
        LoadOutboundType(idGivenType, IDType.OCSRecordHandle, ids, level);
        LoadOutboundType(idGivenType, IDType.ConnID, ids, level);
        LoadOutboundType(idGivenType, IDType.OCSSID, ids, level);
        LoadOutboundType(idGivenType, IDType.CGDBID, ids, level);
        LoadOutboundType(idGivenType, IDType.OCSCampaign, ids, level);
        LoadOutboundType(idGivenType, IDType.OCSCampaignDBID, ids, level);
        LoadOutboundType(idGivenType, IDType.CG, ids, level);
        LoadOutboundType(idGivenType, IDType.AGENT, ids, level);
        LoadOutboundType(idGivenType, IDType.DN, ids, level);

//        inquirer.logger.debug("idType: " + idGivenType);
//        switch (idGivenType) {
//            case OCSChainID: {
//                AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.OCSChainID, ids));
//                AddIDs(IDType.OCSRecordHandle, getIDs(IDType.OCSRecordHandle, IDType.OCSChainID, ids));
//                AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSChainID, ids));
//                AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSRecordHandle, findIDs(IDType.OCSRecordHandle)));
//                break;
//            }
//
//            case OCSRecordHandle: {
//                AddIDs(IDType.OCSChainID, getIDs(IDType.OCSChainID, IDType.OCSRecordHandle, ids));
//                AddIDs(IDType.OCSRecordHandle, getIDs(IDType.OCSRecordHandle, IDType.OCSChainID, findIDs(IDType.OCSChainID)));
//                AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.OCSChainID, findIDs(IDType.OCSChainID)));
//                AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSRecordHandle, findIDs(IDType.OCSRecordHandle)));
//                AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSChainID, findIDs(IDType.OCSChainID)));
//                AddIDs(IDType.DN, getIDs(IDType.DN, IDType.ConnID, findIDs(IDType.ConnID)));
//                break;
//            }
//
//            case ConnID: {
//                Integer[] chainIDs = getIDs(IDType.OCSChainID, IDType.ConnID, ids);
//                AddIDs(IDType.OCSChainID, chainIDs);
//                AddIDs(IDType.OCSRecordHandle, getIDs(IDType.OCSRecordHandle, IDType.OCSChainID, chainIDs));
//                AddIDs(IDType.ConnID, getIDs(IDType.ConnID, IDType.OCSChainID, chainIDs));
//                AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSChainID, findIDs(IDType.OCSChainID)));
//                AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSRecordHandle, findIDs(IDType.OCSRecordHandle)));
//                AddIDs(IDType.OCSSID, getIDs(IDType.OCSSID, IDType.OCSRecordHandle, findIDs(IDType.OCSRecordHandle)));
//                break;
//            }
//
//            case CG: {
//                AddIDs(IDType.CGDBID, getIDs(IDType.CGDBID, IDType.CG, ids));
//            }
//
//            case CGDBID: {
//                break;
//            }
//            default:
//                throw new Exception("Not implemented search for type " + idType);
//        }
    }

    private void LoadGMSType(IDType idGivenType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadGMSType idGivenType[" + idGivenType + "] level[" + level + "]");
        if (level <= 0) {
            if (maxLevelSet) {
                inquirer.logger.error("Max level reached");
            }
            return;
        }
        level--;

        LoadGMSType(idGivenType, IDType.ConnID, ids, level);
        LoadGMSType(idGivenType, IDType.ORSSID, ids, level);
        LoadGMSType(idGivenType, IDType.GMSSESSION, ids, level);
    }

    private void LoadWWEType(IDType idGivenType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadWWEType idGivenType[" + idGivenType + "] level[" + level + "]");
        if (level <= 0) {
            if (maxLevelSet) {
                inquirer.logger.error("Max level reached");
            }
            return;
        }
        level--;

        LoadWWEType(idGivenType, IDType.ConnID, ids, level);
        LoadWWEType(idGivenType, IDType.UUID, ids, level);
        LoadWWEType(idGivenType, IDType.JSessionID, ids, level);
        LoadWWEType(idGivenType, IDType.AGENT, ids, level);
        LoadWWEType(idGivenType, IDType.IxnID, ids, level);
        LoadWWEType(idGivenType, IDType.GWS_DeviceID, ids, level);
        LoadWWEType(idGivenType, IDType.BrowserClientID, ids, level);
        LoadWWEType(idGivenType, IDType.WWEUserID, ids, level);
    }

    private void LoadType(IDType idType, Integer[] ids) throws SQLException {
        if (!queriesRun) {

            switch (searchType) {
                case MediaServer:
                    LoadMediaServerType(idType, ids, queryLevel);
                    break;

                case TSERVER_SIP:
                    LoadTServerType(idType, ids, queryLevel);
                    break;

                case GMS:
                    LoadGMSType(idType, ids, queryLevel);
                    break;

                case WWE:
                    LoadWWEType(idType, ids, queryLevel);
                    break;

                case ROUTING:
                    LoadRoutingType(idType, ids, queryLevel);
                    break;

                case OUTBOUND:
                    LoadOutboundType(idType, ids, queryLevel);
                    break;

                case INTERACTION:
                    LoadIxnType(idType, ids, queryLevel);
                    break;

                case STATSERVER:
                    LoadStatServerType(idType, ids, queryLevel);
                    break;

                default:
                    throw new AssertionError();
            }
            DebugIDs();
            queriesRun = true;
        }
    }

    private void LoadType(IDType idType) throws SQLException {
        for (IDType key : CallIDs.keySet()) {
            Integer[] ids = CallIDs.get(key);
            if (ids != null && ids.length > 0) {
                LoadType(key, ids);
                break;
            }
        }
    }

    boolean anythingSIPRelated() throws SQLException {
        if (inquirer.getCr().isNewTLibSearch()) {
            Integer[] ids = getIDs(IDType.SIPCallID);
            return ids != null && ids.length > 0;

        } else {
            Integer[] callIDs = getCallIDs();
            if (callIDs != null && callIDs.length > 0) {
                return true;
            }

            Integer[] sipdNs = getSIPDNs();
            return sipdNs != null && sipdNs.length > 0;
        }

    }

    boolean outboundCallFound() throws SQLException {
        for (IDType idType : new IDType[]{IDType.OCSRecordHandle, IDType.OCSChainID, IDType.ConnID}) {
            if (getIDs(idType) != null) {
                return true;
            }
        }
        return false;
    }

    boolean anythingTLibRelated() throws SQLException {
        Integer[] ids = getIDs(IDType.ConnID);
        if (ids != null && ids.length > 0) {
            return true;
        }
        ids = getIDs(IDType.UUID);
        return ids != null && ids.length > 0;
    }

    boolean anythingApacheWebRelated() throws SQLException {
        Integer[] ids = getIDs(IDType.JSessionID);
        return ids != null && ids.length > 0;
    }

    private boolean isNewCallRelated() throws SQLException {
        Integer[] ids = getIDs(IDType.ConnID);
        if (ids != null && ids.length > 0) {
            return true;
        }
        ids = getIDs(IDType.UUID);
        return ids != null && ids.length > 0;
    }

    boolean AnythingTLibRelated() throws SQLException {
        if (searchType == SearchType.TSERVER_SIP) {
            if (inquirer.getCr().isNewTLibSearch()) {
                return isNewCallRelated();
            } else {
                Integer[] ConnIDs = getConnIDs();
                if (ConnIDs != null && ConnIDs.length > 0) {
                    return true;
                }

                Integer[] DNs = getDNs();
                if (DNs != null && DNs.length > 0) {
                    return true;
                }

                return refIDs != null && refIDs.length > 0;
            }
        } else {
            return isNewCallRelated();
        }
    }

    public Integer[] getRefIDs() {
        return refIDs;
    }

    boolean ConnIDFound() {
        Integer[] ConnIDs = getConnIDs();
        return ConnIDs != null && ConnIDs.length > 0;
    }

    public boolean initStatServer() throws SQLException {
        searchType = SearchType.STATSERVER;

        return getStatServerIDType();
    }

    boolean AnythingStatServerRelated() {
        return IDsFound(IDType.ConnID) || IDsFound(IDType.DN) || IDsFound(IDType.PLACE) || IDsFound(IDType.AGENT);
    }

    boolean initIxn() throws SQLException {
        searchType = SearchType.INTERACTION;

        return getIxnServerIDType();
    }

    private boolean getApacheWebIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.JSESSIONID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.JSessionID, getRefIDs(ReferenceType.JSessionID, sIDs, regex));
            if (IDsFound(IDType.JSessionID)) {
                return true;
            } else if (selectionType == SelectionType.JSESSIONID) {
                return false;
            }
        }
        if (selectionType == SelectionType.UUID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.UUID, getRefIDs(ReferenceType.UUID, sIDs, regex));
            if (IDsFound(IDType.UUID)) {
                return true;
            } else if (selectionType == SelectionType.UUID) {
                return false;
            }
        }
        if (selectionType == SelectionType.IXN || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.IxnID, getRefIDs(ReferenceType.IxnID, sIDs, regex));
            if (IDsFound(IDType.IxnID)) {
                return true;
            } else if (selectionType == SelectionType.IXN) {
                return false;
            }
        }
        if (selectionType == SelectionType.GWS_DEVICEID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.GWS_DeviceID, getRefIDs(ReferenceType.GWSDeviceID, sIDs, regex));
            if (IDsFound(IDType.GWS_DeviceID)) {
                return true;
            } else if (selectionType == SelectionType.GWS_DEVICEID) {
                return false;
            }
        }

        return false;
    }

    private boolean getWWEIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ConnID, getRefIDs(ReferenceType.ConnID, sIDs, regex));
            if (IDsFound(IDType.ConnID)) {
                return true;
            } else if (selectionType == SelectionType.CONNID) {
                return false;
            }
        }

        if (selectionType == SelectionType.UUID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.UUID, getRefIDs(ReferenceType.UUID, sIDs, regex));
            if (IDsFound(IDType.UUID)) {
                return true;
            } else if (selectionType == SelectionType.UUID) {
                return false;
            }
        }

        if (selectionType == SelectionType.JSESSIONID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.JSessionID, getRefIDs(ReferenceType.JSessionID, sIDs, regex));
            if (IDsFound(IDType.JSessionID)) {
                return true;
            } else if (selectionType == SelectionType.JSESSIONID) {
                return false;
            }
        }
        if (selectionType == SelectionType.IXN || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.IxnID, getRefIDs(ReferenceType.IxnID, sIDs, regex));
            if (IDsFound(IDType.IxnID)) {
                return true;
            } else if (selectionType == SelectionType.IXN) {
                return false;
            }
        }
        if (selectionType == SelectionType.GWS_DEVICEID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.GWS_DeviceID, getRefIDs(ReferenceType.GWSDeviceID, sIDs, regex));
            if (IDsFound(IDType.GWS_DeviceID)) {
                return true;
            } else if (selectionType == SelectionType.GWS_DEVICEID) {
                return false;
            }
        }

        if (selectionType == SelectionType.GWS_USERID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.WWEUserID, getRefIDs(ReferenceType.WWEUserID, sIDs, regex));
            if (IDsFound(IDType.WWEUserID)) {
                return true;
            } else if (selectionType == SelectionType.GWS_USERID) {
                return false;
            }
        }

        return false;
    }

    private boolean getConfServIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.AGENT || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.AGENT, getRefIDs(ReferenceType.Agent, sIDs, regex));
            if (IDsFound(IDType.AGENT)) {
                return true;
            } else if (selectionType == SelectionType.AGENT) {
                return false;
            }
        }

        return false;
    }

    private boolean getGMSIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType != SelectionType.NO_SELECTION) {
            AddIDs(IDType.ANYPARAM, getRefIDs(ReferenceType.Misc, sIDs, regex));
            if (selectionType == SelectionType.ANYPARAM && !idsFound(IDType.ANYPARAM)) {
                return false;
            }
        }

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ConnID, getRefIDs(ReferenceType.ConnID, sIDs, regex));
            if (IDsFound(IDType.ConnID)) {
                return true;
            } else if (selectionType == SelectionType.CONNID && !idsFound(IDType.ANYPARAM)) {
                return false;
            }
        }
        if (selectionType == SelectionType.UUID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.UUID, getRefIDs(ReferenceType.UUID, sIDs, regex));
            if (IDsFound(IDType.UUID)) {
                return true;
            } else if (selectionType == SelectionType.UUID && !idsFound(IDType.ANYPARAM)) {
                return false;
            }
        }

        if (selectionType == SelectionType.IXN || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.IxnID, getRefIDs(ReferenceType.IxnID, sIDs, regex));
            if (IDsFound(IDType.IxnID)) {
                AddIDs(IDType.ANYPARAM, DatabaseConnector.translateIDs(ReferenceType.IxnID, getIDs(IDType.IxnID), ReferenceType.Misc));
                return true;
            } else if (selectionType == SelectionType.IXN && !idsFound(IDType.ANYPARAM)) {
                return false;
            }
        }

        if (selectionType == SelectionType.GMSSESSION || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.GMSSESSION, getRefIDs(ReferenceType.GMSService, sIDs, regex));

            if (IDsFound(IDType.GMSSESSION)) {
                return true;
            }
            if (selectionType == SelectionType.GMSSESSION && !idsFound(IDType.ANYPARAM)) {
                return false;
            }
        }

        if (selectionType == SelectionType.SESSION || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ORSSID, getRefIDs(ReferenceType.ORSSID, sIDs, regex));

            if (IDsFound(IDType.ORSSID)) {
                return true;
            }
            if (selectionType == SelectionType.SESSION && !idsFound(IDType.ANYPARAM)) {
                return false;
            }
        }
        return idsFound(IDType.ANYPARAM);
    }

    private boolean getMediaServerIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.CALLID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.SIPCallID, getRefIDs(ReferenceType.SIPCALLID, sIDs, regex));
            if (IDsFound(IDType.SIPCallID)) {
                return true;
            } else if (selectionType == SelectionType.CALLID) {
                return false;
            }
        }

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ConnID, getRefIDs(ReferenceType.ConnID, sIDs, regex));
            if (IDsFound(IDType.ConnID)) {
                return true;
            } else if (selectionType == SelectionType.CONNID) {
                return false;
            }
        }

        if (selectionType == SelectionType.UUID || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] refIDs = getRefIDs(ReferenceType.UUID, sIDs, regex);
            AddIDs(IDType.UUID, refIDs);

            if (IDsFound(IDType.UUID)) {
                return true;
            }

            if (selectionType == SelectionType.UUID) {
                return false;
            }
        }
        if (selectionType == SelectionType.MCP_CALLID || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] _refIDs = getRefIDs(ReferenceType.VXMLMCPCallID, sIDs, regex);
            AddIDs(IDType.MCPCallID, _refIDs);

            if (IDsFound(IDType.MCPCallID)) {
                return true;
            }

            if (selectionType == SelectionType.MCP_CALLID) {
                return false;
            }
        }

        return false;

    }

    private boolean getRoutingIDType() throws SQLException {
        String[] sIDs = {selection};

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            AddIDs(IDType.ConnID, getRefIDs(ReferenceType.ConnID, sIDs, regex));
            if (IDsFound(IDType.ConnID)) {
                return true;
            } else if (selectionType == SelectionType.CONNID) {
                return false;
            }
        }

        if (selectionType == SelectionType.DN || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] _refIDs = getRefIDs(ReferenceType.DN, sIDs, regex);
            AddIDs(IDType.DN, _refIDs);

            if (IDsFound(IDType.DN)) {
                return true;
            }

            if (selectionType == SelectionType.DN) {
                return false;
            }
        }

        if (selectionType == SelectionType.AGENT || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] _refIDs = getRefIDs(ReferenceType.Agent, sIDs, regex);
            AddIDs(IDType.AGENT, _refIDs);

            if (IDsFound(IDType.AGENT)) {
                return true;
            }

            if (selectionType == SelectionType.AGENT) {
                return false;
            }
        }

        if (selectionType == SelectionType.SESSION || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] _refIDs = getRefIDs(ReferenceType.ORSSID, sIDs, regex);
            AddIDs(IDType.ORSSID, _refIDs);

            if (IDsFound(IDType.ORSSID)) {
                return true;
            }

            if (selectionType == SelectionType.SESSION) {
                return false;
            }
        }

        if (selectionType == SelectionType.UUID || selectionType == SelectionType.IXN || selectionType == SelectionType.GUESS_SELECTION) {
            Integer[] _refIDs = getRefIDs(ReferenceType.UUID, sIDs, regex);
            AddIDs(IDType.UUID, _refIDs);
            _refIDs = getRefIDs(ReferenceType.IxnID, sIDs, regex);
            AddIDs(IDType.IxnID, _refIDs);

            if (IDsFound(IDType.UUID) || IDsFound(IDType.IxnID)) {
                return true;
            }

            if (selectionType == SelectionType.UUID || selectionType == SelectionType.IXN) {
                return false;
            }
        }

        return false;
    }

    /*
    IDType idType - type specified
    Integer[] ids - array of object IDS of type specified
     */
    private void LoadRoutingType(IDType idGivenType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadRoutingType idGivenType[" + idGivenType + "] level[" + level + "]");
        if (level <= 0) {
            if (maxLevelSet) {
                inquirer.logger.error("Max level reached");
            }
            return;
        }
        level--;

        LoadRoutingType(idGivenType, IDType.IxnID, ids, level);
        LoadRoutingType(idGivenType, IDType.ConnID, ids, level);
        LoadRoutingType(idGivenType, IDType.UUID, ids, level);
        LoadRoutingType(idGivenType, IDType.ORSCallID, ids, level);
        LoadRoutingType(idGivenType, IDType.ORSSID, ids, level);
        LoadRoutingType(idGivenType, IDType.AGENT, ids, level);
        LoadRoutingType(idGivenType, IDType.GMSSESSION, ids, level);
    }

    /*
    IDType idType - type specified
    Integer[] ids - array of object IDS of type specified
     */
    private void LoadTServerType(IDType idGivenType, Integer[] ids, int level) throws SQLException {
//        inquirer.logger.debug("idGivenType[" + idGivenType + "] level[" + level + "]");
        if (level <= 0) {
            if (maxLevelSet) {
                inquirer.logger.error("Max level reached");
            }
            return;
        }
        level--;

        LoadTServerType(idGivenType, IDType.ConnID, ids, level);
        LoadTServerType(idGivenType, IDType.UUID, ids, level);
        LoadTServerType(idGivenType, IDType.SIPCallID, ids, level);
        LoadTServerType(idGivenType, IDType.SIPID, ids, level);
        LoadTServerType(idGivenType, IDType.DN, ids, level);
        LoadTServerType(idGivenType, IDType.TLibRefID, ids, level);
        LoadTServerType(idGivenType, IDType.JSONID, ids, level);
        LoadTServerType(idGivenType, IDType.SIPHandlerID, ids, level);
        LoadTServerType(idGivenType, IDType.SIPTriggerID, ids, level);
        LoadTServerType(idGivenType, IDType.TLIBID, ids, level);
    }

    private void LoadMediaServerType(IDType idGivenType, Integer[] ids, int level) throws SQLException {
//        inquirer.logger.debug("idGivenType[" + idGivenType + "] level[" + level + "]");
        if (level <= 0) {
            if (maxLevelSet) {
                inquirer.logger.error("Max level reached");
            }
            return;
        }
        level--;

        LoadMediaServerType(idGivenType, IDType.ConnID, ids, level);
        LoadMediaServerType(idGivenType, IDType.UUID, ids, level);
        LoadMediaServerType(idGivenType, IDType.SIPCallID, ids, level);
        LoadMediaServerType(idGivenType, IDType.MCPCallID, ids, level);
    }

    private Integer[] getIDsWWE(IDType retIDType, IDType providedIDType, Integer[] searchIDs) throws SQLException {
        if (retIDType == providedIDType) {
            return null;
        }

        //gws is general user and so if we are searching, data from this user should be ignored
        Integer[] regAgentIDs = DatabaseConnector.getRefIDs(ReferenceType.Agent, new String[]{"gws"});

        switch (retIDType) {

            case ConnID: {
                switch (providedIDType) {
                    case UUID:
                        return (DatabaseConnector.TableExist("wwetevents")) ? DatabaseConnector.getIDs(
                                "wwetevents", "connectionIDID",
                                getWhere("UUIDID", searchIDs, true))
                                : null;

                }
                break;
            }

            case WWEUserID: {
                switch (providedIDType) {
                    case JSessionID:
                        return ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                TableType.WWEMessage.toString(), "UserIDID",
                                getWhere("JSessionIDID", searchIDs, true))
                                : null);

                }
                break;
            }

            case BrowserClientID: {
                switch (providedIDType) {
                    case JSessionID:
                        return QueryTools.uniqueInts(
                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
                                        TableType.ApacheWeb.toString(), "browserClientID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEMessage.toString(), "browserClientID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null)
                        );

                    case IxnID: {
                        return ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                TableType.WWEMessage.toString(), "browserClientID",
                                getWhere("IxnIDID", searchIDs, true))
                                : null);
                    }
                    case UUID: {
                        return ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                TableType.WWEMessage.toString(), "browserClientID",
                                getWhere("UUIDID", searchIDs, true))
                                : null);
                    }

                }
                break;

            }

            case JSessionID: {
                switch (providedIDType) {
                    case WWEUserID: {
                        return ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                TableType.WWEMessage.toString(), "JSessionIDID",
                                getWhere("UserIDID", searchIDs, true))
                                : null);
                    }

                    case BrowserClientID: {
                        return QueryTools.uniqueInts(
                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
                                        TableType.ApacheWeb.toString(), "JSessionIDID",
                                        getWhere("browserClientID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEMessage.toString(), "JSessionIDID",
                                        getWhere("browserClientID", searchIDs, true))
                                        : null)
                        );
                    }

                    case UUID:
//                                                return QueryTools.uniqueInts(
//                                DatabaseConnector.getIDs("ursconnidixnid", "ixnid", getWhere("connid", searchIDs, true)),
//                                DatabaseConnector.getIDs("URSSTR_logbr", "ixnidid", getWhere("connidid", searchIDs, true)),
//                                DatabaseConnector.getIDs("urs_logbr", "ixnidid", getWhere("connectionidid", searchIDs, true))
//                        );

                        return QueryTools.uniqueInts(
                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
                                        TableType.ApacheWeb.toString(), "JSessionIDID",
                                        getWhere("UUIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEBayeuxMessage.toString(), "JSessionIDID",
                                        getWhere("UUIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEMessage.toString(), "JSessionIDID",
                                        getWhere("UUIDID", searchIDs, true))
                                        : null)
                        );

                    case IxnID:

                        return QueryTools.uniqueInts(
                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
                                        TableType.ApacheWeb.toString(), "JSessionIDID",
                                        getWhere("IxnIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEBayeuxMessage.toString(), "JSessionIDID",
                                        new WhereBuilder()
                                                .addOpenBracket()
                                                .addCond(getWhere("IxnIDID", searchIDs, false))
                                                .addCond(ArrayUtils.isNotEmpty(regAgentIDs), "AND", IQuery.getWhereNot("UserNameID", regAgentIDs, false, false))
                                                .addCloseBracket()
                                                .build(true)
                                        //                                       getWhere("IxnIDID", searchIDs, true)

                                )
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEMessage.toString(), "JSessionIDID",
                                        new WhereBuilder()
                                                .addOpenBracket()
                                                .addCond(getWhere("IxnIDID", searchIDs, false))
                                                .addCond(ArrayUtils.isNotEmpty(regAgentIDs), "AND", IQuery.getWhereNot("UserNameID", regAgentIDs, false, false))
                                                .addCloseBracket()
                                                .build(true)
                                )
                                        : null)
                        );

//                    case GWS_DeviceID:
//                        return QueryTools.uniqueInts(
//                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
//                                TableType.ApacheWeb.toString(), "JSessionIDID",
//                                getWhere("DeviceIDID", searchIDs, true))
//                                : null),
//                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
//                                TableType.WWEBayeuxMessage.toString(), "JSessionIDID",
//                                getWhere("DeviceIDID", searchIDs, true))
//                                : null),
//                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
//                                TableType.WWEMessage.toString(), "JSessionIDID",
//                                getWhere("DeviceIDID", searchIDs, true))
//                                : null)
//                        );
//                    case AGENT:
//                        return QueryTools.uniqueInts(
//                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
//                                TableType.ApacheWeb.toString(), "JSessionIDID",
//                                getWhere("userNameID", searchIDs, true))
//                                : null),
//                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
//                                TableType.WWEBayeuxMessage.toString(), "JSessionIDID",
//                                getWhere("userNameID", searchIDs, true))
//                                : null),
//                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
//                                TableType.WWEMessage.toString(), "JSessionIDID",
//                                getWhere("userNameID", searchIDs, true))
//                                : null)
//                        );
                }
                break;
            }

            case IxnID: {
                switch (providedIDType) {
                    case BrowserClientID: {
                        return ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                TableType.WWEMessage.toString(), "IxnIDID",
                                getWhere("browserClientID", searchIDs, true))
                                : null);
                    }
                    case JSessionID:

                        return QueryTools.uniqueInts(
                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
                                        TableType.ApacheWeb.toString(), "IxnIDID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEBayeuxMessage.toString(), "IxnIDID",
                                        new WhereBuilder()
                                                .addOpenBracket()
                                                .addCond(getWhere("JSessionIDID", searchIDs, false))
                                                .addCond(ArrayUtils.isNotEmpty(regAgentIDs), "AND", IQuery.getWhereNot("UserNameID", regAgentIDs, false, false))
                                                .addCloseBracket()
                                                .build(true)
                                )
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEMessage.toString(), "IxnIDID",
                                        new WhereBuilder()
                                                .addOpenBracket()
                                                .addCond(getWhere("JSessionIDID", searchIDs, false))
                                                .addCond(ArrayUtils.isNotEmpty(regAgentIDs), "AND", IQuery.getWhereNot("UserNameID", regAgentIDs, false, false))
                                                .addCloseBracket()
                                                .build(true)
                                )
                                        : null)
                        );
                }
                break;
            }

//            case AGENT: {
//                switch (providedIDType) {
//                    case JSessionID:
//
//                        return QueryTools.uniqueInts(
//                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
//                                TableType.ApacheWeb.toString(), "userNameID",
//                                getWhere("JSessionIDID", searchIDs, true))
//                                : null),
//                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
//                                TableType.WWEBayeuxMessage.toString(), "userNameID",
//                                getWhere("JSessionIDID", searchIDs, true))
//                                : null),
//                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
//                                TableType.WWEMessage.toString(), "userNameID",
//                                getWhere("JSessionIDID", searchIDs, true))
//                                : null)
//                        );
//
//                }
//                break;
//            }
            case GWS_DeviceID: {
                switch (providedIDType) {
                    case JSessionID:

                        return QueryTools.uniqueInts(
                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
                                        TableType.ApacheWeb.toString(), "DeviceIDID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEBayeuxMessage.toString(), "DeviceIDID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEMessage.toString(), "DeviceIDID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null)
                        );

                }
                break;
            }

            case UUID: {
                switch (providedIDType) {
                    case BrowserClientID: {
                        return ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                TableType.WWEMessage.toString(), "UUIDID",
                                getWhere("browserClientID", searchIDs, true))
                                : null);
                    }

                    case ConnID:
                        return (DatabaseConnector.TableExist("wwetevents")) ? DatabaseConnector.getIDs(
                                "wwetevents", "UUIDID",
                                getWhere("connectionIDID", searchIDs, true))
                                : null;

                    case JSessionID:
                        return QueryTools.uniqueInts(
                                ((DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) ? DatabaseConnector.getIDs(
                                        TableType.ApacheWeb.toString(), "UUIDID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEBayeuxMessage.toString(), "UUIDID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null),
                                ((DatabaseConnector.TableExist(TableType.WWEMessage.toString())) ? DatabaseConnector.getIDs(
                                        TableType.WWEMessage.toString(), "UUIDID",
                                        getWhere("JSessionIDID", searchIDs, true))
                                        : null)
                        );
                }
                break;
            }

            default: {
            }
        }
        inquirer.logger.trace(
                "not implemented search of [" + retIDType + "] by " + providedIDType + " ids: " + listIDs(searchIDs));

        return null;
    }

    private Integer[] getIDsGMS(IDType retIDType, IDType providedIDType, Integer[] searchIDs) throws SQLException {
        if (retIDType == providedIDType) {
            return null;
        }

        switch (retIDType) {
            case GMSSESSION: {
                switch (providedIDType) {
                    case ORSSID:
                        Integer[] tmpIDs1 = DatabaseConnector.getIDs(
                                "gmsors", "GMSServiceID",
                                getWhere("ORSSessionID", searchIDs, true));
                        Integer[] tmpIDs2 = DatabaseConnector.getIDs(
                                "gmspost", "GMSServiceID",
                                getWhere("ORSSessionID", searchIDs, true));
                        return QueryTools.uniqueInts(tmpIDs1, tmpIDs2);

                    case ConnID:
                        return DatabaseConnector.getIDs(
                                "gmsors", "GMSServiceID",
                                getWhere("connIDID", searchIDs, true));
                }
                break;
            }

            case ORSSID: {
                switch (providedIDType) {
                    case GMSSESSION:
                        Integer[] tmpIDs1 = DatabaseConnector.getIDs(
                                "gmsors", "ORSSessionID",
                                getWhere("GMSServiceID", searchIDs, true));
                        Integer[] tmpIDs2 = DatabaseConnector.getIDs(
                                "gmspost", "ORSSessionID",
                                getWhere("GMSServiceID", searchIDs, true));
                        return QueryTools.uniqueInts(tmpIDs1, tmpIDs2);

                    case ConnID:
                        return DatabaseConnector.getIDs(
                                "gmsors", "ORSSessionID",
                                getWhere("connIDID", searchIDs, true));
                }
                break;
            }

            case ConnID: {
                switch (providedIDType) {
                    case ORSSID:
                        return (DatabaseConnector.TableExist("gmsors")) ? DatabaseConnector.getIDs(
                                "gmsors", "connIDID",
                                getWhere("ORSSessionID", searchIDs, true))
                                : null;

                    case GMSSESSION:
                        return (DatabaseConnector.TableExist("gmsors")) ? DatabaseConnector.getIDs(
                                "gmsors", "connIDID",
                                getWhere("GMSServiceID", searchIDs, true))
                                : null;
                }
                break;
            }

            default: {
            }
        }
        inquirer.logger.trace("not implemented search of [" + retIDType + "] by " + providedIDType + " ids: " + listIDs(searchIDs));
        return null;
    }

    private Integer[] getIDsOutbound(IDType retIDType, IDType providedIDType, Integer[] searchIDs) throws SQLException {
        if (retIDType == providedIDType) {
            return null;
        }

        switch (retIDType) {
            case OCSCampaignDBID: {
                switch (providedIDType) {
                    case CG:
                        return DatabaseConnector.getIDs("ocspasi_logbr", "CampaignDBID",
                                getWhere("cgnameid", searchIDs, true));

                }
            }
            break;

//<editor-fold defaultstate="collapsed" desc="OCSCampaign">
            case OCSCampaign: {
                switch (providedIDType) {
                    case CG:
                        String[] names = DatabaseConnector.getNames(this, ReferenceType.OCSCG.toString(), "name", getWhere("id", searchIDs, true));
                        if (names != null && names.length > 0) {
                            ArrayList<String> camp = new ArrayList<>(names.length);
                            for (String name : names) {
                                Matcher m;
                                if ((m = regCampName.reset(name)).find()) {
                                    camp.add(m.group(1));
                                } else {
                                    inquirer.logger.error("Incorrect campaign group name: [" + name + "]");
                                }
                            }
                            return DatabaseConnector.getIDs(ReferenceType.OCSCAMPAIGN.toString(), "id",
                                    getWhere("name", camp.toArray(new String[camp.size()]), true, false));
                        }
                }
                break;
            }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="CG">
            case CG: {
                switch (providedIDType) {
                    case OCSChainID:
                        break;

                    case CGDBID:
                        return (DatabaseConnector.TableExist("ocscg_logbr")) ? DatabaseConnector.getIDs(
                                "select distinct cgnameid "
                                        + "from ocscg_logbr "
                                        + getWhere("cgDBID",
                                        searchIDs, true))
                                : null;

                }
                break;
            }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="AGENT">
            case AGENT: {
                switch (providedIDType) {
                    case OCSChainID: {
                        return null; // there is agent name by record handle, no need to run it by chain
                    }

                    case OCSRecordHandle: {
                        return (DatabaseConnector.TableExist("ocspaagi_logbr")) ? DatabaseConnector.getIDs(
                                "select distinct agentNameID "
                                        + "from ocspaagi_logbr "
                                        + getWhere("recHandle",
                                        searchIDs, true))
                                : null;
                    }
                }
                break;
            }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="CGDBID">
            case CGDBID:
                switch (providedIDType) {
                    case CG:
                        return (DatabaseConnector.TableExist("ocspasi_logbr")) ? DatabaseConnector.getIDs(
                                "select distinct cgDBID "
                                        + "from ocspasi_logbr "
                                        + getWhere("cgNameID",
                                        searchIDs, true))
                                : null;

                    case OCSChainID: {
//                        return DatabaseConnector.getIDs(
//                                "select distinct cgDBID "
//                                + "from ocspaevi_logbr "
//                                + getWhere("cgNameID",
//                                        searchIDs, true));

                    }
                    case OCSRecordHandle: {
                        return (DatabaseConnector.TableExist("ocspaevi_logbr")) ? DatabaseConnector.getIDs(
                                "select distinct cgDBID "
                                        + "from ocspaevi_logbr "
                                        + getWhere("recHandle",
                                        searchIDs, true))
                                : null;

                    }

                }
                break;
//</editor-fold>

            case IxnID:
                switch (providedIDType) {
                    case ORSSID:
                        return (DatabaseConnector.TableExist("orssessixn")) ? DatabaseConnector.getIDs(
                                "select distinct ixnid "
                                        + "from orssessixn "
                                        + getWhere("sidid",
                                        searchIDs, true))
                                : null;

                }
                break;

            case OCSSID:
                switch (providedIDType) {
                    case OCSRecordHandle:
                        return DatabaseConnector.getIDs("ocsscxmltr", "SessIDID", getWhere("recHandle", searchIDs, true));

                    case OCSChainID:
                        return DatabaseConnector.getIDs("ocsscxmltr", "SessIDID", getWhere("chID", searchIDs, true));

                }
                break;

            case ConnID:
                switch (providedIDType) {
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

//<editor-fold defaultstate="collapsed" desc="OCSRecordHandle">
            case OCSRecordHandle:
                switch (providedIDType) {
                    case OCSChainID:
                        Integer[] tmpIDs1 = DatabaseConnector.getIDs("ocs_logbr", "recHandle", getWhere("chID", searchIDs, true));
                        Integer[] tmpIDs2 = DatabaseConnector.getIDs("ocsreccr", "recHandle", getWhere("chID", searchIDs, true));
                        return QueryTools.uniqueInts(tmpIDs1, tmpIDs2);

                    case ConnID: {
                        return DatabaseConnector.getIDs("ocs_logbr", "recHandle", "where " + getWhere("connectionIDID", searchIDs, false) + " or " + getWhere("TransferIDID", searchIDs, false));
                    }

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="DN">
            case DN:
                switch (providedIDType) {
                    case ConnID:
                        return DatabaseConnector.getIDs("ocs_logbr", "thisdnid", getWhere("connectionidid", searchIDs, true));

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="OCSChainID">
            case OCSChainID:
                switch (providedIDType) {
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
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="UUID">
            case UUID:
                switch (providedIDType) {
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
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="ORSSID">
            case ORSSID:
                switch (providedIDType) {
                    case UUID:
                        return DatabaseConnector.getIDs("orssess_logbr", "sidid", getWhere("uuidid", searchIDs, true));

                    case IxnID: {
                        return DatabaseConnector.getIDs("orssessixn", "sidid", getWhere("ixnid", searchIDs, true));

                    }

                }
                break;
//</editor-fold>

//            case ORSSID:
//                switch (searchIDType) {
//                    case UUID:
//                        return DatabaseConnector.getIDs("orssess_logbr", "uuidid", getWhere("sidid", searchIDs, true));
//                }
            default: {
            }
        }
        inquirer.logger.trace("not implemented search of [" + retIDType + "] by " + providedIDType + " ids: " + listIDs(searchIDs));
        return null;
    }

    private Integer[] getIDsMediaServer(IDType retIDType, IDType givenIDType, Integer[] searchIDs) throws SQLException {
        switch (retIDType) {

            case SIPCallID:
                switch (givenIDType) {
                    case MCPCallID: {
                        return DatabaseConnector.getIDs(TableType.VXMLIntStepsTable.toString(), "SIPCallID", getWhere("mcpCallIDID", searchIDs, true));
                    }

                    case UUID: {
                        return DatabaseConnector.getIDs("SIPMS", "callidid", getWhere("uuidID", searchIDs, true));
                    }

                }
                break;

            case MCPCallID:
                switch (givenIDType) {
                    case SIPCallID: {
                        return DatabaseConnector.getIDs(TableType.VXMLIntStepsTable.toString(), "mcpCallIDID", getWhere("SIPCallID", searchIDs, true));
                    }

                }
                break;

            case ConnID:
//                switch (givenIDType) {
//                    case ORSSID: {
//                        return QueryTools.uniqueInts(
//                                DatabaseConnector.getIDs("ursri", "connidid", getWhere("ORSSidID", searchIDs, true)),
//                                DatabaseConnector.getIDs("URSCONNIDSID", "connidid", getWhere("sidid", searchIDs, true))
//                        );
//                    }
//
//                }
                break;

            case UUID:
                switch (givenIDType) {
                    case SIPCallID: {
                        return DatabaseConnector.getIDs("SIPMS", "uuidID", getWhere("callidid", searchIDs, true));
                    }

                }
                break;

            default:
        }
        return null;
    }

    private Integer[] getIDsRouting(IDType retIDType, IDType givenIDType, Integer[] searchIDs) throws SQLException {
        switch (retIDType) {

            case AGENT:
                switch (givenIDType) {
                    case ConnID: {
                        String[] names = DatabaseConnector.getNames(this,
                                ReferenceType.URSStrategyRef.toString(),
                                "name", "where id in (select ref2id from URSSTR_logbr " + getWhere("connidid", searchIDs, true)
                                        + " and " + getWhere("ref1id", ReferenceType.URSStrategyRef, new String[]{"Agent"}, false)
                                        + ")");

//                        String[] names = DatabaseConnector.getNames(this, "URSSTR_logbr", "ref2id", getWhere("connidid", searchIDs, true));
                        Integer[] _refIDs = DatabaseConnector.getRefIDs(ReferenceType.Agent, names);
                        return DatabaseConnector.getRefIDs(ReferenceType.Agent, names);
                    }
                }
                break;

            case IxnID:
                switch (givenIDType) {
                    case ORSSID:
                        return DatabaseConnector.getIDs("orssessixn", "ixnid", getWhere("sidid", searchIDs, true));

                    case IxnID: {
                        return DatabaseConnector.getIDs("ORSMM", "PARENTIXNIDID", getWhere("IXNIDID", searchIDs, true));
                    }

                    case ConnID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ursconnidixnid", "ixnid", getWhere("connid", searchIDs, true)),
                                DatabaseConnector.getIDs("URSSTR_logbr", "ixnidid", getWhere("connidid", searchIDs, true)),
                                DatabaseConnector.getIDs("urs_logbr", "ixnidid", getWhere("connectionidid", searchIDs, true))
                        );
                    }
                    case UUID: {
                        return DatabaseConnector.getIDs("urs_logbr", "ixnidid", getWhere("uuidid", searchIDs, true));
                    }
                }
                break;

            case ConnID:
                switch (givenIDType) {
                    case ORSSID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ursri", "connidid", getWhere("ORSSidID", searchIDs, true)),
                                DatabaseConnector.getIDs("URSCONNIDSID", "connidid", getWhere("sidid", searchIDs, true))
                        );
                    }

                    case IxnID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ursconnidixnid", "connid", getWhere("ixnid", searchIDs, true)),
                                DatabaseConnector.getIDs("URSSTR_logbr", "connidid", getWhere("ixnidid", searchIDs, true)),
                                DatabaseConnector.getIDs("urs_logbr", "connectionidid", getWhere("ixnidid", searchIDs, true))
                        );
                    }

                    case UUID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ors_logbr", "connectionidid", getWhere("uuidid", searchIDs, true)),
                                DatabaseConnector.getIDs("urs_logbr", "connectionidid", getWhere("uuidid", searchIDs, true))
                        );
                    }

                    case ConnID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ors_logbr", "TransferIdID", getWhere("ConnectionIDID", searchIDs, true)),
                                DatabaseConnector.getIDs("urs_logbr", "TransferIdID", getWhere("ConnectionIDID", searchIDs, true))
                        );
                    }

                    case DN: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ors_logbr", "ConnectionIDID", "where (" + getWhere("ThisDNID", searchIDs, false) + " or " + getWhere("OtherDNID", searchIDs, false) + ")"),
                                DatabaseConnector.getIDs("urs_logbr", "ConnectionIDID", "where (" + getWhere("ThisDNID", searchIDs, false) + " or " + getWhere("OtherDNID", searchIDs, false) + ")")
                        );
                    }
                }
                break;

            case DN:
                switch (givenIDType) {
                    case ConnID:
                        return DatabaseConnector.getIDs("ocs_logbr", "thisdnid", getWhere("connectionidid", searchIDs, true));

                }
                break;

            case ORSCallID: {
                switch (givenIDType) {
                    case UUID: {
//                        return DatabaseConnector.getIDs(TableType.ORSCallID.toString(), "callID", getWhere("uuidid", searchIDs, true));
                    }
                }
                break;
            }

            case UUID:
                switch (givenIDType) {
                    case ORSCallID: {
//                        return DatabaseConnector.getIDs(TableType.ORSCallID.toString(), "uuidid", getWhere("callID", searchIDs, true));
                    break;
                    }
                   

                    case ConnID:
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ors_logbr", "uuidid", getWhere("connectionidid", searchIDs, true)),
                                DatabaseConnector.getIDs("urs_logbr", "uuidid", getWhere("connectionidid", searchIDs, true))
                        );

                    case ORSSID:
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ORSURS_logbr", "uuidid", getWhere("sidid", searchIDs, true)),
                                DatabaseConnector.getIDs("orssess_logbr", "uuidid", getWhere("sidid", searchIDs, true)),
                                DatabaseConnector.getIDs("URSRLIB", "uuidid", getWhere("sidid", searchIDs, true))
                        );

                    case IxnID:
                        return DatabaseConnector.getIDs("urs_logbr", "uuidid", getWhere("ixnidid", searchIDs, true));

                    case DN:
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ors_logbr", "uuidID", "where (" + getWhere("ThisDNID", searchIDs, false) + " or " + getWhere("OtherDNID", searchIDs, false) + ")"),
                                DatabaseConnector.getIDs("urs_logbr", "uuidID", "where (" + getWhere("ThisDNID", searchIDs, false) + " or " + getWhere("OtherDNID", searchIDs, false) + ")")
                        );

                }
                break;

            case GMSSESSION:
                switch (givenIDType) {
                    case ORSSID:
                        return (DatabaseConnector.TableExist("orshttp"))
                                ? DatabaseConnector.getIDs("orshttp", "GMSServiceID", getWhere("sidid", searchIDs, true))
                                : null;
                }
                break;

            case ORSSID:
                switch (givenIDType) {
                    case GMSSESSION:
                        return (DatabaseConnector.TableExist("orshttp"))
                                ? DatabaseConnector.getIDs("orshttp", "sidid", getWhere("GMSServiceID", searchIDs, true))
                                : null;

                    case UUID:
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ORSURS_logbr", "sidid", getWhere("uuidid", searchIDs, true)),
                                DatabaseConnector.getIDs("URSRLIB", "sidid", getWhere("uuidid", searchIDs, true)),
                                DatabaseConnector.getIDs("orssess_logbr", "sidid", getWhere("uuidid", searchIDs, true))
                        );

                    case IxnID: {
                        return DatabaseConnector.getIDs("orssessixn", "sidid", getWhere("ixnid", searchIDs, true));

                    }

                    case ConnID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ursri", "ORSSidID", getWhere("connidid", searchIDs, true)),
                                DatabaseConnector.getIDs("URSCONNIDSID", "sidid", getWhere("connidid", searchIDs, true))
                        );
                    }

                    case ORSSID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("orssidsid", "sidid", getWhere("newsidid", searchIDs, true)),
                                DatabaseConnector.getIDs("orssidsid", "newsidid", getWhere("sidid", searchIDs, true))
                        );

                    }
                }
                break;

            default:
        }
        return null;
    }

    private Integer[] getIDsTServer(IDType retIDType, IDType givenType, Integer[] givenIDs) throws SQLException {
        switch (retIDType) {
//<editor-fold defaultstate="collapsed" desc="ConnID">
            case ConnID:
                switch (givenType) {
                    case UUID: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("tlib_logbr", "connectionidid", getWhere("uuidid", givenIDs, true)),
                                DatabaseConnector.getIDs("tlib_logbr", "TransferIdID", getWhere("uuidid", givenIDs, true))
                        );
                    }

                    case ConnID: {
                        return QueryTools.uniqueInts(
                                QueryTools.uniqueInts(
                                        DatabaseConnector.getIDs("tlib_logbr", "TransferIdID", getWhere("ConnectionIDID", givenIDs, true)),
                                        DatabaseConnector.getIDs("tlib_logbr", "ConnectionIDID", getWhere("TransferIdID", givenIDs, true))),
                                QueryTools.uniqueInts(
                                        DatabaseConnector.getIDs("connid_logbr", "newConnIDID", getWhere("ConnectionIDID", givenIDs, true)),
                                        DatabaseConnector.getIDs("connid_logbr", "ConnectionIDID", getWhere("newConnIDID", givenIDs, true))),
                                QueryTools.uniqueInts(
                                        DatabaseConnector.getIDs("iscc_logbr", "otherConnectionIDID", getWhere("ConnectionIDID", givenIDs, true)),
                                        DatabaseConnector.getIDs("iscc_logbr", "ConnectionIDID", getWhere("otherConnectionIDID", givenIDs, true)))
                        );
                    }

                    case DN: {
                        return QueryTools.uniqueInts(
                                DatabaseConnector.getIDs("ors_logbr", "ConnectionIDID", "where (" + getWhere("ThisDNID", givenIDs, false) + " or " + getWhere("OtherDNID", givenIDs, false) + ")"),
                                DatabaseConnector.getIDs("urs_logbr", "ConnectionIDID", "where (" + getWhere("ThisDNID", givenIDs, false) + " or " + getWhere("OtherDNID", givenIDs, false) + ")")
                        );
                    }
                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="UUID">
            case UUID:
                switch (givenType) {
                    case SIPCallID:
                        return DatabaseConnector.getIDs("sip_logbr", "uuidid", getWhere("callidid", givenIDs, true));

//                    case SIPID:
//                        return DatabaseConnector.getIDs("sip_logbr", "uuidid", getWhere("id", givenIDs, true));
                    case SIPHandlerID:
                        return DatabaseConnector.getIDs("sip_logbr", "uuidid", getWhere("handlerid", givenIDs, true) + " and callRelated=1");

                    case ConnID:
                        return DatabaseConnector.getIDs("tlib_logbr", "uuidid", getWhere("connectionidid", givenIDs, true));
                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="SIPCallID">
            case SIPCallID:
                switch (givenType) {
                    case SIPHandlerID:
                        return DatabaseConnector.getIDs("sip_logbr", "callidid", getWhere("handlerid", givenIDs, true) + " and callRelated=1");

//                    case SIPTriggerID:
//                        return DatabaseConnector.getIDs("sip_logbr", "callidid", getWhere("handlerid", givenIDs, true) + " and callRelated=1");
                    case UUID:
                        return DatabaseConnector.getIDs("sip_logbr", "callidid", getWhere("uuidid", givenIDs, true) + " and callRelated=1");

                    case ConnID: {
                        String connIDWhere = IQuery.getWhere("tlib.connectionidid", givenIDs, true) + "\n";
                        TReqTabs q = new TReqTabs("SELECT distinct callidid from tlib_logbr" + " AS tlib \n"
                                + "INNER JOIN cireq_logbr" + " AS cireq "
                                + "ON cireq.nameid=tlib.nameid "
                                + "AND cireq.refid=tlib.referenceid "
                                + "AND cireq.thisdnid=tlib.thisdnid "
                                + "AND cireq.otherdnid=tlib.otherdnid \n"
                                + "INNER JOIN sip_logbr AS sip "
                                + "ON cireq.handlerid=sip.handlerid AND sip.handlerid>0 \n"
                                + "INNER JOIN FILE_logbr AS f1 ON tlib.fileId=f1.id \n"
                                + "INNER JOIN FILE_logbr AS f2 ON cireq.fileId=f2.id \n"
                                + "INNER JOIN FILE_logbr AS f3 ON sip.fileId=f3.id \n"
                                + connIDWhere
                                + "and f1.appnameid=f2.appnameid and f2.appnameid=f3.appnameid",
                                new String[]{"tlib_logbr", "sip_logbr", "cireq_logbr"});
                        if (q.allTabsExist()) {
                            return DatabaseConnector.getIDs(q.getReq());
                        }

                    }
                    break;

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="DN">
            case DN:
                switch (givenType) {

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="TLibRefID">
            case TLibRefID:
                switch (givenType) {

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="JSONID">
            case JSONID:
                switch (givenType) {
//                    case SIPID:
//                        return DatabaseConnector.getIDs("json_logbr", "id", getWhere("sipid", givenIDs, true));

                    case SIPHandlerID:
                        return DatabaseConnector.getIDs("json_logbr", "id", getWhere("handlerid", givenIDs, true));

                    case JSONID:
                        return DatabaseConnector.getIDs("select j1.id\n"
                                + "from json_logbr as j1\n"
                                + "INNER JOIN json_logbr AS j2 on j2.SipsReqId=j1.SipsReqId\n"
                                + "INNER JOIN FILE_logbr AS f1 ON j1.fileId=f1.id \n"
                                + "INNER JOIN FILE_logbr AS f2 ON j2.fileId=f2.id \n"
                                + "where\n"
                                + "f1.appnameid=f2.appnameid\n"
                                + "and\n"
                                + getWhere("j2.id", givenIDs, false)
                        );

                    case SIPTriggerID:
                        return DatabaseConnector.getIDs("trigger_logbr", "jsonid",
                                getWhere("id", givenIDs, true) + " and jsonid >0"
                        );

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="SIPID">
            case SIPID:
                switch (givenType) {
                    case SIPCallID:
                        return DatabaseConnector.getIDs("sip_logbr", "id", getWhere("callidid", givenIDs, true) + " and callRelated=1");

                    case SIPTriggerID:
                        return DatabaseConnector.getIDs("trigger_logbr", "sipid", getWhere("id", givenIDs, true) + " and sipid>0");

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="TLIBID">
            case TLIBID:
                switch (givenType) {
                    case SIPHandlerID:
                        return DatabaseConnector.getIDs("tlib_logbr", "id",
                                getWhere("handlerid", givenIDs, true));

                    case ConnID:
                        Wheres wh = new Wheres();
                        wh.addWhere(getWhere("connectionidid", givenIDs, false));
                        wh.addWhere(getWhere("transferidid", givenIDs, false), "OR");
                        return DatabaseConnector.getIDs("tlib_logbr", "id",
                                wh.makeWhere(true));

                    case SIPTriggerID:
                        return DatabaseConnector.getIDs("trigger_logbr", "tlibid",
                                getWhere("id", givenIDs, true) + " and tlibid>0"
                        );

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="SIPHandlerID">
            case SIPHandlerID:
                switch (givenType) {
                    case ConnID:
                        return DatabaseConnector.getIDs("tlib_logbr", "handlerid",
                                "where (" + getWhere("connectionidid", givenIDs, false) + " or " + getWhere("transferIDid", givenIDs, false) + " and handlerid > 0" + ")");

                    case UUID:
                        return DatabaseConnector.getIDs("tlib_logbr", "handlerid",
                                getWhere("uuidid", givenIDs, true) + " and handlerid > 0");

                    case JSONID:
                        return DatabaseConnector.getIDs("json_logbr", "handlerid",
                                getWhere("id", givenIDs, true) + " and handlerid > 0");

                    case SIPCallID:
                        return DatabaseConnector.getIDs("sip_logbr", "handlerid",
                                getWhere("callidid", givenIDs, true) + " and callRelated=1" + " and handlerid > 0");

                    case SIPTriggerID:
                        break;
//                        return DatabaseConnector.getIDs("select handler.id\n"
//                                + "from handler_logbr AS handler \n"
//                                + "INNER JOIN trigger_logbr as trgr on trgr.textid=handler.textid\n"
//                                //                                + "INNER JOIN FILE_logbr AS f1 ON trgr.fileId=f1.id \n"
//                                //                                + "INNER JOIN FILE_logbr AS f2 ON handler.fileId=f2.id \n"
//                                + "where\n"
//                                //                                + "f1.appnameid=f2.appnameid\n"
//                                //                                + "and\n"
//                                + getWhere("trgr.id", givenIDs, false)
//                                + " and handlerid > 0"
//                        );
                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="SIPTriggerID">
            case SIPTriggerID:
                switch (givenType) {
                    case SIPHandlerID:
//                        if (DatabaseConnector.TableExist("trigger_logbr")) {
//                            return DatabaseConnector.getIDs("select trgr.id\n"
//                                    + "from trigger_logbr as trgr\n"
//                                    + "INNER JOIN handler_logbr AS handler on trgr.textid=handler.textid\n"
//                                    //                                    + "INNER JOIN FILE_logbr AS f1 ON trgr.fileId=f1.id \n"
//                                    //                                    + "INNER JOIN FILE_logbr AS f2 ON handler.fileId=f2.id \n"
//                                    + "where\n"
//                                    //                                    + "f1.appnameid=f2.appnameid\n"
//                                    //                                    + "and\n"
//                                    + getWhere("handler.id", givenIDs, false)
//                            );
//                        }
                        break;

                    case ConnID:
                        Wheres wh = new Wheres();
                        wh.addWhere(getWhere("connectionidid", givenIDs, false));
                        wh.addWhere(getWhere("transferidid", givenIDs, false), "OR");
                        return DatabaseConnector.getIDs("trigger_logbr", "id",
                                getWhere("tlibid", "select id from tlib_logbr"
                                        + wh.makeWhere(true), true)
                        );

                }
                break;
//</editor-fold>

            default:
        }
        inquirer.logger.trace("not implemented search of [" + retIDType + "] by " + givenType + " ids: " + listIDs(givenIDs));

        return null;
    }

    private void LoadRoutingType(IDType givenType, IDType theSearchType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("givenType: " + givenType + " theSearchType: " + theSearchType + " level: " + level + " ids: " + listIDs(ids));

        Integer[] iDs = getIDs(theSearchType, givenType, ids);
        if (AddIDs(theSearchType, iDs)) {
            LoadRoutingType(theSearchType, findIDs(theSearchType), level);
        }
    }

    private void LoadMediaServerType(IDType givenType, IDType theSearchType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("level: " + level + " theSearchType: " + theSearchType + " givenType: " + givenType + " ids: " + listIDs(ids));
//        inquirer.logger.debug("givenType: " + givenType + " theSearchType: " + theSearchType + " level: " + level + " ids: " + listIDs(ids));

        Integer[] iDs = getIDs(theSearchType, givenType, ids);
        if (AddIDs(theSearchType, iDs)) {
            LoadMediaServerType(theSearchType, findIDs(theSearchType), level);
        }
    }

    private void LoadTServerType(IDType givenType, IDType theSearchType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("level: " + level + " theSearchType: " + theSearchType + " givenType: " + givenType + " ids: " + listIDs(ids));
//        inquirer.logger.debug("givenType: " + givenType + " theSearchType: " + theSearchType + " level: " + level + " ids: " + listIDs(ids));

        Integer[] iDs = getIDs(theSearchType, givenType, ids);
        if (AddIDs(theSearchType, iDs)) {
            LoadTServerType(theSearchType, findIDs(theSearchType), level);
        }
    }

    private void LoadIxnType(IDType idGivenType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("idGivenType[" + idGivenType + "] level[" + level + "]");
        if (level <= 0) {
            if (maxLevelSet) {
                inquirer.logger.error("Max level reached");
            }
            return;
        }
        level--;

        LoadIxnType(idGivenType, IDType.IxnID, ids, level);
        LoadIxnType(idGivenType, IDType.ConnID, ids, level);
        LoadIxnType(idGivenType, IDType.UUID, ids, level);
    }

    private void LoadIxnType(IDType givenType, IDType theSearchType, Integer[] ids, int level) throws SQLException {
        inquirer.logger.debug("LoadIxnType givenType: " + givenType + " theSearchType: " + theSearchType + " level: " + level + " ids: " + listIDs(ids));

        if (AddIDs(theSearchType, getIDs(theSearchType, givenType, ids))) {
            LoadIxnType(theSearchType, findIDs(theSearchType), level);
        }
    }

    private Integer[] getIDsIxn(IDType retIDType, IDType searchIDType, Integer[] searchIDs) throws SQLException {
        switch (retIDType) {

            case IxnID:
                switch (searchIDType) {

                    case IxnID: {
                        return DatabaseConnector.getIDs("ixn", "PARENTIXNIDID", getWhere("IXNID", searchIDs, true));
                    }

                    case ConnID: {
                        return DatabaseConnector.getIDs("ixn", "ixnid", getWhere("connidid", searchIDs, true));
                    }
                }
                break;

            case ConnID:
                switch (searchIDType) {
                    case IxnID: {
                        return DatabaseConnector.getIDs("ixn", "connidid", getWhere("IXNID", searchIDs, true));
                    }

                }
                break;

            case DN:
                switch (searchIDType) {
                    case ConnID:
                        return DatabaseConnector.getIDs("ocs_logbr", "thisdnid", getWhere("connectionidid", searchIDs, true));

                }
                break;

            default:
        }
        return null;
    }

    private String listIDs(Integer[] value) {
        if (value == null || value.length == 0) {
            return "";
        }
        StringBuilder buf = new StringBuilder(120);
        for (Integer callid : sortedArray(value)) {
            buf.append(" ").append(callid);
        }
        return buf.toString();

    }

    private void DebugIDs() {
        if (inquirer.logger.isDebugEnabled()) {
            inquirer.logger.debug("Collected following IDs ");
            for (Map.Entry<IDType, Integer[]> entry : CallIDs.entrySet()) {
                IDType key = entry.getKey();
                inquirer.logger.debug("Type [" + key + "] IDs: " + listIDs(entry.getValue()));

            }
        }
    }

    private void LoadStatServerType(IDType idType, Integer[] ids, int i) {
    }

    boolean initSCS() {
        return true;
    }

    boolean idsFound(IDType idType) {
        return CallIDs.containsKey(idType);
    }

    boolean initWWECloud() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    boolean initRouting() throws SQLException {
        searchType = SearchType.ROUTING;

        return getRoutingIDType();

    }

    boolean initApacheWeb() throws SQLException {
        searchType = SearchType.ApacheWeb;

        return getApacheWebIDType();
    }

    boolean initWWE() throws SQLException {
        searchType = SearchType.WWE;

        return getWWEIDType();
    }

    boolean initCS() throws SQLException {
        searchType = SearchType.CONFSERV;

        return getConfServIDType();
    }

    boolean initMediaServer() throws SQLException {
        searchType = SearchType.MediaServer;

        return getMediaServerIDType();

    }

    boolean initGMS() throws SQLException {
        searchType = SearchType.GMS;

        return getGMSIDType();

    }


    /*
    init search for TServer objects
     */
    public boolean initSearch() throws SQLException {
        searchType = SearchType.TSERVER_SIP;
        if (!queriesRun) {
            switch (selectionType) {
                case CONNID:
                case CALLID:
                case GUESS_SELECTION:
                case DN:
                case PEERIP:
                case AGENTID:
                case UUID:
                case REFERENCEID:
                    idsFound = GuessCallIDs();
                    break;

                default:
                    inquirer.logger.error("unsupported search type: " + selectionType);
                    idsFound = false;

            }
//            queriesRun = true;
            if (!inquirer.getCr().isNewTLibSearch()) {
                if (idsFound) {
                    FindRelated();
                }
            }
        }
        inquirer.logger.info("initSearch: " + idsFound);

        return idsFound;
    }

    //    private HashMap<IDType, Integer[]> CallIDs = new HashMap<IDType, Integer[]>();
    /*
    returns true if type added    
     */
    private boolean AddIDs(IDType idType, Integer[] IDs) {
        /*should extend list by ID type */
        if (IDs != null && IDs.length > 0) {
            if (!CallIDs.containsKey(idType)) {
                CallIDs.put(idType, IDs);
                return true;
            } else {
                Integer[] before = CallIDs.get(idType);
                Integer[] after = uniqueInts(before, IDs);
                if (after.length != before.length) {
                    CallIDs.put(idType, after);
                    return true;
                }
            }
        }
        return false;
    }

    private void AddIDs(IDType idType, String[] ids) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    /*Returns array of TLibrary ConnIDs */
    public boolean initOutbound() throws SQLException {
        searchType = SearchType.OUTBOUND;

        return getOutboundIDType();
    }

    Integer[] getIDs(IDType retIDType, IDType searchIDType, Integer[] searchIDs) throws SQLException {
        if (searchIDs == null) {
            return null;
        }
        switch (searchType) {
            case MediaServer:
                return getIDsMediaServer(retIDType, searchIDType, searchIDs);

            case TSERVER_SIP:
                return getIDsTServer(retIDType, searchIDType, searchIDs);

            case GMS:
                return getIDsGMS(retIDType, searchIDType, searchIDs);

            case WWE:
                return getIDsWWE(retIDType, searchIDType, searchIDs);

            case OUTBOUND:
                return getIDsOutbound(retIDType, searchIDType, searchIDs);

            case ROUTING:
                return getIDsRouting(retIDType, searchIDType, searchIDs);

            case INTERACTION:
                return getIDsIxn(retIDType, searchIDType, searchIDs);

            default:
                throw new SQLException("Not implemented: retIDType: " + retIDType + " searchIDType: " + searchIDType);
        }
    }

    private Integer[] locateRef(String dn, ReferenceType rt, boolean isRegex, boolean addQuotes) throws SQLException {
        String tmpDN;
        if (isRegex) {
            tmpDN = dn;
        } else {
            if (addQuotes) {
                tmpDN = com.myutils.logbrowser.indexer.Record.SingleQuotes(dn);
            } else {
                tmpDN = dn;
            }
        }
        Integer[] DNIDs = getRefIDs(rt, new String[]{tmpDN}, isRegex);
        if (DNIDs.length == 0) {
            return null;
        }
        return DNIDs;
    }

    private Integer[] locateRef(String dn, ReferenceType rt, boolean isRegex) throws SQLException {
        return locateRef(dn, rt, isRegex, true);
    }

    private void FindRelated() throws SQLException {
        if (!m_callIdHash.isEmpty()) {
            Loop(null, new ArrayList<>(m_callIdHash), LoopState.NEW_CALLIDS_FROM_CONNIDS);
        } else if (!m_connIdHash.isEmpty()) {
            Loop(new ArrayList<>(m_connIdHash), null, LoopState.NEW_CONNIDS_FROM_CONNIDS);
        }
    }

    SelectionType getSearchType() {
        return selectionType;
    }

    Integer[] getSIPDNs() {
        return SIPdnIDs;
    }

    Integer[] getPeerIDs() {
        return peerIDs;
    }

    Integer[] getCallIDs() {
        return m_callIdHash.toArray(new Integer[m_callIdHash.size()]);
    }

    Integer[] getDNs() {
        return dnIDs;
    }

    Integer[] getConnIDs() {
        return m_connIdHash.toArray(new Integer[m_connIdHash.size()]);
    }

    Integer[] getCgDBID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    String[] getCampaigns() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    public HashSet<Integer> getCallIds() {
        return m_callIdHash;
    }

    public HashSet<Integer> getConnIds() {
        return m_connIdHash;
    }

    private boolean GuessCallIDs() throws SQLException {
        Integer[] ConnIDs = null;
        Integer[] UUIDs = null;

        if (selectionType == SelectionType.CONNID || selectionType == SelectionType.GUESS_SELECTION) {
            ConnIDs = getRefIDs(ReferenceType.ConnID, new String[]{selection}, regex);
            if (ConnIDs != null && ConnIDs.length > 0) {
                Collections.addAll(m_connIdHash, ConnIDs);
                AddIDs(IDType.ConnID, ConnIDs);
                return true;
            } else if (selectionType == SelectionType.CONNID) {
                return false;
            }
        }
        if (selectionType == SelectionType.UUID || selectionType == SelectionType.GUESS_SELECTION) {// is it AttributeCallUUID?            
            if (DatabaseConnector.refTableExist(ReferenceType.UUID)) {
                UUIDs = getRefIDs(ReferenceType.UUID, new String[]{selection}, regex);
            }
            if (UUIDs != null && UUIDs.length > 0) { // parameter is a AttributeCallUUID
                AddIDs(IDType.UUID, UUIDs);
                ConnIDs = DatabaseConnector.getDatabaseConnector(this).getIDs(this, "tlib_logbr", "ConnectionIDID", IQuery.getWhere("uuidID", UUIDs, true));
                if (ConnIDs != null && ConnIDs.length > 0) { // parameter is a ConnID
                    Collections.addAll(m_connIdHash, ConnIDs);
                    return true;
                }
            } else if (selectionType == SelectionType.UUID) {
                return false;
            }
        }

        if (selectionType == SelectionType.CALLID || selectionType == SelectionType.GUESS_SELECTION) {// is it AttributeCallUUID?            
            Integer[] _CallIDs = getRefIDs(ReferenceType.SIPCALLID, new String[]{selection}, regex);
            if (_CallIDs.length == 0) {
                _CallIDs = getRefIDsLike(this, ReferenceType.SIPCALLID, new String[]{selection});
            }
            if (ConnIDs != null && _CallIDs.length > 0) {
                AddIDs(IDType.SIPCallID, _CallIDs);
                Collections.addAll(m_callIdHash, _CallIDs);
                return true;
            } else if (selectionType == SelectionType.CALLID) {
                return false;
            }
        }
        if (selectionType == SelectionType.DN || selectionType == SelectionType.GUESS_SELECTION) {// is it AttributeCallUUID?            
            dnIDs = locateRef(selection, ReferenceType.DN, regex);
            SIPdnIDs = locateRef(selection, ReferenceType.DN, regex);
            if ((dnIDs != null && dnIDs.length > 0)
                    || (SIPdnIDs != null && SIPdnIDs.length > 0)) {
                AddIDs(IDType.DN, dnIDs);
                return true;
            } else if (selectionType == SelectionType.DN) {
                return false;
            }
        }
        if (selectionType == SelectionType.PEERIP || selectionType == SelectionType.GUESS_SELECTION) {// is it AttributeCallUUID?            
            peerIPs = locateRef(selection, ReferenceType.IP, regex, false);
            if (peerIPs != null && peerIPs.length > 0) {
                AddIDs(IDType.PEERIP, peerIPs);
                return true;
            } else if (selectionType == SelectionType.PEERIP) {
                return false;
            }
        }
        if (selectionType == SelectionType.AGENTID || selectionType == SelectionType.GUESS_SELECTION) {// is it AttributeCallUUID?            
            agentIDs = locateRef(selection, ReferenceType.Agent, regex, false);
            if (agentIDs != null && agentIDs.length > 0) {
                AddIDs(IDType.AGENT, agentIDs);
                return true;
            } else if (selectionType == SelectionType.AGENTID) {
                return false;
            }
        }
        if (selectionType == SelectionType.REFERENCEID || selectionType == SelectionType.GUESS_SELECTION) { // is it AttributeCallUUID?
            refIDs = DatabaseConnector.getDatabaseConnector(this).getIDs(this,
                    "tlib_logbr", "referenceID", IQuery.getWhere("referenceID", new String[]{selection}, true, false));

            if (refIDs != null && refIDs.length > 0) {
                AddIDs(IDType.TLibRefID, refIDs);
                return true;
            } else if (selectionType == SelectionType.REFERENCEID) {
                return false;
            }
        }
        return false;

    }

    private void Loop(ArrayList<Integer> newConnIds,
                      ArrayList<Integer> newCallIds,
                      LoopState state) throws SQLException {
        boolean newCallIdsFromConnIds = true;
        boolean newCallIdsFromBlocks = true;
        boolean newConnIdsFound = true;
        int depth = 0;
        IQuery query;

        while ((newCallIdsFromConnIds || newCallIdsFromBlocks || newConnIdsFound)
                && depth < MAX_DEPTH) {
            inquirer.logger.debug("\t++ Loop: depth=" + depth + " state: " + state);
            switch (state) {
                case NEW_CONNIDS:
                    // retrieve CallIds for these ConnIds
                    // if new ones found, set state to
                    // new_callids_from_connids and continue,
                    // otherwise exit
                    newCallIdsFromConnIds = false;
                    state = LoopState.NEW_CALLIDS_FROM_CONNIDS;
                    if (newCallIds == null) {
                        newCallIds = new ArrayList<>();
                    }
                    if (newConnIds == null || newConnIds.isEmpty()) {
                        break;
                    }

                    /* Trying to optimize SQL request. 
                        CallIdFromConnIdBlockQuery callIdQuery = new CallIdFromConnIdBlockQuery(newConnIds);
                        callIdQuery.Execute();
                        for (Integer callid = callIdQuery.GetNext();
                        callid != null;
                        callid = callIdQuery.GetNext()) {
                        if (callid > 0 && !m_callIdHash.contains(callid)) {
                        int val = callid.intValue();
                        m_callIdHash.add(val);
                        if (!newCallIds.contains(val)) {
                        newCallIds.add(val);
                        }
                        newCallIdsFromConnIds = true;
                        }
                        }
                        callIdQuery.Reset();                        
                     */
                    String alias = DatabaseConnector.getDatabaseConnector(this).getAlias();

                    if (true) {
                        String connIDWhere = IQuery.getWhere("tlib.connectionidid", newConnIds.toArray(new Integer[newConnIds.size()]), true) + "\n";
                        for (TReqTabs q : new TReqTabs[]{
                                new TReqTabs("SELECT distinct callidid FROM tlib_" + alias + " AS tlib "
                                        + "INNER JOIN handler_" + alias + " AS handler "
                                        + "ON tlib.handlerId=handler.id \n"
                                        + "INNER JOIN trigger_" + alias + " AS trgr "
                                        + "ON trgr.textid=handler.textid \n"
                                        + "INNER JOIN sip_" + alias + " AS sip "
                                        + "ON sip.handlerid=trgr.handlerId \n"
                            /*Not joining FILE_logbr anymore as trigger name is unique per fix for
                                        Bug 30 - Unrelated calls in newSearch for ID
                                        http://co67.step/bugzilla/show_bug.cgi?id=30
                             */
                                        //                                + "INNER JOIN FILE_logbr AS f1 ON tlib.fileId=f1.id \n"
                                        //                                + "INNER JOIN FILE_logbr AS f2 ON handler.fileId=f2.id \n"
                                        //                                + "INNER JOIN FILE_logbr AS f3 ON sip.fileId=f3.id \n"
                                        //                                + "INNER JOIN FILE_logbr AS f4 ON trgr.fileId=f4.id \n"
                                        + connIDWhere
                                        + "AND sip.handlerid!=0 \n" //                                + "and f1.appnameid=f2.appnameid and f2.appnameid=f3.appnameid and f3.appnameid=f4.appnameid"
                                        ,
                                        new String[]{"tlib_" + alias, "handler_" + alias, "trigger_" + alias, "sip_" + alias}),
                                /*

                                below request is particularly slow. Not sure if it is needed */
                                new TReqTabs("SELECT distinct callidid FROM tlib_" + alias + " AS tlib \n"
                                        + "INNER JOIN sip_" + alias + " AS sip "
                                        + "ON tlib.handlerId=sip.handlerId\n"
                                        + "INNER JOIN FILE_logbr AS f1 ON tlib.fileId=f1.id \n"
                                        + "INNER JOIN FILE_logbr AS f2 ON sip.fileId=f2.id \n"
                                        + connIDWhere
                                        + "\nand f1.appnameid=f2.appnameid"
                                        + "\nAND tlib.handlerId>0"
                                        + "\nand sip.callRelated=1 \n",
                                        new String[]{"tlib_" + alias, "handler_" + alias, "sip_" + alias}),
                                /**
                                 * **************************
                                */
                                new TReqTabs("SELECT distinct callidid from tlib_" + alias + " AS tlib \n"
                                        + "INNER JOIN cireq_" + alias + " AS cireq "
                                        + "ON cireq.nameid=tlib.nameid "
                                        + "AND cireq.refid=tlib.referenceid "
                                        + "AND cireq.thisdnid=tlib.thisdnid "
                                        + "AND cireq.otherdnid=tlib.otherdnid \n"
                                        + "INNER JOIN sip_" + alias + " AS sip "
                                        + "ON cireq.handlerid=sip.handlerid AND sip.handlerid>0 \n"
                                        + "INNER JOIN FILE_logbr AS f1 ON tlib.fileId=f1.id \n"
                                        + "INNER JOIN FILE_logbr AS f2 ON cireq.fileId=f2.id \n"
                                        + "INNER JOIN FILE_logbr AS f3 ON sip.fileId=f3.id \n"
                                        + connIDWhere
                                        + "and f1.appnameid=f2.appnameid and f2.appnameid=f3.appnameid",
                                        new String[]{"tlib_" + alias, "sip_" + alias, "cireq_" + alias}),
                                /**
                                 * **************************
                                */
                                new TReqTabs("SELECT distinct callidid FROM sip_logbr AS sip \n"
                                        + "INNER JOIN trigger_logbr AS trgr ON trgr.handlerId=sip.handlerid \n"
                                        + "INNER JOIN handler_logbr AS handler ON handler.textid=trgr.textid \n"
                                        + "INNER JOIN connid_logbr AS tlib ON handler.id=tlib.handlerid \n"
                                        + "INNER JOIN FILE_logbr AS f1 ON tlib.fileId=f1.id \n"
                                        //                                + "INNER JOIN FILE_logbr AS f2 ON handler.fileId=f2.id \n"
                                        + "INNER JOIN FILE_logbr AS f3 ON sip.fileId=f3.id \n"
                                        //                                + "INNER JOIN FILE_logbr AS f4 ON trgr.fileId=f4.id \n"
                                        + connIDWhere
                                        + "and f1.appnameid=f3.appnameid "
                                        //                                        + "and f2.appnameid=f3.appnameid and f3.appnameid=f4.appnameid"
                                        + ";\n",
                                        new String[]{"connid_" + alias, "handler_" + alias, "trigger_" + alias, "sip_" + alias}),}) {
                            if (q.allTabsExist()) {
                                Integer[] callids = DatabaseConnector.getIDs(q.getReq());
                                DebugIDs(callids);
                                if (callids != null) {
                                    for (Integer callid : callids) {
                                        if (callid > 0 && !m_callIdHash.contains(callid)) {
                                            int val = callid;
                                            m_callIdHash.add(val);
                                            if (!newCallIds.contains(val)) {
                                                newCallIds.add(val);
                                            }
                                            newCallIdsFromConnIds = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    depth++;

                    break;

                case NEW_CALLIDS_FROM_CONNIDS:
                    // retrieve callids in the same blocks with new callids
                    // if new ones found, set state to new_callids_from_blocks
                    // and continue, otherwise exit
                    newCallIdsFromBlocks = false;
                    state = LoopState.NEW_CALLIDS_FROM_BLOCKS;
                    if (newCallIds == null) {
                        newCallIds = new ArrayList<>();
                    }
                    if (newCallIds.isEmpty()) {
                        break;
                    }

                    try {
                        /**
                         * ********* search for uuid UUID *****************
                         */
                        if (DatabaseConnector.TableExist("sip_logbr")) {
                            Integer[] callids = getIDs(
                                    this,
                                    "sip_logbr",
                                    "callidid",
                                    getWhere("uuidid",
                                            MakeQueryID(
                                                    "uuidid",
                                                    "sip_logbr",
                                                    getWhere("callidid",
                                                            newCallIds.toArray(new Integer[newCallIds.size()]),
                                                            " AND uuidid>0 ",
                                                            true)
                                            ),
                                            true
                                    )
                            );

                            DebugIDs(callids);
                            for (int callid : callids) {
                                if (!m_callIdHash.contains(callid)) {
                                    m_callIdHash.add(callid);
                                    if (!newCallIds.contains(callid)) {
                                        newCallIds.add(callid);
                                    }
                                    newCallIdsFromBlocks = true;
                                }
                            }

                            /**
                             * *****************************
                             */
                            callids = getIDs(
                                    this,
                                    "sip_logbr",
                                    "callidid",
                                    getWhere("handlerid",
                                            MakeQueryID(
                                                    "handlerid",
                                                    "sip_logbr",
                                                    getWhere("callidid",
                                                            newCallIds.toArray(new Integer[newCallIds.size()]),
                                                            " AND handlerid>0 ",
                                                            true)
                                            ),
                                            true
                                    ) + " and callRelated=1 "
                            );
                            DebugIDs(callids);
                            for (int callid : callids) {
                                if (!m_callIdHash.contains(callid)) {
                                    m_callIdHash.add(callid);
                                    if (!newCallIds.contains(callid)) {
                                        newCallIds.add(callid);
                                    }
                                    newCallIdsFromBlocks = true;
                                }
                            }
                        }

                        depth++;

                    } catch (ArrayStoreException e) {
                        inquirer.ExceptionHandler.handleException("callid not found", e);
                        newCallIdsFromBlocks = false;
                    }
                    break;

                case NEW_CONNIDS_FROM_CONNIDS:
                    newConnIdsFound = false;
                    state = LoopState.NEW_CONNIDS;
                    if (newConnIds == null) {
                        newConnIds = new ArrayList<>();
                    }
                    if (newConnIds.isEmpty()) {
                        break;
                    }

                    for (Integer connid : m_connIdHash) {
                        if (RetrieveParentTransfers(connid, 1)
                                || RetrieveChildTransfers(connid, 1)) {
                            newConnIdsFound = true;
                            break;
                        }
                    }
                    if (newConnIdsFound) {
                        for (Integer newCallId : m_connIdHash) {
                            if (!newConnIds.contains(newCallId)) {
                                newConnIds.add(newCallId);
                            }
                        }
                    }

                    depth++;

                    break;

                case NEW_CALLIDS_FROM_BLOCKS:
                    // retrieve connids dependent on new callids
                    // If new ones found, set state to NEW_CONNIDS
                    // and continue, otherwise exit
                    newConnIdsFound = false;
                    state = LoopState.NEW_CONNIDS;
                    if (newConnIds == null) {
                        newConnIds = new ArrayList<>();
                    }
                    if (newCallIds.isEmpty()) {
                        break;
                    }

                    ConnIdFromCallIdBlockQuery connIdQuery = new ConnIdFromCallIdBlockQuery(newCallIds);
                    connIdQuery.Execute();

                    for (Integer connid = connIdQuery.GetNext();
                         connid != null;
                         connid = connIdQuery.GetNext()) {
                        if (connid > 0 && !m_connIdHash.contains(connid)) {
                            m_connIdHash.add(connid);
                            newConnIdsFound = true;
                            RetrieveParentTransfers(connid, 1);
                            RetrieveChildTransfers(connid, 1);
                        }
                    }
                    if (newConnIdsFound) {
                        for (Integer newCallId : m_connIdHash) {
                            if (!newConnIds.contains(newCallId)) {
                                newConnIds.add(newCallId);
                            }
                        }
                    }
                    connIdQuery.Reset();
                    depth++;

                    break;
            }
        }
    }

    private boolean RetrieveParentTransfers(Integer connId, int depth) throws SQLException {
        boolean ret = false;
        if (depth < MAX_DEPTH) {

            ParentTransferQuery query = new ParentTransferQuery(connId);
            query.Execute();
            for (Integer retrievedConnId = query.GetNext();
                 retrievedConnId != null;
                 retrievedConnId = query.GetNext()) {

                if (retrievedConnId > 0 && !m_connIdHash.contains(retrievedConnId)) {
                    m_connIdHash.add(retrievedConnId);
                    RetrieveParentTransfers(retrievedConnId, depth + 1);
                    ret = true;
                }
            }
            Integer[] connIDs = {connId};
            Integer[] ids = DatabaseConnector.getIDs("connid_logbr", "connectionidID", getWhere("newConnIDID", connIDs, true));
            if (ids != null && ids.length > 0) {
                for (Integer id : ids) {
                    m_connIdHash.add(id);
                    RetrieveParentTransfers(id, depth + 1);
                }
                ret = true;
            }
            connIDs[0] = connId;
            ids = DatabaseConnector.getIDs("iscc_logbr", "connectionidID", getWhere("otherConnectionIDID", connIDs, true));
            if (ids != null && ids.length > 0) {
                for (Integer id : ids) {
                    m_connIdHash.add(id);
                    RetrieveParentTransfers(id, depth + 1);
                }
                ret = true;
            }
        }
        return ret;
    }

    private boolean RetrieveChildTransfers(Integer connId, int depth) throws SQLException {
        boolean ret = false;
        if (depth < MAX_DEPTH) {
            ChildTransferQuery query = new ChildTransferQuery(connId);
            query.Execute();
            for (Integer retrievedConnId = query.GetNext();
                 retrievedConnId != null;
                 retrievedConnId = query.GetNext()) {

                if (retrievedConnId > 0 && !m_connIdHash.contains(retrievedConnId)) {
                    m_connIdHash.add(retrievedConnId);
                    RetrieveChildTransfers(retrievedConnId, depth + 1);
                    ret = true;
                }
            }
            Integer[] connIDs = {connId};
            Integer[] ids = DatabaseConnector.getIDs("connid_logbr", "newConnIDID", getWhere("connectionidID", connIDs, true));
            if (ids != null && ids.length > 0) {
                for (Integer id : ids) {
                    m_connIdHash.add(id);
                    RetrieveChildTransfers(id, depth + 1);
                }
                ret = true;
            }
            connIDs[0] = connId;
            ids = DatabaseConnector.getIDs("iscc_logbr", "otherConnectionIDID", getWhere("connectionidID", connIDs, true));
            if (ids != null && ids.length > 0) {
                for (Integer id : ids) {
                    m_connIdHash.add(id);
                    RetrieveParentTransfers(id, depth + 1);
                }
                ret = true;
            }

        }
        return ret;
    }

    private enum SearchType {
        UNKNOWN,
        ROUTING,
        OUTBOUND,
        TSERVER_SIP,
        INTERACTION,
        STATSERVER,
        GMS,
        WWE,
        MediaServer,
        ApacheWeb,
        CONFSERV
    }

    public enum RequestLevel {
        LevelMax("Maximum"),
        Level0("No related search"),
        Level1("Level 1"),
        Level2("Level 2"),
        Level3("Level 3"),
        Level4("Level 4"),
        Level5("Level 5"),
        Level6("Level 6"),
        Level7("Level 7"),
        Level8("Level 8"),
        ;

        private final String name;

        RequestLevel(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return name.equals(otherName);
        }

        @Override
        public String toString() {
            return this.name.toLowerCase();
        }

    }

    private enum LoopState {
        NEW_CONNIDS,
        NEW_CALLIDS_FROM_CONNIDS,
        NEW_CONNIDS_FROM_CONNIDS,
        NEW_CALLIDS_FROM_BLOCKS
    }

    private static class WhereBuilder {

        private final ArrayList<String> components;

        public WhereBuilder() {
            this.components = new ArrayList<>();
        }

        private WhereBuilder addOpenBracket() {
            components.add("(");
            return this;
        }

        private WhereBuilder addCond(String where) {
            if (StringUtils.isNoneEmpty(where)) {
                components.add(where);
            }
            return this;
        }

        private WhereBuilder addCond(boolean cond, String AndOr, String where) {
            if (cond) {
                if (!components.isEmpty()) {
                    addAnd();
                }
                components.add(where);
            }
            return this;
        }

        private WhereBuilder addAnd() {
            if (!components.isEmpty()) {
                components.add("AND");
            }
            return this;

        }

        private WhereBuilder addCloseBracket() {
            components.add(")");
            return this;
        }

        private String build(boolean addWhere) {
            StringBuilder ret = new StringBuilder();
            if (addWhere) {
                ret.append("where");
            }
            for (String component : components) {
                ret.append(" ");
                ret.append(component);
            }
            return ret.toString();
        }
    }

    class IDFound {

        public IDType idType;
        private Integer[] ids = null;

        public IDFound(IDType idType, Integer[] _ids) {
            this.idType = idType;
            this.ids = _ids;
        }
    }

    class TReqTabs {

        private final String[] tabs;
        private final String req;

        public TReqTabs(String req, String[] tabs) {
            this.req = req;
            this.tabs = tabs;
        }

        public String getReq() {
            return req;
        }

        private boolean allTabsExist() throws SQLException {
            for (String tab : tabs) {
                if (!DatabaseConnector.TableExist(tab)) {
                    return false;
                }
            }
            return true;
        }
    }

}
