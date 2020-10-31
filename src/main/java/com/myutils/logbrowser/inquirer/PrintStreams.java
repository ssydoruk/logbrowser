/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.inquirer.OutputSpecFormatter.Parameter;
import com.myutils.logbrowser.inquirer.gui.TabResultDataModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ssydoruk
 */
public class PrintStreams {

    private int nrows;

    private boolean isAggregate;
    private boolean blockFilePrint;
    private String queryName = null;
    private String searchString = null;
    private TabResultDataModel tabDataModel;
    ArrayList<String> memStorage = null;
    ArrayList<PrintStreamStorage> shortStreams = new ArrayList<>(2);
    ArrayList<PrintStreamStorage> fullStreams = new ArrayList<>(1);

    public PrintStreams() {
        this.blockFilePrint = false;
        this.tabDataModel = new TabResultDataModel();
        nrows = 0;
    }

    public PrintStreams(InquirerCfg cfg) throws FileNotFoundException {
        this();
        if (cfg.isSaveFileLong()) {
            String fileName = cfg.getFileNameLong();
            inquirer.logger.info("Long output goes to " + fileName);
            addFullStream(fileName, -1, cfg.isAddShortToFull());
        }
        if (cfg.isSaveFileShort()) {
            String fileName = cfg.getFileNameShort();
            inquirer.logger.info("Short output goes to " + fileName);
            addShortStream(fileName, -1);
        }
        tabDataModel.setFullFileNames(getFullStreamsFileNames());
        tabDataModel.setShortFileNames(getShortStreamsFileNames());
        tabDataModel.setShortAbsoluteFileNames(getShortStreamsFullFileNames());
    }

    public String getQueryName() {
        return queryName;
    }

    public String getSearchString() {
        return searchString;
    }

    private void printShort(String shorRec, ArrayList<PrintStreamStorage> streams, int i) {
        if (!blockFilePrint) {
            for (PrintStreamStorage entry : streams) {
                if (entry.isAddShort()) {
                    entry.print(shorRec + "\n", i);
                }
            }
        }
        if (memStorage != null) {
            memStorage.add(shorRec);
        }
    }

    void printSummary(String reportSummary) {
        if (!reportSummary.endsWith("\n")) {
            reportSummary += "\n";
        }
        int lines = StringUtils.countMatches(reportSummary, "\n");

        for (PrintStreamStorage ss : shortStreams) {
            ss.print(reportSummary, lines);
        }
        for (PrintStreamStorage ss : fullStreams) {
            ss.print(reportSummary, lines);
        }
    }

    private ArrayList<String> getFileNames(ArrayList<PrintStreamStorage> streams) {
        ArrayList<String> ret = null;
        if (streams.size() > 0) {
            ret = new ArrayList<>(streams.size());
            for (PrintStreamStorage pss : streams) {
                String fileName = pss.getFileName();
                if (fileName != null && fileName.length() > 0) {
                    ret.add(pss.getFileName());
                }
            }
            if (ret.isEmpty()) {
                return null;
            }
        }
        return ret;
    }

    private ArrayList<String> getFullFileNames(ArrayList<PrintStreamStorage> streams) {
        ArrayList<String> ret = null;
        if (streams.size() > 0) {
            ret = new ArrayList<>(streams.size());
            for (PrintStreamStorage pss : streams) {
                File file = pss.getFile();
                if (file != null) {
                    ret.add(file.getAbsolutePath());
                }
            }
            if (ret.isEmpty()) {
                return null;
            }
        }
        return ret;
    }

    final public ArrayList<String> getFullStreamsFileNames() {
        return getFileNames(fullStreams);
    }

    final public ArrayList<String> getShortStreamsFileNames() {
        return getFileNames(shortStreams);
    }

    final public ArrayList<String> getShortStreamsFullFileNames() {
        return getFullFileNames(shortStreams);
    }

    void addField(Parameter param, String s) {
        tabDataModel.addCell(param, s);
    }

    void addField(String title, String s) {
        tabDataModel.addCell(title, s);
    }

