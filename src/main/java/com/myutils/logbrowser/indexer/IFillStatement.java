package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface IFillStatement {
    void fillStatement(PreparedStatement stmt) throws SQLException;
}
