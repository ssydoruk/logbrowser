package com.myutils.logbrowser.inquirer;

import java.sql.SQLException;

public interface ILogRecordFormatter {

    public boolean IsLayoutRequired();

    public void Layout(ILogRecord loggedEvent) throws SQLException;

    public void ProcessLayout();

    public void Close() throws Exception;

    public void reset();

    public void Print(ILogRecord record, PrintStreams ps, IQueryResults qr);

    public void refreshFormatter() throws Exception;
}
