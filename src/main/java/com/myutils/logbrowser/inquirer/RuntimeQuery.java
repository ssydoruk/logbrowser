/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author ssydoruk
 */
class RuntimeQuery {

    private final int statID;

    public RuntimeQuery(String query) throws SQLException {
        statID = DatabaseConnector.getDatabaseConnector(this).PrepareStatement(query);
    }

    public int getSingleValue(Object[] params) throws SQLException {
        PreparedStatement stat = DatabaseConnector.getDatabaseConnector(this).GetStatement(statID);

//        stat.setInt(1, 2);
        //stat.clearParameters();
        for (int i = 0; i < params.length; i++) {
            stat.setObject(i + 1, params[i]);
//            if (params[i] instanceof String) {
//                stat.setString(i, (String)params[i]);
//            }
//            else if (params[i] instanceof Integer) {
//                stat.setInt(i, (Integer)params[i]);
//            }
        }
        ResultSet rs = DatabaseConnector.getDatabaseConnector(this).getRecords(statID);
        int ret = rs.getInt(1);
        rs.close();

        return ret;
        /*PreparedStatement GetStatement
         return null;
         * */
    }
}
