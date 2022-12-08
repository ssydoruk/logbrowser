/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSDBActivity extends Message {

    //16:19:06.052 CM_DBCallList(236-136-336): DBServer 'DBServer_OCS for CL_CALLBACK (1440)' MSG_RETRIEVED(DBM_SUCCESS) [ReqID=877866]
    private static final Pattern regDBServerReqID = Pattern.compile("\\[ReqID=([0-9]+)\\]$");
    private static final Pattern regDBServerReqIDError = Pattern.compile("\\[ReqID=([0-9]+)\\]");
    private static final Pattern regDBServerReq = Pattern.compile(
            "DBServer\\s+'(\\S+) for (\\S+) \\(\\d+\\)'");
    private static final DBAction dbAction = new DBAction();

    private long listDBID = -1;
    private long campaignDBID = -1;
    private long groupDBID = -1;
    private long reqID = -1;
    private int chainID = -1;
    private String DBServer = null;
    private String dbact = null;
    private boolean DBParsed = false;

    private String listName;

    public OCSDBActivity(String listDBID, String campaignDBID, String groupDBID, String line, int fileID) {
        super(TableType.OCSDBActivity, fileID);
        this.listDBID = Long.parseLong(listDBID);
        this.campaignDBID = Long.parseLong(campaignDBID);
        this.groupDBID = Long.parseLong(groupDBID);

        Matcher m;
        if ((m = regDBServerReqID.matcher(line)).find()) {
            this.reqID = Long.parseLong(m.group(1));
        } else if ((m = regDBServerReqIDError.matcher(line)).find()) {
            this.reqID = Long.parseLong(m.group(1));
        }

    }

    public String GetSid() {
        return GetHeaderValueNoQotes("session");
    }

    public String GetCall() {
        return GetHeaderValueNoQotes("call");
    }

    /**
     * @return the listDBID
     */
    public long getListDBID() {
        return listDBID;
    }

    /**
     * @return the campaignDBID
     */
    public long getCampaignDBID() {
        return campaignDBID;
    }

    /**
     * @return the groupDBID
     */
    public long getGroupDBID() {
        return groupDBID;
    }

    /**
     * @return the reqID
     */
    public long getReqID() {
        return reqID;
    }

    long getChainID() {
        return chainID;
    }

    void setChainID(String group) {
        try {
            chainID = Integer.parseInt(group);
        } catch (NumberFormatException numberFormatException) {
            Main.logger.error("Incorrect number: [" + group + "]", numberFormatException);
        }
    }

    private void parseDB() {
        if (!DBParsed) {
            Matcher m;
            String l = (m_MessageLines.size() > 0) ? m_MessageLines.get(0) : null;
            dbact = null;
            if (l != null) {
                if ((m = regDBServerReq.matcher(l)).find()) {
                    DBServer = m.group(1);
                    this.listName = m.group(2);
                    dbact = dbAction.get(l.substring(m.end()));
                } else {
                    dbact = dbAction.get(l);
                }
                Main.logger.trace("l:[" + l + "] dbact:[" + dbact + "]");
            }
            DBParsed = true;
        }
    }

    String getDBReq() {
        parseDB();
        return dbact;
    }

    String getDBServer() {
        parseDB();
        Main.logger.trace("getDBReq dbact:[" + DBServer + "]");
        return DBServer;
    }

    String getCallingList() {
        parseDB();
        return listName;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        stmt.setLong(6, getListDBID());
        stmt.setLong(7, getCampaignDBID());
        stmt.setLong(8, getGroupDBID());
        stmt.setLong(9, getReqID());
        stmt.setLong(10, getChainID());
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.DBRequest, getDBReq()));
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.App, getDBServer()));
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.OCSCallList, getCallingList()));
        return true;
    }

    private static class DBAction {

        private static final Map<Pattern, String> dbrecMap = new HashMap<>();

        DBAction() {
            try {
//CM_DBCallList(311-250-5098): DBServer 'ca_ocs_dbserver_pri for PM_CA_SERVICE_QUEUE_10_Calling_List (876)' MSG_PROCCOMPLETED(DBM_SUCCESS) [ReqID=15202170]
                addAction(" (MSG_\\S+)", "");

//CM_DBCallList(311-250-5098): DBServer 'ca_ocs_dbserver_pri for PM_CA_SERVICE_QUEUE_10_Calling_List (876)' SQL: sp3112505098922 @CampID = '250', @GroupID = '5098', @ListID = '311', @RecType = 2, @NumRec = 4, @CTime = '11969', @TTime = '1501730469', @SPResult = @out [ReqID=15455458]
//13:49:36.902 CM_DBCallList(104-103-115): ExecuteSP:0
                addAction(new String[]{"SQL: sp", ": ExecuteSP"}, "SP procedure");

//CM_DBCallList(311-250-5098): DBServer 'ca_ocs_dbserver_pri for PM_CA_SERVICE_QUEUE_10_Calling_List (876)' SQL: sr3112505098922 @CTime = '12138', @ReadyCount = @out, @RetrievedCount = @out, @FinalCount = @out, @TotalCount = @out, @TotalRecsCount = @out, @ReadyRecsCount = @out, @CustomCounter01 = @out, @CustomCounter02 = @out, @CustomCounter03 = @out, @CustomCounter04 = @out, @CustomCounter05 = @out [ReqID=15457846]
//13:49:35.870 CM_DBCallList(104-103-115): ExecuteSR
//13:49:36.745 CM_DBCallList(106-104-5877): SR return value (ReadyCount):780
                addAction(new String[]{"SQL: sr", ": ExecuteSR$", ": SR return value"}, "SR procedure");

//CM_DBCallList(311-250-5098): ExecuteRequest:0
                addAction(": (Execute.+)", "");

//15:52:15.924 CM_DBCallList(575-513-17484): Estimated Hit Ratio 0.7500; Processed Calls: 4; Answered Calls: 3
                addAction(": (Estimated Hit Ratio)", "Campaign dial statistic");

//CM_DBCallList(311-250-5098): SelectEnd:0  SelectedRecords:0 RequestID:15202300 ReadyRecords:9 SelectTime:0
//CM_DBCallList(311-250-5098): DBServer 'ca_ocs_dbserver_pri for PM_CA_SERVICE_QUEUE_10_Calling_List (876)' SQL: select T.* from #ReadyRecord R, MSC_PM_Calling_List T where R.chain_id=T.chain_id and T.record_status = 2  and (Business_Unit = 'PMQ CA S10'  AND  Campaign_Name = '10CA AUTO INSV INC AO END')  order by Extract_Date DESC [ReqID=15202300]                
                addAction(new String[]{"SelectEnd:", "SQL:\\s*select.+from[\\s\\#]+ReadyRecord", "SQL: TRUNCATE TABLE \\#ReadyRecord"}, "ReadyRecords table requests");

//CM_DBCallList(311-250-5098): WeightInfo: RecordType:  2, Agents:  1, Share: 10, RecsNeededTotal:  4, SumShares: 10, Result:  4                
                addAction("WeightInfo:", "WeightInfo - records needed");
//CM_DBCallRecord(104-103-115): DBServer 'CUL_DBServer_OCS for CL_VAD_MASTER (852)' SQL:  update VAD_MASTER set call_result=9,agent_id='',call_time=1506013366,record_type=2,record_status=3,attempt=1,switch_id=108  where chain_id=103 and chain_n=2 [ReqID=95186]
                addAction("SQL:\\s+update", "SQL - update");

            } catch (Exception exception) {
                Main.logger.error("Cannot create StrategySteps1", exception);
            }
        }

        public String get(String logLine) {
            Main.logger.trace("get DBActivity: [" + dbrecMap + "] count: " + dbrecMap.size() + "]");

            for (Map.Entry<Pattern, String> entry : dbrecMap.entrySet()) {
                Pattern key = entry.getKey();
                String value = entry.getValue();
                Matcher m;
                Main.logger.trace("Compare DBActivity: [" + logLine + "] vs [" + key.toString() + "]");

                try {
                    if ((m = key.matcher(logLine)).find()) {
                        Main.logger.trace("Compare DBActivity: [" + logLine + "] vs [" + key + "] :" + (((m.groupCount() > 0)) ? m.group(1) : ""));
                        if (m.groupCount() > 0) {
                            return m.group(1);
                        } else {
                            return value;
                        }
                    }
                }
                catch (NullPointerException e){
                    Main.logger.error("error", e);
                }

            }
            return "Misc";
        }

        private void addAction(String[] pt, String retString) throws Exception {
            for (String string : pt) {
                addAction(string, retString);
            }
        }

        private void addAction(String pt, String retString) throws Exception {
            dbrecMap.put(Pattern.compile(pt), retString);
        }

    }
}
