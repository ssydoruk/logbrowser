/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * @author Stepan
 */
class PrintStreamStorage extends PrintStreams {

    private final PrintStream ps;
    private final String FileName;
    private final PrintStreams outer;
    private int MaxRecords;
    private int linesPrinted = 0;
    private int recsPrinted = 0;
    private boolean addShort = false;
    private File file;

    PrintStreamStorage(PrintStream out, int MaxRecords, final PrintStreams outer) {
        this.outer = outer;
        ps = out;
        this.MaxRecords = MaxRecords;
        FileName = null;
    }

    public PrintStreamStorage(String fileName, int MaxRecords, boolean _addShort, final PrintStreams outer) throws FileNotFoundException {
        this.outer = outer;
        file = new File(fileName);
        this.ps = new PrintStream(new FileOutputStream(file));
        this.MaxRecords = adjustMaxRecords(MaxRecords);
        this.FileName = fileName;
        this.addShort = _addShort;
    }

    public boolean isAddShort() {
        return addShort;
    }

    public void setAddShort(boolean addShort) {
        this.addShort = addShort;
    }

    public PrintStream getPs() {
        return ps;
    }

    public int getMaxRecords() {
        return MaxRecords;
    }

    public String getFileName() {
        return FileName;
    }

    private int adjustMaxRecords(int MaxRecords) {
        return (MaxRecords <= 0) ? Integer.MAX_VALUE : MaxRecords;
    }

    public void close() {
        if (!ps.equals(System.out)) {
            ps.close();
        }
        inquirer.logger.info("Printed " + linesPrinted + " lines (" + recsPrinted + " records) to " + ((FileName == null) ? "stdout" : FileName));
    }

    void print(String format, int lines) {
        if (getMaxRecords() > 0) {
            ps.print(format);
            MaxRecords -= lines;
            linesPrinted += lines;
            recsPrinted++;
        }
    }

    public void OpenNotepad() {
        if (FileName != null) {
            System.out.println("npp_open " + FileName);
        }
    }

    public File getFile() {
        return file;
    }

}
