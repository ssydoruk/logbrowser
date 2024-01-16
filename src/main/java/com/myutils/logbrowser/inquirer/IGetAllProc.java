package com.myutils.logbrowser.inquirer;

import java.sql.SQLException;

public interface IGetAllProc {
    FullTableColors getAll(QueryDialog qd) throws Exception;
}
