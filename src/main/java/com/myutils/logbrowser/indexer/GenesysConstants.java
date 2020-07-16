/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author ssydoruk
 */
enum GenesysConstants {
    TSERVER("TServer"),
;

    static String showAll() {
        StringBuilder ret = new StringBuilder();
        for (GenesysConstants value : GenesysConstants.values()) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(value);
        }
        return ret.toString();
    }

    private final String name;

    private static final Map<String, GenesysConstants> ENUM_MAP;

    GenesysConstants(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // Build an immutable map of String name to enum pairs.
    // Any Map impl can be used.
    static {
        Map<String, GenesysConstants> map = new ConcurrentHashMap<>();
        for (GenesysConstants instance : GenesysConstants.values()) {
            map.put(instance.getName(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static GenesysConstants get(String name) {
        return ENUM_MAP.get(name);
    }
}
