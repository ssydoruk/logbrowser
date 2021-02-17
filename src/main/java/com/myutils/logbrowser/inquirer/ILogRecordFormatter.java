package com.myutils.logbrowser.inquirer;

import java.sql.SQLException;

public interface ILogRecordFormatter {

    boolean IsLayoutRequired();

    void Layout(ILogRecord loggedEvent) throws SQLException;

    void ProcessLayout();

    void Close() throws Exception;

    void reset();

    void Print(ILogRecord record, PrintStreams ps, IQueryResults qr);

    void refreshFormatter() throws Exception;
}
