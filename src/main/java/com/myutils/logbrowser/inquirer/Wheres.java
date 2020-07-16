/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import java.util.ArrayList;

/**
 *
 * @author ssydoruk
 */
class Wheres {

    ArrayList<Where> wh = new ArrayList<>();

    void addWhere(Wheres addWh) {
        if (addWh != null) {
            wh.add(new Where(addWh, null));
        }
    }

    public Where addWhere(String st, String andOr) {

        if (st != null && st.length() > 0) {
            Where ret = new Where(st, andOr);
            wh.add(ret);
            return ret;
        }
        return null;
    }

    public void addWhere(String timeField, UTCTimeRange timeRange, String andOr) {
        addWhere(null, timeField, timeRange, andOr);
    }

    public void addWhere(String tab, String timeField, UTCTimeRange timeRange, String andOr) {
        if (timeRange != null && timeField != null) {
            addWhere(((tab != null) ? (tab + ".") : "") + timeField + " between " + timeRange.getStart() + " and " + timeRange.getEnd(), "AND");
        }
    }

    public void addWhere(Wheres _wh, String andOr) {
        if (_wh != null) {
            wh.add(new Where(_wh, andOr));
        }
    }

    public void addWhere(String st) {
        inquirer.logger.debug("AddWhere: [" + ((st == null || st.isEmpty()) ? "null" : st));

        if (st != null && st.length() > 0) {
            wh.add(new Where(st, null));
        }
    }

    public boolean isEmpty() {
        return wh.isEmpty();
    }

    public String makeWhere(String leadAndOr, boolean addWhere) {
        String allWhere = makeWhere(false);

        if (allWhere != null && !allWhere.isEmpty()) {
            return leadAndOr + " " + allWhere;
        }
        return "";
    }

    public String makeWhere(String firstPart, String andOr, boolean addWhere) {
        StringBuilder ret = new StringBuilder(256);
        String allWhere = makeWhere(false);

        if (firstPart != null && firstPart.length() > 0) {
            ret.append(firstPart);
        }
        if (ret.length() > 0) {
            if (allWhere != null && allWhere.length() > 0) {
                ret.append(" ").append(andOr).append(" ");
            }
        }
        if (allWhere != null) {
            ret.append(allWhere);
        }
        if (addWhere && ret.length() > 0) {
            return " WHERE " + ret;
        } else {
            return ret.toString();
        }
    }

    String makeWhere(boolean addWhere) {
        StringBuilder ret = new StringBuilder(256);
        int idx = 0;

        if (wh.size() > 0) {
            Where w = wh.get(idx);
            String s = w.getWh();
            if (s != null && !s.isEmpty()) {
                if (addWhere) {
                    ret.append(" WHERE ");
                }
                ret.append(" ").append(s);
            }
            idx++;
            if (idx < wh.size()) {
//                ret.append("(");
                for (; idx < wh.size(); idx++) {
                    w = wh.get(idx);
                    s = w.getWh();
                    if (s != null && !s.isEmpty()) {
                        if (ret.length() > 0) {
                            ret.append("\n").append((w.andOr != null) ? w.andOr : "AND");
                        }

                        ret.append("\n").append(s);
                    }
                }
//                ret.append(")\n");
            }
        }

        return ret.toString();
    }

    class Where {

        private String st = null;
        String andOr = null;
        private Wheres theWh = null;

        Where(String st, String andOr) {
            this.st = st;
            this.andOr = andOr;
        }

        Where(Wheres _wh, String andOr) {
            this.theWh = _wh;
            this.andOr = andOr;
        }

        public String getWh() {
            if (st != null) {
                return st;
            } else if (theWh != null) {
                String makeWhere = theWh.makeWhere(false).trim();
                return (makeWhere != null && makeWhere.length() > 0) ? "( " + makeWhere + " )" : null;
            }
            return "";
        }
    }
}
