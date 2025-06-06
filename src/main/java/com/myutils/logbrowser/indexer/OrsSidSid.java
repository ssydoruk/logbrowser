/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;
//import org.jdom.input.*;
//import org.jdom.xpath.XPath;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ssydoruk
 */

/*It extends TServer message, however there is now TServer message attributes*/
public class OrsSidSid extends Message {

    private final String SID;
    private final String newSID;
    private final String app = null;

    public OrsSidSid(String SID, String NewSID, int fileID) {
        super(TableType.ORSSidSid, fileID);

        this.SID = SID;
        this.newSID = NewSID;
    }

    String getSID() {
        return SID;
    }

    String getNewSID() {
        return newSID;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setInt(1, getFileID());

        setFieldInt(stmt, 2, Main.getRef(ReferenceType.ORSSID, getSID()));
        setFieldInt(stmt, 3, Main.getRef(ReferenceType.ORSSID, getNewSID()));
        return true;
    }
}