    public TabResultDataModel getTabDataModel() {
        return tabDataModel;
    }

    TabResultDataModel.TableRow newRow() {
        return tabDataModel.addRow();
    }

    TabResultDataModel.TableRow currentRow() {
        return tabDataModel.getCurrentRow();
    }

    void newRow(MsgType GetType) {
        TabResultDataModel.TableRow newRow = newRow();
        newRow.setType(GetType);
        addField("Type", GetType.toString());
    }

    public void addMemoryPrint() {
        memStorage = new ArrayList<>();
    }

    public void printMemConsole() {
        if (memStorage != null) {
            for (String string : memStorage) {
                System.out.println(string);
            }
        }
    }

    void newRow(ILogRecord record) {
        TabResultDataModel.TableRow newRow = newRow();
        newRow.setRecord(record);
        newRow.setType(record.GetType());
        newRow.setFileInfo(record.GetFileName(),
                record.GetFileBytes(),
                record.GetLine(),
                record.GetFileOffset());
        newRow.setFileID(record.GetFileId());
        addField("Type", record.GetType().toString());
    }

    void setAggregate(boolean b) {
        isAggregate = b;
        this.tabDataModel.setAggregate(b);
    }

    void setNoFiles() {
        blockFilePrint = true;
    }

    void setQueryName(String name) {
        this.queryName = name;
    }

    void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    final void addFullStream(String fileName, int MaxRecords, boolean addShort) throws FileNotFoundException {
        fullStreams.add(new PrintStreamStorage(fileName, MaxRecords, addShort, this));
    }

    final void addShortStream(String fileName, int MaxRecords) throws FileNotFoundException {
        shortStreams.add(new PrintStreamStorage(fileName, MaxRecords, false, this));
    }

    void addShortStream(PrintStream out, int MaxRecords) {
        shortStreams.add(new PrintStreamStorage(out, MaxRecords, this));
    }

    public void closeStreams() {
        for (PrintStreamStorage shortStream : shortStreams) {
            shortStream.close();
        }
        for (PrintStreamStorage shortStream : fullStreams) {
            shortStream.close();
        }
    }

    /*this prints to short stream only */
    void print(String format, ArrayList<PrintStreamStorage> streams, int lines) {
        for (PrintStreamStorage entry : streams) {
            entry.print(format, lines);
        }
    }

    public int getNrows() {
        return nrows;
    }

    void println(StringBuilder format) {
        println(format.toString());
    }

    void println(String format) {
        print(format + "\n");
        nrows++;
    }

    void print(String format) {
        int lines = StringUtils.countMatches(format, "\n");
        print(format, shortStreams, lines);
    }

    void printFull(String format, String shorRec, ArrayList<Pattern> filters) {
        int lines;
        printShort(shorRec, fullStreams, 1);
        if (!blockFilePrint) {
            if (filters == null) {
                lines = StringUtils.countMatches(format, "\n");
                print(format, fullStreams, lines);
            } else {
                for (String string : StringUtils.splitPreserveAllTokens(format, '\n')) {
                    boolean print = true;
                    for (Pattern filter : filters) {
                        if (filter.matcher(string).find()) {
                            print = false;
                            break;
                        }
                    }
                    if (print) {
                        print(string + "\n", fullStreams, 1);
                    }
                }
            }
        }
    }

    void OpenNotepad(String batFileName) {
        BufferedWriter out = null;
        try {
            FileWriter fstream = new FileWriter(batFileName, false); //true tells to append data.
            out = new BufferedWriter(fstream);

            for (PrintStreamStorage ss : shortStreams) {
                String fileName = ss.getFileName();
                if (fileName != null && fileName.length() > 0) {
                    out.write("npp_open " + fileName + "\n");
                }
            }
            for (PrintStreamStorage ss : fullStreams) {
                String fileName = ss.getFileName();
                if (fileName != null && fileName.length() > 0) {
                    out.write("npp_open " + fileName + "\n");
                }
            }

        } catch (IOException e) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
            }
        }

    }

    interface PrintStorage {

    }

}
