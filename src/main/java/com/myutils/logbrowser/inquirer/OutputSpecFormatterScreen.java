/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.util.Enumeration;
import java.util.HashSet;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author ssydoruk
 */
public class OutputSpecFormatterScreen extends OutputSpecFormatter {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    public OutputSpecFormatterScreen(XmlCfg cfg, boolean isLongFileNameEnabled, HashSet<String> components) throws Exception {
        super(cfg, isLongFileNameEnabled, components);
    }

    @Override
    public void Print(ILogRecord record, PrintStreams ps, IQueryResults qr) {

        RecordLayout lo = getLayout(record.GetType());

        if (lo != null) {
            try {
                lo.PrintRecord(record, ps, qr);
            } catch (Exception ex) {
                logger.log(org.apache.logging.log4j.Level.FATAL, ex);
            }
        } else {
            printAll(record, ps, qr);
        }
//        ps.printFull(InquirerFileIo.GetFileBytes(record) + "\n", shortRec, inquirer.getFullFilters(record.GetFileId()));
    }

    private void printAll(ILogRecord record, PrintStreams ps, IQueryResults qr) {

        ps.addField("timestamp", record.GetTime());

        Enumeration e = record.m_fieldsAll.propertyNames();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            ps.addField(key, key + ":" + record.m_fieldsAll.getProperty(key));
        }

    }
}
