/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author kvoroshi
 */
public interface DBAccessor {

    public void runQuery(String query);

    public PreparedStatement GetStatement(int statementId);

    public void SubmitStatement(int statementId) throws SQLException;

    public void Close(boolean shouldAnalyze);
}
