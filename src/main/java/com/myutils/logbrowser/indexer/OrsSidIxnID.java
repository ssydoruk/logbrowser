/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;
//import org.jdom.input.*;
//import org.jdom.xpath.XPath;

/**
 * @author ssydoruk
 */

/*It extends TServer message, however there is now TServer message attributes*/
public class OrsSidIxnID extends Message {

    private final String IxnID;

    private final String GID;
    private String theURL = null;
    private String app = null;

    public OrsSidIxnID(String IxnID, String GID) {
        super(TableType.ORSSidIxnID);

        this.IxnID = IxnID;
        this.GID = GID;
    }

    public String getGID() {
        return GID;
    }

    String getIxnID() {
        return IxnID;
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

}
