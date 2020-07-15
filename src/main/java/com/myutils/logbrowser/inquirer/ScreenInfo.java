/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.inquirer.gui.ReportFrame;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import javax.swing.JFrame;

/**
 *
 * @author ssydoruk
 */
public class ScreenInfo {

    public static int getScreenID(Window jf) {
        int scrID = 1;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        for (int i = 0; i < gd.length; i++) {
            GraphicsConfiguration gc = gd[i].getDefaultConfiguration();
            Rectangle r = gc.getBounds();
            if (r.contains(jf.getLocation())) {
                scrID = i + 1;
            }
        }
        return scrID;
    }

    public static Dimension getScreenDimension(int scrID) {
        Dimension d = new Dimension(0, 0);
        if (scrID > 0) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle maximumWindowBounds = ge.getMaximumWindowBounds();
            d.setSize(maximumWindowBounds.width, maximumWindowBounds.height);

//            DisplayMode mode = ge.getScreenDevices()[scrID - 1].getDisplayMode();
//            d.setSize(mode.getWidth(), mode.getHeight());
        }
        return d;
    }

    public static int getScreenWidth(int scrID) {
        Dimension d = getScreenDimension(scrID);
        return d.width;
    }

    public static int getScreenHeight(int scrID) {
        Dimension d = getScreenDimension(scrID);
        return d.height;
    }

    public static void CenterWindow(Window aThis) {
        int screenID = ScreenInfo.getScreenID(aThis);
//        inquirer.logger.info("Centering " + aThis.toString() + "; screen: " + screenID);
//        aThis.setLocationRelativeTo(null);
        aThis.setLocation((ScreenInfo.getScreenWidth(screenID) - aThis.getWidth()) / 2,
                (ScreenInfo.getScreenHeight(screenID) - aThis.getHeight()) / 2);

    }

    public static void CenterWindowMaxWidth(ReportFrame aThis) {
        int screenID = ScreenInfo.getScreenID(aThis);
//        aThis.setLocationRelativeTo(null);
        aThis.setLocation(0,
                (ScreenInfo.getScreenHeight(screenID) - aThis.getHeight()));
        aThis.setSize(ScreenInfo.getScreenWidth(screenID), aThis.getHeight());
    }

    public static void CenterWindowTopMaxWidth(JFrame aThis) {
        int screenID = ScreenInfo.getScreenID(aThis);
//        aThis.setLocationRelativeTo(null);
        aThis.setLocation(0,
                0);
        aThis.setSize(ScreenInfo.getScreenWidth(screenID), ScreenInfo.getScreenHeight(screenID) / 2);
    }

    static void setVisible(Window frm, boolean b) {
        setVisible(null, frm, b);
    }

    static void CenterWindowMaxWidth(Window parent, Window frm) {
//        Rectangle windowScreenBounds = getWindowScreenBounds(parent);
//        if (windowScreenBounds != null) {
//            frm.setLocation(0,
//                    windowScreenBounds.height);
////            frm.setSize(windowScreenBounds.height/2, windowScreenBounds.width);
////            frm.setSize();
//        }
    }

    static private Rectangle getWindowScreenBounds(Window parent) {
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            if (parent.getLocation().getX() >= bounds.getMinX()
                    && parent.getLocation().getX() < bounds.getMaxX()
                    && parent.getLocation().getY() >= bounds.getMinY()
                    && parent.getLocation().getY() < bounds.getMaxY()) {
//                    frm.setLocationRelativeTo(parent);
                return bounds;
            }
        }
        return null;
    }

    public static void setVisible(Component parent, Window frm, boolean b) {

//        if (parent != null) {
//            GraphicsDevice myDevice = parent.getGraphicsConfiguration().getDevice();
//            inquirer.logger.info("myDevice before:" + myDevice);
//            for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
//                if (parent.getLocation().getX() >= gd.getDefaultConfiguration().getBounds().getMinX()
//                        && parent.getLocation().getX() < gd.getDefaultConfiguration().getBounds().getMaxX()
//                        && parent.getLocation().getY() >= gd.getDefaultConfiguration().getBounds().getMinY()
//                        && parent.getLocation().getY() < gd.getDefaultConfiguration().getBounds().getMaxY()) {
////                    frm.setLocationRelativeTo(parent);
//                    myDevice = gd;
//                    inquirer.logger.info("myDevice found:" + myDevice);
//                }
//            }
//        } else {
//            GraphicsDevice myDevice = frm.getGraphicsConfiguration().getDevice();
//            inquirer.logger.info("myDevice before:" + myDevice);
//            for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
//                if (frm.getLocation().getX() >= gd.getDefaultConfiguration().getBounds().getMinX()
//                        && frm.getLocation().getX() < gd.getDefaultConfiguration().getBounds().getMaxX()
//                        && frm.getLocation().getY() >= gd.getDefaultConfiguration().getBounds().getMinY()
//                        && frm.getLocation().getY() < gd.getDefaultConfiguration().getBounds().getMaxY()) {
////                    frm.setLocationRelativeTo(parent);
//                    myDevice = gd;
//                    inquirer.logger.info("myDevice found:" + myDevice);
//                }
//            }
//        }
//        
        if (parent != null) {
            frm.setLocationRelativeTo(parent);
        } else {
        }

        frm.setVisible(b);
    }
}
