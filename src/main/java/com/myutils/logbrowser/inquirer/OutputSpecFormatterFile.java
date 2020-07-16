/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.util.HashSet;

/**
 *
 * @author ssydoruk
 */
public class OutputSpecFormatterFile extends OutputSpecFormatter {

    public OutputSpecFormatterFile(XmlCfg cfg, boolean isLongFileNameEnabled, HashSet<String> components) throws Exception {
        super(cfg, isLongFileNameEnabled, components);
    }

    @Override
    public void Print(ILogRecord record, PrintStreams ps, IQueryResults qr) {
        RecordLayout lo = getLayout(record.GetType());

        String shortRec = null;
        if (lo != null) {
            shortRec = lo.PrintRecordFile(record, ps, qr);
        } else {
            super.Print(record, ps, qr);
        }
    }

}
