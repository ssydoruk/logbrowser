/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.myutils.logbrowser.inquirer.inquirer;

/**
 *
 * @author ssydoruk
 */
public class WindowHandleGetter {

    private static final Logger log = LogManager.getLogger(WindowHandleGetter.class);
    private final Frame rootFrame;

    protected WindowHandleGetter(Frame rootFrame) {
        this.rootFrame = rootFrame;
    }

    protected long getWindowId() {

        try {
            Frame frame = rootFrame;

            // The reflection code below does the same as this
            // long handle = frame.getPeer() != null ? ((WComponentPeer) frame.getPeer()).getHWnd() : 0;
            Object wComponentPeer = invokeMethod(frame, "getPeer");

            Long hwnd = (Long) invokeMethod(wComponentPeer, "getHWnd");

            return hwnd;

        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException("Error getting window handle", ex);

        }

        return 0;
    }

    protected Object invokeMethod(Object o, String methodName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Class c = o.getClass();
        for (Method m : c.getMethods()) {
            if (m.getName().equals(methodName)) {
                Object ret = m.invoke(o);
                return ret;
            }
        }
        throw new RuntimeException("Could not find method named '" + methodName + "' on class " + c);

    }
}
