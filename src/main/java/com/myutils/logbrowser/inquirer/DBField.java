/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

/**
 *
 * @author ssydoruk
 */
public class DBField {

    private String screenLabel;
    private String dbField;
    private String dbOutField;
    private String refTable;
    public DBField(String screenLabel, String dbField, String dbOutField, String refTable) {
        this.screenLabel = screenLabel;
        this.dbField = dbField;
        this.dbOutField = dbOutField;
        this.refTable = refTable;
    }

    public String getScreenLabel() {
        return screenLabel;
    }

    public String getDbField() {
        return dbField;
    }

    public String getDbOutField() {
        return dbOutField;
    }

    public String getRefTable() {
        return refTable;
    }


}
