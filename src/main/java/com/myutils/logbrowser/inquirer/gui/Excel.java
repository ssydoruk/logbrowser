/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the excelInstance.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.myutils.logbrowser.inquirer.inquirer;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author ssydoruk
 */
public class Excel {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private static Dispatch openExcel(String fileName) throws IOException {
        ActiveXComponent xl = getExcel().excel;

//get "Workbooks" 1 property from "Application" object
        Dispatch workbooks = xl.getProperty("Workbooks").toDispatch();
//call method "Open" with filepath param on "Workbooks" object and save "Workbook" object

        inquirer.logger.info("Opening in excel [" + fileName + "]");

        Variant t = new Variant(true);
        Variant f = new Variant(false);

        Variant workbook = Dispatch.call(workbooks, "OpenText", new Variant(fileName), new Variant(xlWindows),
                new Variant(1),
                new Variant(xlDelimited),
                new Variant(xlTextQualifierNone),
                t,
                f,
                f,
                t
        );
        access(workbook);
//        inquirer.logger.info("wb" + workbook.toString());

        Dispatch actWorkbook = xl.getProperty("ActiveWorkbook").toDispatch();
        access(actWorkbook);

//        Dispatch wb = workbook.getDispatch();
        //get "Worksheets" property from "Workbook" object
        return Dispatch.get(actWorkbook, "Worksheets").toDispatch();

    }

    static void aggregateEdit(ArrayList<String> shortFileNames) throws IOException {
        Dispatch sheets = openExcel(shortFileNames.get(0));
        Dispatch sheet = Dispatch.call(sheets, "Item", new Variant(1)).toDispatch();

        Dispatch range = Dispatch.invoke(sheet, "Range", Dispatch.Get, new Object[]{"A1:A5"}, new int[1]).toDispatch();

        Variant r = Dispatch.call(range, "AutoFilter");
        Dispatch range1 = Dispatch.call(sheet, "Columns", new Variant(1)).toDispatch();

        Variant r1 = Dispatch.invoke(range1, "NumberFormat", Dispatch.Put, new Object[]{"mm/dd/yyyy hh:mm:ss"}, new int[1]);

//        range1.s
//     r = Dispatch.call(range1, "NumberFormat",  new Variant("mm/dd/yyyy hh:mm:ss"));
        getExcel().excel.setProperty("Visible", true);

        inquirer.logger.info("Loaded excel");
    }

//        https://msdn.microsoft.com/en-us/library/office/aa221100(v=office.11).aspx
    private static final int xlWindows = 2;
    private static final int xlDelimited = 1;
    private static final int xlTextQualifierNone = -4142;

    static void close() {
        if (excelInstance != null) {
            Dispatch workbooks = excelInstance.excel.getProperty("Workbooks").toDispatch();
            inquirer.logger.info("result of Workbooks: " + workbooks);

            Dispatch.call(workbooks, "Close");

            excelInstance.excel.setProperty("DisplayAlerts", false);
            Variant invoke = excelInstance.excel.invoke("Quit", new Variant[]{});
            inquirer.logger.info("result of close: " + invoke);

            /*
            This is to terminate excel application completely 
            https://sourceforge.net/p/jacob-project/discussion/375946/thread/01491160/
            
            o successfully run Excel:
Upgrade to at least Jacob 1.13.  There have been a number of stability improvements since 1.9.  I can't say for sure that it will not work with 1.9, but all my current testing has been done with 1.13 and 1.14 Mx. 
Before you start to work with Excel, use initMTA(), NOT initSTA().  InitSTA() still causes JVM crashes.
When you are done working with an instance of Excel, call COMThread.Release().  This is very important, since Jacob is really wrapping a C++ library, and this is the only way that it will clean up after itself propertly in all cases.  I recommend the following structure:
ComThread.InitMTA();
try
{
  //Make sure all Dispatch variables and Variants do NOT leave this scope!!!
  ...
}
finally
{
  ComThread.Release();
}
Do NOT use com.jacob.autogc.  It kind of works most of the time, but it causes JVM crashes occasionally.  It is tempting to try to get this to work, but you don't need it.  Calling ComThread.Release() cleans up memory perfectly.
If you need help with a strategy to make Excel work reliably without user input, post back to the Help thread under a different heading.
 
             */
        }
    }

    public static void reportEdit(ArrayList<String> shortFileNames) throws IOException {
        Dispatch sheets = openExcel(shortFileNames.get(0));
        access(sheets);
        Dispatch sheet = Dispatch.call(sheets, "Item", new Variant(1)).toDispatch();
        access(sheet);

        Dispatch range = Dispatch.invoke(sheet, "Range", Dispatch.Get, new Object[]{"A1"}, new int[1]).toDispatch();
        access(range);
        Variant r = Dispatch.call(range, "AutoFilter");
        access(r);
        access(Dispatch.call(range, "Count"));
        Dispatch range1 = Dispatch.call(sheet, "Columns", new Variant(2)).toDispatch();
        access(range1);

        Variant r1 = Dispatch.invoke(range1, "NumberFormat", Dispatch.Put, new Object[]{"mm/dd/yyyy hh:mm:ss"}, new int[1]);
        access(sheet);

//        range1.s
//     r = Dispatch.call(range1, "NumberFormat",  new Variant("mm/dd/yyyy hh:mm:ss"));
        getExcel().excel.setProperty("Visible", true);

        inquirer.logger.info("Loaded excel");
    }

    private static void access(Dispatch sheets) {
        inquirer.logger.info((sheets == null) ? "[NULL]" : sheets.toString());
    }

    private static void access(Variant r) {
        inquirer.logger.info((r == null) ? "[NULL]" : r.toString());
    }

    private ActiveXComponent excel;

    public Excel() throws IOException {
        InitDLL();
    }

    private static Excel excelInstance = null;

    static public Excel getExcel() throws IOException {
        if (excelInstance == null) {
            excelInstance = new Excel();
        }
        return excelInstance;
    }

    private void InitDLL() throws IOException {
        WindowsSystemUtility.InitDLLActiveX();
        excel = new ActiveXComponent("Excel.Application");

    }

}
