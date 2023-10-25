/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

/**
 * @author ssydoruk
 */
public class DBField {

    private final String screenLabel;
    private final String fieldName;
    private final String dbOutField;
    private final String refTable;

    public DBField(String screenLabel, String dbField, String dbOutField, String refTable) {
        this.screenLabel = screenLabel;
        this.fieldName = dbField;
        this.dbOutField = dbOutField;
        this.refTable = refTable;
    }

    public String getScreenLabel() {
        return screenLabel;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDbOutField() {
        return dbOutField;
    }

    public String getRefTable() {
        return refTable;
    }

}
