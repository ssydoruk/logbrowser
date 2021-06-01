/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.util.ArrayList;

/**
 * @author ssydoruk
 */
public class ReportItem extends IReportConfigItem {

    ArrayList<ReportComponent> comp = new ArrayList<>();

    public ReportItem(IReportConfigItem _parent, String name) {
        super(_parent, name);
    }

    public ArrayList<ReportComponent> getComps() {
        return comp;
    }

    public ReportComponent addComponent(String name) {
        ReportComponent rc = new ReportComponent(this, name);
        comp.add(rc);
        return rc;
    }

    class ReportComponent extends IReportConfigItem {

        ArrayList<ComponentAttribute> attrs = new ArrayList<>();

        public ReportComponent(IReportConfigItem _parent, String name) {
            super(_parent, name);
        }

        public ArrayList<ComponentAttribute> getAttrs() {
            return attrs;
        }

        ComponentAttribute addAttribute(String metric_type) {
            ComponentAttribute ca = new ComponentAttribute(this, metric_type);
            attrs.add(ca);
            return ca;
        }

    }

    class ComponentAttribute extends IReportConfigItem {

        ArrayList<String> attrValue = new ArrayList<>();
        private String name;

        public ComponentAttribute(IReportConfigItem _parent, String name) {
            super(_parent, name);
        }

        void addValue(String eval) {
            attrValue.add(eval);
        }

    }

}
