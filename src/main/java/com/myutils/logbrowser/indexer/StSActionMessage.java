package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author akolo
 */
public class StSActionMessage extends Message {

    static final Pattern regAction = Pattern.compile("^Action:\\s*(:?\\(owned\\))?\\s*(\\w+)[\\s']+([^@]+)[^']+'\\s*(?:\\((\\w+)\\))?");

    private String m_MessageName = null;
    private String m_type = null;
    private String m_value = null;

    public StSActionMessage(String header, ArrayList newMessageLines) {
        super(TableType.StSAction);

        m_MessageLines = newMessageLines;
        Matcher m;

        if ((m = regAction.matcher(header)).find()) {
            m_MessageName = m.group(3);//DN
            m_type = m.group(2); //DN type
            m_value = m.group(4);//DN action name
        }
        m_MessageLines.add(0, header);

    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetType() {
        return m_type;
    }

    public String GetName() {
        return m_MessageName;
    }

    public String GetValue() {
        return m_value;
    }

    public String GetConnID() {
        return GetHeaderValue("ConnID");
    }

//    public static String InitDB(DBAccessor accessor, int statementId) {
//        m_statementId = statementId;
//        
//        String query = "create table if not exists STSACTION_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"+
//				"time timestamp," +
//				"name CHAR(64)," +
//				"type CHAR(32)," +
//				"value CHAR(32)," +
//				"conn_id CHAR(32)," +
//				"FileId INT,"+
//				"FileOffset BIGINT,"+
//				"FileBytes INT,"+
//				"HandlerId INT,"+
//				"line INT);";
//        accessor.ExecuteQuery(query);
//        return "INSERT INTO STSACTION_"+m_alias+" VALUES(NULL,?,?,?,?,?,?,?,?,?,?);";
//    }
//    
//    public void AddToDB(DBAccessor accessor) {
//        
//        PreparedStatement stmt = accessor.GetStatement(m_statementId);
//        
//        try {
//            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
//            setFieldString(stmt,2,GetName());
//            setFieldString(stmt,3,GetType());
//            setFieldString(stmt,4,GetValue());
//            setFieldString(stmt,5,GetConnID());
//            stmt.setInt(6,m_fileId);
//            stmt.setLong(7,getM_fileOffset());
//            stmt.setInt(8,GetFileBytes());
//            stmt.setInt(9,0);
//            stmt.setInt(10,getM_line());                
//            
//            accessor.SubmitStatement(m_statementId);
//        } catch (Exception e) {
//            System.out.println("Could not add Action/StatServer message: " + e);
//        }
//    }
}
