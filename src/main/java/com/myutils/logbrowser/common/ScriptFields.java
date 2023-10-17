package com.myutils.logbrowser.common;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

public class ScriptFields<K, V> extends HashMap<K, V> {
    public V appendValue(K key, V value, V separator) {
        V oldValue = get(key);
        if (oldValue == null) {
            return put(key, value);
        } else {
            if (oldValue instanceof String && separator instanceof String && value instanceof String) {
                return put(key, (V) StringUtils.joinWith((String) separator, oldValue, value));
            } else
                return put(key, value);
        }
    }
}
