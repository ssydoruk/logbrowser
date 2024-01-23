package com.myutils.logbrowser.common;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

public class ScriptFields<K, V> extends HashMap<K, V> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5944406558664155223L;

	public V appendValue(K key, V value, V separator) {
        V oldValue = get(key);
        if (oldValue == null) {
            return put(key, value);
        } else {
            if (oldValue instanceof String && separator instanceof String && value instanceof String) {
                return put(key, (V) StringUtils.joinWith((String) separator, (String) oldValue, (String) value));
            } else
                return put(key, value);
        }
    }
}
