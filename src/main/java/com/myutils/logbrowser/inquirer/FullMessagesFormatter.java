/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

/**
 * @author ssydoruk
 */
public class FullMessagesFormatter implements ILogRecordFormatter {

    @Override
    public boolean IsLayoutRequired() {
        return false;
    }

    @Override
    public void Layout(ILogRecord loggedEvent) {
    }

    @Override
    public void ProcessLayout() {
    }

    @Override
    public void Close() throws Exception {
    }

    @Override
    public void reset() {
    }

    @Override
    public void Print(ILogRecord record, PrintStreams ps, IQueryResults qr) {
        ps.println(InquirerFileIo.GetFileBytes(record.GetFileName(), record.GetFileOffset(), record.GetFileBytes()));
    }

    @Override
    public void refreshFormatter() throws Exception {
    }

}
