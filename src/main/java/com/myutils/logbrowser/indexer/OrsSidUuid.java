/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;
//import org.jdom.input.*;
//import org.jdom.xpath.XPath;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 */

/*It extends TServer message, however there is now TServer message attributes*/
public class OrsSidUuid extends Message {

    private final String UUID;
    private final String GID;
    private String theURL = null;
    private String app = null;

    public OrsSidUuid(String UUID, String GID, int fileID) {
        super(TableType.ORSSidUUID, fileID);

        this.UUID = UUID;
        this.GID = GID;
    }

    public String getUUID() {
        return UUID;
    }

    public String getGID() {
        return GID;
    }

    /*
    public void AddToDB(DBAccessor accessor) {
        try {
            PreparedStatement stmt = accessor.GetStatement(m_batchId);        
            setFieldString(stmt,8,GID);
            setFieldString(stmt,9,UUID); 
            accessor.SubmitStatement(m_batchId);
        } catch (SQLException ex) {
            logger.error("fatal: ",  ex);
        }
    }
     */
//    private String getXMLProperty(Element JDOMElement, String propertyName) {
//        String ret = JDOMElement.getAttributeValue(propertyName);
//        
//        if( ret!=null ){
//            return ret;
//        }
//        else{
//            return "";
//        }
//    }
    String getIxnID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setURI(String URL) {
        this.theURL = URL;
    }

    String getURL() {
        return theURL;
    }

    String getApp() {
        return app;
    }

    void setApp(String app) {
        this.app = app;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ORSSID, getGID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.UUID, getUUID()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.URSStrategyName, getURL()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.URSStrategyName, getApp()));
        return true;

    }
}
