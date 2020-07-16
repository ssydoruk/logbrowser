/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.util.ArrayList;

/**
 *
 * @author ssydoruk
 */
public abstract class IReportConfigItem {

    private IReportConfigItem parent = null;
    private String name;
    private ArrayList<IReportConfigItem> children = new ArrayList<>();
    public IReportConfigItem(IReportConfigItem _parent, String name) {
        parent = _parent;
        this.name = name;
    }

    public void addChild(IReportConfigItem iReportConfigItem) {
        children.add(iReportConfigItem);
    }

    public IReportConfigItem getParent() {
        return parent;
    }

    public void setParent(IReportConfigItem parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
