package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface IFillStatement {
    boolean fillStatement(PreparedStatement stmt) throws SQLException;
}
