/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public interface DBAccessor {

    void runQuery(String query);

    void Close(boolean shouldAnalyze);
}
