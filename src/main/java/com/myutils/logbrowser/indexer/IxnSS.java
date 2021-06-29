/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

public class IxnSS extends Ixn {

    final private static Matcher regIxnMessage = Pattern.compile("^Server: IxnEvent '([^']+)'").matcher("");
    final private static Matcher regIxnCustomEvent = Pattern.compile("'([^']+)'$").matcher("");

    final private static Matcher regConnID = Pattern.compile("^\\s+AttributeConnID .+ (\\w+)$").matcher("");
    final private static Matcher regConnIDCustom = Pattern.compile("^\\s+ConnID\\s+(\\w+)").matcher("");
    final private static Matcher regService = Pattern.compile("^\\s+'Service'.+= \"([^\"]+)\"$").matcher("");
    final private static Matcher regMethod = Pattern.compile("^\\s+'Method'.+= \"([^\"]+)\"$").matcher("");
    final private static Matcher regClientRef = Pattern.compile("^\\s+attr_esp_client_refid.+ = (\\d+)$").matcher("");
    final private static Matcher regServerRef = Pattern.compile("^\\s+attr_esp_server_refid.+ = (\\d+)$").matcher("");

    private final String messageName;
    private boolean customEvent = false;

    public IxnSS(ArrayList messageLines, boolean customEvent) {
        super(TableType.IxnSS, messageLines);
        this.customEvent = customEvent;
        this.messageName = parseMessageName();
    }

    private String parseMessageName() {
        String ret = "";
        SetInbound(true);
        if (!m_MessageLines.isEmpty()) {

            Matcher m;
            if (customEvent) {
                if ((m = regIxnCustomEvent.reset(m_MessageLines.get(1))).find()) {
                    SetInbound(true);
                    ret = m.group(1);
                }
            } else if ((m = regIxnMessage.reset(m_MessageLines.get(0))).find()) {
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

}
