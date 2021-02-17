/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author ssydoruk
 */
public interface DBAccessor {

    void runQuery(String query);

    PreparedStatement GetStatement(int statementId);

    void SubmitStatement(int statementId) throws SQLException;

    void Close(boolean shouldAnalyze);
}
