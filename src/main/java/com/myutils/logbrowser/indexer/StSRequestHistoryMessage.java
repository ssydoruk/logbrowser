package com.myutils.logbrowser.indexer;

import java.util.ArrayList;

/**
 * @author akolo
 */
public class StSRequestHistoryMessage extends Message {

    String m_MessageName;

    public StSRequestHistoryMessage(String event, ArrayList newMessageLines, int fileID) {
        super(TableType.StSRequestHistory, fileID);
        m_MessageLines = newMessageLines;
        m_MessageName = event;
        m_MessageLines.add(0, event);

    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetName() {
        return GetHeaderValue("Name");
    }

    public String GetRequestID() {
        return GetHeaderValue("'REQ_ID'");
    }

    public String GetRequestUserID() {
        return GetHeaderValue("'REQ_USER_ID'");
    }

    public String GetAssocRequestID() {
        return GetHeaderValue("'ASSOC_REQ_ID'");
    }

//    public static String InitDB(DBAccessor accessor, int statementId) {
//        m_statementId = statementId;
//        
//        String query = "create table if not exists STSREQHISTORY_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"+
//		        "rid INT," +
//		        "urid INT," +
//		        "arid INT," +
//		        "clname CHAR(128)," +
//		        "cluid CHAR(64)," +
//		        "time timestamp," +
//		        "action CHAR(64)," +
//				"FileId INT,"+
//				"FileOffset BIGINT,"+
//				"FileBytes INT,"+
//				"HandlerId INT,"+
//				"line INT);";
//        accessor.ExecuteQuery(query);
//        return "INSERT INTO STSREQHISTORY_"+m_alias+" VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?);";
//    }
//    
//    public void AddToDB(DBAccessor accessor) {
//        
//        PreparedStatement stmt = accessor.GetStatement(m_statementId);
//        
//        try {
//            setFieldString(stmt,1,GetRequestID());
//            setFieldString(stmt,2,GetRequestUserID());
//            setFieldString(stmt,3,GetAssocRequestID());
//            setFieldString(stmt,4,GetName());
//            setFieldString(stmt,5,GetHeaderValue("CLUID"));
//            stmt.setTimestamp(6, new Timestamp(GetAdjustedUsecTime()));
//            setFieldString(stmt,7,GetMessageName());
//            stmt.setInt(8,m_fileId);
//            stmt.setLong(9,getM_fileOffset());
//            stmt.setInt(10,GetFileBytes());
//            stmt.setInt(11,0);
//            stmt.setInt(12,getM_line());                
//            
//            accessor.SubmitStatement(m_statementId);
//        } catch (Exception e) {
//            Main.logger.error("Could not add RequestHistory/StatServer message: " + e);
//        }
//    }
}
