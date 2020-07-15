/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.inquirer.DatabaseConnector;
import com.myutils.logbrowser.inquirer.RuntimeInterruptException;
import com.myutils.logbrowser.inquirer.inquirer;
import javax.swing.SwingWorker;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author ssydoruk
 */
public abstract class MySwingWorker<T, V> extends SwingWorker<T, V> {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private Thread threadID = null;

    public Thread getThreadID() {
        return threadID;
    }

    protected abstract T myDoInBackground() throws Exception;

    @Override
    protected T doInBackground() throws Exception {
        threadID = Thread.currentThread();

        try {
            return myDoInBackground();
        } catch (InterruptedException interruptedException) {
            inquirer.logger.info("Interrupted!!!");
//                        Thread.currentThread().interrupt();//preserve the message
            return null;//Stop doing whatever I am doing and terminate

        } catch (RuntimeInterruptException e) {
            inquirer.logger.info("Interrupted while sorting!!!");
            return null;
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
        return null;
    }

    boolean myCancel(boolean mayInterruptIfRunning) {
        DatabaseConnector.cancelCurrent();
        cancel(mayInterruptIfRunning);
        if (isDone()) {
            return true;
        }

        try {
            Thread.sleep(150);

            /*  
            may consider implementing this
            
            from https://stackoverflow.com/questions/671049/how-do-you-kill-a-thread-in-java
            
            Thread f = <A thread to be stopped>
            Method m = Thread.class.getDeclaredMethod( "stop0" , new Class[]{Object.class} );
            m.setAccessible( true );
            m.invoke( f , new ThreadDeath() );
            
             */
            if (!isDone()) {
                inquirer.logger.info("Thread not done; killing");
                threadID.stop();
            }

        } catch (InterruptedException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        }
        return true;
    }

}
