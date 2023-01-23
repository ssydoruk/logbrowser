/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.common;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.graalvm.polyglot.HostAccess;

/**
 *
 * @author ssydoruk
 */
public class RecordPrintout {

    @HostAccess.Export
    public String fullMessage;
    @HostAccess.Export
    public String detailsMessage;
    @HostAccess.Export
    public String lang;

    public RecordPrintout(String fullMessage) {
        this.fullMessage = fullMessage;
        this.lang = SyntaxConstants.SYNTAX_STYLE_NONE;
    }

}
