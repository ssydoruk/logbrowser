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
public class URSCONNIDIxnID extends Message {

    private final String IxnID;

    private final String ConnID;

    public URSCONNIDIxnID(String ConnID, String IxnID, int fileID) {
        super(TableType.URSCONNIDIxnID, fileID);

        this.IxnID = IxnID;
        this.ConnID = ConnID;
    }

    public String getConnID() {
        return ConnID;
    }

    String getIxnID() {
        return IxnID;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, getConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, getIxnID()));
        return true;
    }
}
