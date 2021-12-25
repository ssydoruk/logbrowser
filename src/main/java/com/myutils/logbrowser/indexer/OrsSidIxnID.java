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
public class OrsSidIxnID extends Message {

    private final String IxnID;

    private final String GID;
    private String theURL = null;
    private String app = null;

    public OrsSidIxnID(String IxnID, String GID, int fileID) {
        super(TableType.ORSSidIxnID, fileID);

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

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ORSSID, getGID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, getIxnID()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.URSStrategyName, getURL()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.URSStrategyName, getApp()));
        return true;

    }
}
