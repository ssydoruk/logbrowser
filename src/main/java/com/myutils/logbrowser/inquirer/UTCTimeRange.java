/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.Pair;

/**
 *
 * @author Stepan
 */
public class UTCTimeRange extends Pair<Long, Long> {

    public UTCTimeRange(Long key, Long value) {
        super(key, value);
    }

    public UTCTimeRange(int key, int value) {
        super(new Long(key), new Long(value));
    }

    public UTCTimeRange() {
        super(new Long(0), new Long(0));
    }

    public void setStart(Long t) {
        setKey(t);
    }

    public void setEnd(Long t) {
        setValue(t);
    }

    public Long getStart() {
        return getKey();
    }

    public Long getEnd() {
        return getValue();
    }
}
