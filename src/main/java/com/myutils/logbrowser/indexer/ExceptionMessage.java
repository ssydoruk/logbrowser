/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;

public class ExceptionMessage extends Message {

    private final String exception;
    private final String msg;

    ExceptionMessage(ArrayList<String> m_MessageContents, String m_msgName, String m_msg1) {
        super(TableType.WWEException);
        this.exception = m_msgName;
        this.msg = m_msg1;
    }

    String getException() {
        return exception;
    }

    String getExceptionMessage() {
        return msg;
    }

}
