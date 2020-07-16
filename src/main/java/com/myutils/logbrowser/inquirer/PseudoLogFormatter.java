package com.myutils.logbrowser.inquirer;

import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PseudoLogFormatter implements ILogRecordFormatter {

    protected String m_outputFileName;
    protected FileWriter m_fileWriter;

    private int m_lineCount;
    private final String m_markText = "***\n";
    private int m_markedLine;
    private int m_actualLogMarkLine;
    private String m_actualLogFileName;
    private final boolean m_navigate;
    private final HashSet<String> m_components;

    public PseudoLogFormatter(String fileName, boolean navigate, HashSet<String> components) throws Exception {
        m_lineCount = 0;
        m_markedLine = -1;
        m_navigate = navigate;
        m_actualLogFileName = null;
        m_actualLogMarkLine = -1;
        m_components = components;

        if (fileName == null || fileName.isEmpty()) {
            // generate fileName
            DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            Date date = new Date();
            fileName = dateFormat.format(date) + ".pseudo";

        }
        m_outputFileName = fileName;

        // open file writer
        m_fileWriter = new FileWriter(fileName);
    }

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
    public void Print(ILogRecord loggedEvent, PrintStreams ps, IQueryResults qr) {
        try {
            String comp = loggedEvent.GetField("comp").toUpperCase();
            if (!m_components.contains(comp)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        try {
            MsgType eventType = loggedEvent.GetType();
            if (eventType != MsgType.SIP && eventType != MsgType.TLIB
                    && eventType != MsgType.JSON && eventType != MsgType.PROXY) {
                return;
            }

            // get file, offset, bytes
            LogFile logFileName = loggedEvent.GetFileName();
//            logFileName = PathManager.GetAbsolutePath(logFileName);
            long fileOffset = loggedEvent.GetFileOffset();
            int fileBytes = loggedEvent.GetFileBytes();
            int fileID = loggedEvent.GetFileId();

            byte buffer[] = InquirerFileIo.GetLogBytes(logFileName, fileOffset, fileBytes);
            int eventLineCount = 0;
            int upperLimit = fileBytes < buffer.length ? fileBytes : buffer.length;
            for (int j = 0; j < upperLimit; j++) {
                if (buffer[j] == '\n') {
                    eventLineCount++;
                }
            }
            if (IsMarkWithin(loggedEvent, eventLineCount)) {
                m_fileWriter.write(m_markText.toCharArray(), 0, 4);
                m_markedLine = ++m_lineCount;
            }
            WriteNavigationInfo(loggedEvent);
            WriteJsonHeader(loggedEvent);

            if (eventType == MsgType.PROXY) {
                PrintProxied(loggedEvent);
                return;
            }

            m_lineCount += eventLineCount;
//            m_fileWriter.write(buffer,0,fileBytes);
            m_fileWriter.write("\n\n");
            m_lineCount += 2;
        } catch (Exception e) {
            inquirer.logger.error(e);
            // ignore exceptions for now
        }

    }

    @Override
    public void Close() throws Exception {
        if (m_markedLine == -1) {
            inquirer.logger.debug(m_outputFileName + "(1): Pseudo-Log Created");
        } else {
            inquirer.logger.debug(m_outputFileName
                    + "("
                    + Integer.toString(m_markedLine)
                    + "): current message in the created pseudo-log");
        }
        m_fileWriter.close();
    }

    public void ApplyMark(String mark) {
        Pattern markPosPattern = Pattern.compile("^([\\w\\.\\\\\\:\\-]+)\\((\\d+)\\)$");
        Matcher matcher = markPosPattern.matcher(mark);
        if (matcher.find()) {
            m_actualLogMarkLine = Integer.parseInt(matcher.group(2));
            m_actualLogFileName = matcher.group(1);
        }

    }

    private boolean IsMarkWithin(ILogRecord record, int lineCount) throws Exception {
        String fileName = record.GetFileName().getFileName();
        int fileLine = record.GetLine();

        boolean result = fileName.equalsIgnoreCase(m_actualLogFileName)
                && fileLine <= m_actualLogMarkLine
                && fileLine + lineCount >= m_actualLogMarkLine;

        return result;
    }

    private void WriteNavigationInfo(ILogRecord record) throws Exception {
        if (m_navigate) {
            String fileName = record.GetFileName().getFileName();
            fileName = PathManager.GetAbsolutePath(fileName);
            int fileLine = record.GetLine();
            m_fileWriter.write(fileName + "(" + fileLine + ")\n");
            m_lineCount++;
        }
    }

    private void WriteJsonHeader(ILogRecord record) throws Exception {
        if (record.GetType() != MsgType.JSON) {
            return;
        }

        String time = record.GetTime();
//        String type = record.GetField("type");
//        m_fileWriter.write(time + ": " + type +"\n");
        m_lineCount++;

    }

    private void PrintProxied(ILogRecord record) throws Exception {
        String time = record.GetTime();
        if (time.length() > 12) {
            time = time.substring(11);
        }
        String name = record.GetField("name");
        String dest = record.GetField("destination");
        m_fileWriter.write(time + ": Trc 04542 " + name + " sent to (" + (dest == "" ? "Unknown" : dest) + ")\n");
        m_lineCount++;
        m_fileWriter.write("message " + name + "\n");
        m_lineCount++;

        if (!record.GetField("connid").isEmpty()) {
            m_fileWriter.write("\tAttributeConnID\t" + (String) record.GetField("connid") + "\n");
            m_lineCount++;
        }

        if (!record.GetField("refid").isEmpty()) {
            m_fileWriter.write("\tAttributeReferenceID\t" + (String) record.GetField("refid") + "\n");
            m_lineCount++;
        }

        if (!record.GetField("thisdn").isEmpty()) {
            m_fileWriter.write("\tAttributeThisDN\t" + (String) record.GetField("thisdn") + "\n");
            m_lineCount++;
        }
        if (!record.GetField("otherdn").isEmpty()) {
            m_fileWriter.write("\tAttributeOtherDN\t" + (String) record.GetField("otherdn") + "\n");
            m_lineCount++;
        }

        m_fileWriter.write("\n\n");
        m_lineCount += 2;
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void refreshFormatter() throws Exception {

    }

}
