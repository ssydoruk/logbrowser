/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.inquirer;

/**
 *
 * @author ssydo
 */
public class AllProcSettings {
    private IGetAllProc proc;
    private AllInteractionsSettings settings;

    public AllProcSettings(IGetAllProc proc, AllInteractionsSettings settings) {
        this.proc = proc;
        this.settings = settings;
    }

    public IGetAllProc getProc() {
        return proc;
    }

    public AllInteractionsSettings getSettings() {
        return settings;
    }
    
}
