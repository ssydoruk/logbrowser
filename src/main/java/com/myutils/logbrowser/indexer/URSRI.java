/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;

public class URSRI extends URSRIBase {

    public URSRI(TableType t, ArrayList messageLines, int fileID) {
        super(t, messageLines, fileID);
    }

    URSRI( int fileID) {
        super(TableType.URSRI, fileID);
    }

}
