package com.myutils.logbrowser.indexer;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.logging.log4j.ThreadContext.containsKey;

public class DBTables extends ConcurrentHashMap<String, DBTable> {
    @Override
    public DBTable put(String key, DBTable value) {
        synchronized (this) {
            if (!containsKey(key))
                return super.put(key, value);
            else
                return get(key);
        }
    }
}
