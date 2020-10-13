/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;
//import org.jdom.input.*;
//import org.jdom.xpath.XPath;

/**
 *
 * @author ssydoruk
 */

/*It extends TServer message, however there is now TServer message attributes*/
public class OrsSidUuid extends Message {

    private final String UUID;
    private final String GID;
    private String theURL = null;
    private String app = null;

    public OrsSidUuid(String UUID, String GID) {
        super(TableType.ORSSidUUID);

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

    void setApp(String app) {
        this.app = app;
    }

    String getURL() {
        return theURL;
    }

    String getApp() {
        return app;
    }
}
