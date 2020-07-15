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
public class URSCONNIDIxnID extends Message {

    private final String IxnID;

    private final String ConnID;

    public URSCONNIDIxnID(String ConnID, String IxnID) {
        super(TableType.URSCONNIDIxnID);

        this.IxnID = IxnID;
        this.ConnID = ConnID;
    }

    public String getConnID() {
        return ConnID;
    }

    String getIxnID() {
        return IxnID;
    }

}
