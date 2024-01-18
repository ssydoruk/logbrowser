package com.myutils.logbrowser.inquirer;

import java.sql.SQLException;

public interface IGetAllProc {
    FullTableColors getAll(QueryDialog qd, AllInteractionsSettings settings) throws Exception;
}
