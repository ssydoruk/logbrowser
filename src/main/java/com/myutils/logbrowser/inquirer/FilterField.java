package com.myutils.logbrowser.inquirer;

import Utils.Pair;

@SuppressWarnings("serial")
class FilterField extends Pair<String, String> {

    public FilterField(String key, String value) {
        super(key, value);
    }

    @Override
    public String toString() {
        return getKey();
    }

}
