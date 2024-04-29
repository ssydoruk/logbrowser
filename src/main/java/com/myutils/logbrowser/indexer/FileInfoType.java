/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ssydoruk
 */
public enum FileInfoType {
    type_Unknown(0),
    // Insert your component types here
    type_SessionController(1),
    type_CallManager(2),
    type_TransportLayer(3),
    type_InteractionProxy(4),
    type_tController(5),
    type_URS(6),
    type_SipProxy(7),
    type_StatServer(8),
    type_ICON(9),
    type_ORS(10),
    type_OCS(11),
    type_IxnServer(12),
    type_DBServer(13),
    type_ConfServer(14),
    type_WorkSpace(15),
    type_MCP(16),
    type_RM(17),
    type_SIPEP(18),
    type_SCS(19),
    type_WWE(20),
    type_GMS(21),
    type_LCA(22),
    type_VOIPEP(23),
    type_WWECloud(24),
    type_ApacheWeb(25),
    type_URSHTTP(26);

    private static final Map<FileInfoType, String> TypeFile = new HashMap<FileInfoType, String>() {
        {
            put(type_URS, "RouterServer");
            put(type_ORS, "OrchestrationServer");
            put(type_OCS, "OCServer");
            put(type_StatServer, "StatServer");
            put(type_SessionController, "TServer");
            put(type_CallManager, "TServer");
            put(type_TransportLayer, "TServer");
            put(type_InteractionProxy, "TServer");
            put(type_IxnServer, "InteractionServer");
            put(type_tController, "TServer");
            put(type_DBServer, "RealDBServer");
            put(type_SipProxy, "GenericServer");
            put(type_ConfServer, "ConfigurationServer");
            put(type_RM, "CFGGVPResourceMgr");
            put(type_MCP, "CFGGVPMCP");
            put(type_WorkSpace, "Workspace");
            put(type_SIPEP, "SIPEndPoint");
            put(type_SCS, "SCS");
            put(type_WWE, "WWE");
            put(type_GMS, "GMS");
            put(type_LCA, "LCA");
            put(type_VOIPEP, "VoIP endpoint");
            put(type_WWECloud, "WWECloud");
            put(type_ApacheWeb, "ApacheWeb");
            put(type_URSHTTP, "URS_HTTP");
        }
    };
    private static final Map<Integer, FileInfoType> map = new HashMap<>();

    static {
        for (FileInfoType fileInfoType : FileInfoType.values()) {
            map.put(fileInfoType.value, fileInfoType);
        }
    }

    private final int value;

    FileInfoType(int value) {
        this.value = value;
    }

    public static FileInfoType FileType(String t) {
        for (Map.Entry<FileInfoType, String> entrySet : TypeFile.entrySet()) {
            FileInfoType key = entrySet.getKey();
            String value = entrySet.getValue();
            if (value.equalsIgnoreCase(t)) {
                return key;
            }
        }
        return type_Unknown;
    }

    public static String getFileType(FileInfoType t) {
        return TypeFile.get(t);
    }

    public static FileInfoType GetvalueOf(int fileInfoType) {
        return map.get(fileInfoType);
    }

    public static String getTableName(FileInfoType t) {
        String toString = t.toString();
        StringBuilder ret = new StringBuilder(toString.length() + 10);
        if (toString.startsWith("type_")) {
            ret.append(toString.substring(5));
        } else {
            ret.append(toString);
        }
        return ret.append("_tdiff").toString();
    }

    public int getValue() {
        return value;
    }

}
