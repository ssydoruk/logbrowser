/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;
import static java.sql.Types.INTEGER;

public class IxnSS extends Ixn {

    private static final Pattern regIxnMessage = Pattern.compile("^Server: IxnEvent '([^']+)'");
    private static final Pattern regIxnCustomEvent = Pattern.compile("'([^']+)'$");

    private static final Pattern regConnID = Pattern.compile("^\\s+AttributeConnID .+ (\\w+)$");
    private static final Pattern regConnIDCustom = Pattern.compile("^\\s+ConnID\\s+(\\w+)");
    private static final Pattern regService = Pattern.compile("^\\s+'Service'.+= \"([^\"]+)\"$");
    private static final Pattern regMethod = Pattern.compile("^\\s+'Method'.+= \"([^\"]+)\"$");
    private static final Pattern regClientRef = Pattern.compile("^\\s+attr_esp_client_refid.+ = (\\d+)$");
    private static final Pattern regServerRef = Pattern.compile("^\\s+attr_esp_server_refid.+ = (\\d+)$");

    private final String messageName;
    private boolean customEvent = false;

    public IxnSS(ArrayList messageLines, boolean customEvent, int fileID) {
        super(TableType.IxnSS, messageLines, fileID);
        this.customEvent = customEvent;
        this.messageName = parseMessageName();
    }

    private String parseMessageName() {
        String ret = "";
        SetInbound(true);
        if (!m_MessageLines.isEmpty()) {

            Matcher m;
            if (customEvent) {
                if ((m = regIxnCustomEvent.matcher(m_MessageLines.get(1))).find()) {
                    SetInbound(true);
                    ret = m.group(1);
                }
            } else if ((m = regIxnMessage.matcher(m_MessageLines.get(0))).find()) {
                SetInbound(true);
                ret = m.group(1);
            }
        }
        return ret;
    }

    @Override
    String GetMessageName() {
        return messageName;
    }

    @Override
    String GetConnID() {
        if (!customEvent) {
            return FindByRx(regConnID, 1, "");
        } else {
            return FindByRx(regConnIDCustom, 1, "");
        }
    }

    @Override
    String GetClient() {
        if (!customEvent) {
            return super.GetClient();
        } else {
            return "";
        }
    }

    @Override
    String GetAgent() {
        if (!customEvent) {
            return super.GetAgent();

        } else {
            return "";
        }
    }

    @Override
    String GetPlace() {
        if (!customEvent) {
            return super.GetPlace();
        } else {
            return "";
        }
    }

    String GetService() {
        if (!customEvent) {
            return FindByRx(regService, 1, "");
        } else {
            return "";
        }

    }

    String GetMethod() {
        if (!customEvent) {
            return FindByRx(regMethod, 1, "");
        } else {
            return "";
        }
    }

    Integer GetClientRef() {
        if (!customEvent) {
            return intOrDef(FindByRx(regClientRef, 1, null), (Integer) null);
        } else {
            return null;
        }

    }

    Integer GetServerRef() {
        if (!customEvent) {
            return intOrDef(FindByRx(regServerRef, 1, null), (Integer) null);
        } else {
            return null;
        }
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, GetIxnID()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.IxnMedia, GetMedia()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, cleanDN(GetIxnQueue())));
        stmt.setLong(10, getM_refID());
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.ConnID, GetConnID()));
        stmt.setBoolean(12, isInbound());
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.IxnID, GetParentIxnID()));
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.Agent, GetAgent()));
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.Place, GetPlace()));
        setFieldInt(stmt, 16, Main.getRef(ReferenceType.App, GetClient()));

        setIntOrNull(stmt, 17, GetServerRef(), -1);
        setIntOrNull(stmt, 18, GetClientRef(), -1);

        setFieldInt(stmt, 19, Main.getRef(ReferenceType.IxnService, GetService()));
        setFieldInt(stmt, 20, Main.getRef(ReferenceType.IxnMethod, GetMethod()));
        return true;
    }

    private void setIntOrNull(PreparedStatement stmt, int rec, Object val, Object def) throws SQLException {
        if (val == null || val.equals(def)) {
            stmt.setNull(rec, INTEGER);
        } else {
            stmt.setObject(rec, val);
        }
    }


}
