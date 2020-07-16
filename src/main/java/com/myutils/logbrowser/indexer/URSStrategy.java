/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class URSStrategy extends Message {

    private static final StrategySteps1 StrategySteps = new StrategySteps1();
    private static final Pattern uuidPattern = Pattern.compile("^calluuid ([\\w~]+) is bound");
    private static final Pattern ptnMod = Pattern.compile("^ASSIGN: v:\\d+:_m.+\"([\\w~]+)\"$");
    private static final Pattern ptnFun = Pattern.compile("^ASSIGN: v:\\d+:_f.+\"([\\w~]+)\"$");
    private static final Pattern ptnParam = Pattern.compile("^ASSIGN: v:\\d+:_p.+(\\[.+\\])$");
    private static final Pattern ptnEventAll = Pattern.compile("^ASSIGN: v:\\d+:_eventdata\\(LOCAL\\) <- LIST: (.+)$");
    private static final Pattern ptnNotif = Pattern.compile("^ASSIGN: v:\\d+:_event\\(LOCAL\\) <- OBJECT:[^=]+=(.+)");

    private final String FileLine;
    private String ConnID;
    private String rest;
    private String ref2;
    private String ref1;

//    final private static Pattern regCampaignDBID= Pattern.compile("CampaignDBID: ([^;]+);");
    URSStrategy(ArrayList m_MessageContents, String ConnID, String FileLine,
            String lineRest) {
        super(TableType.URSStrategy);
        this.m_MessageLines = m_MessageContents;
        this.FileLine = FileLine;
        this.ConnID = ConnID;
        this.rest = lineRest;
        Main.logger.trace(this.toString());
    }

    URSStrategy(String line, String ConnID, String FileLine, String lineRest) {
        super(TableType.URSStrategy, line);
        this.FileLine = FileLine;
        this.ConnID = ConnID;
        this.rest = lineRest;
        Main.logger.trace(this.toString());
    }

    URSStrategy(String lastConnID, String FileLine, ArrayList m_MessageContents) {
        super(TableType.URSStrategy);
        this.m_MessageLines = m_MessageContents;
        this.FileLine = FileLine;
        this.ConnID = lastConnID;
    }

    @Override
    public String toString() {
        return "URSStrategy{" + "FileLine=" + FileLine + ", ConnID=" + ConnID + ", rest=" + rest + '}';
    }

    String GetConnID() {
        return ConnID;
    }

    String getFileFun() {
        return FileLine;
    }

    String getStrategyMsg() {
        return StrategySteps.get(FileLine, rest);
    }

    String getUUID() {
        return FindByRx(uuidPattern, rest, 1, "");
    }

    String getStrategyRef1() {
        return ref1;
    }

    String getStrategyRef2() {
        return ref2;
    }

    void parseSCXMLAttributes() {
        String rxRet = FindByRx(ptnMod, 1, null);
        if (rxRet != null) {
            String fun = FindByRx(ptnFun, 1, null);
            ref1 = rxRet + ((fun == null) ? "" : "/" + fun);
            ref2 = FindByRx(ptnParam, 1, "");
            return;
        }
        rxRet = FindByRx(ptnEventAll, 1, null);
        if (rxRet != null) {
            String[] split = StringUtils.split(rxRet, '|');
            if (split != null && split.length > 0) {
                String module = null;
                String func = null;
                String params = null;

                for (String string : split) {
                    String[] split1 = StringUtils.split(string, ':');
                    if (split1 != null && split1.length > 2) {
                        if (split1[0].equals("module")) {
                            module = split[1];
                        } else if (split1[0].equals("func")) {
                            func = split[1];
                        } else if (split1[0].equals("params")) {
                            params = split[1];
                        }
                    }
                }
                ref1 = ((module == null) ? "" : module + "/") + ((func == null) ? "" : func);
                ref2 = params;
            }
            return;
        }

        rxRet = FindByRx(ptnNotif, 1, null);
        if (rxRet != null) {
            JsonValue value = Json.parse(rxRet);
            if (value != null) {
                Main.logger.trace(value.toString());
                if (value.isObject()) {
                    JsonObject obj = value.asObject();
                    ref1 = obj.get("name").asString();
                    ref2 = obj.get("data").toString();
                }
            }
        }
    }

    void parseTarget() {
        for (String h : new String[]{"Agent", "Place", "Number"}) {
            String v = GetHeaderValue(h);
            if (v != null && !v.isEmpty()) {
                ref1 = h;
                ref2 = v;
                break;
            }

        }
    }

    private static class StrategySteps1 {

        private static final Map<String, SS> ss1 = new HashMap<>();

        StrategySteps1() {
            try {
//17:42:51.889_I_I_00d202972e8c2ebc [07:07] HERE IS TARGETS
                addStep("07:07", null, "HERE IS TARGETS");
                addStep("07:26", null, "HERE IS TARGETS - SelectDN");
                addStep("07:57", null, "HERE IS TARGETS - SCXML");
//    _I_I_00d202972e8c2ebc [14:33] strategy: *0x65*strt_Generali_AgentGroupSRLTest_001 (3347761790) is attached to the call
                addStep("14:33", "^strategy:", "strategy: is attached to the call");
//    _I_I_06db029d41e71008 [1B:01] strategy: *0x65*GWIMIVR_MAIN (3017419778) is attached to the call
                addStep("1B:01", "^strategy:", "strategy: is attached to the call");
//    _I_I_00d202972e8c2ebc [07:0a] HERE IS WAIT (3600 sec)
                addStep("07:0a", null, "HERE IS WAIT");
//    _I_I_00d202972e8c2ebc [0E:10] HERE IS ROUTE_CALL: VQ=, target=SRLTest
                addStep("0E:10", null, "HERE IS ROUTE_CALL");
//    _I_I_00d202972e8c2ebc [14:33] wandering call come to cdn
                addStep("14:33", "^wandering call", "wandering call come to cdn");
//17:42:51.895_I_I_00d202972e8c2ebc [01:08] call (252087-104e77cd0) deleting truly
                addStep("01:08", null, "call deleting truly");
//    _I_I_01310294fda750ca [01:07] treatment added: type=IVR, par1=, par2=6510_SV7000@StatServer_URS.Q, tout=999999
                addStep("01:07", null, "treatment added");
//    _B_I_01310294fda750ca [14:1f] treatment end===========>TREATMENT_NONE
                addStep("14:1f", null, "treatment end");
//    _T_I_01310294fda750ca [14:27] treatment starting: type=IVR, par1=, par2=6510_SV7000@StatServer_URS.Q, tout=999999
                addStep("14:27", null, "treatment starting");
//    _T_I_03fa029c5bfef131 [01:1b] calluuid 0002CaCCQYA1007F is bound to the call 363-08c3e8e8 as 04dff9e8
                addStep("01:1b", null, "calluuid is bound to the call");
//20:17:08.450_I_I_03fa029c5bfef131 [01:01] call (363-08c3e8e8) for Resources created (del=0 ts=0,1,0)
                addStep("01:01", null, "Call created");
//    _I_I_3112029b976fd8a2 [09:04] ASSIGN: __TargetVar(SCRIPT) <- STRING[6,0]: ""
                addStep("09:04", "^ASSIGN:.+\\(SCRIPT\\) <-", "Assign script");
//    _I_I_3112029b976fd8a2 [09:04] ASSIGN: _statServer(LOCAL) <- STRING[5,12]: "ss_rtg_H_PRI"
                addStep("09:04", "^ASSIGN:.+\\([^S]", "Assign local");

//  _I_I_06db029d41e71001 [07:42] jump to strategy *0x65*8500 v52 RG SBR 76 DMZ (crc=2514624076)                
                addStep("07:42", null, "jump to strategy");
//11:21:21.277_I_I_06db029d41e71001 [07:37] HERE IS BUSINESS RULE ICR Switch to 8500 Mega
                addStep("07:37", null, "HERE IS BUSINESS RULE");
//    _I_I_06db029d41e71001 [07:43] call strategy *0x65*07172003 Prod DMZ Attached Data (level=2, crc=1072102340)
                addStep("07:43", null, "call strategy");
//    _I_I_06db029d41e71001 [07:44] return to strategy *0x65*8500 v52 RG SBR 76 DMZ (level=1, crc=2514624076)
                addStep("07:44", null, "return to strategy");

//  _T_I_06db029d41e71001 [0E:0d] routing done
                addStep("0E:0d", null, "routing done");
//    _M_I_06db029d41e71001 [01:0d] attempt to route (1)
                addStep("01:0d", null, "attempt to route");
//15:35:16.129_M_I_05c302a209c3bd68 [13:02] entering virtual queue "AI_OTM_Ipoteka_VQ"                
                addStep("13:02", null, "entering virtual queue");
//15:35:56.770_M_I_05c302a209c3aa65 [10:2f] RStatCallsInQueue update: object <ZIC_AGENT>(144), type CfgPerson value 1+0+0, reason <target selection>
//15:35:56.770_M_I_05c302a209c3aa65 [10:2f] RStatCallsInQueue update: object <6686iv>(308), type CfgPlace value 1+0+0, reason <target selection>                
                addStep("10:2f", null, "RStatCallsInQueue update");
//    _M_I_05c302a209c3bb06 [10:21] try to route to agent "Krasilnikov9" (place "Place6671", 4 ready DNs reported): Target 00e82bd0 VQ 034e2270
                addStep("10:21", null, "try to route to agent");
//_T_I_05c302a209c3bb06 [0E:19] check call routing states: state=4 delivery=0 treatment=0 held=0 reserving=0 ivr=0 - true                
                addStep("0E:19", null, "check call routing states");
//00:23:14.003_M_I_ [10:1d] PULSE (calls: 1(1)=1+0-0, targets=0, time=1496956994, mem=0,92300,969,244,274,1)
                addStep("10:1d", null, "PULSE on timer");
//00:23:12.144_M_I_007902a1b7081aba [10:1f] pulse for one call                
                addStep("10:1f", null, "pulse for one call");
//    _I_E_044b02a4581e0f91 [09:05] error in strategy: 0013 Remote error (RequestService)                
                addStep("09:05", null, "error in strategy");
//2018-01-18T14:20:42.000_X_I_019902b5998fa018 [06:04] send to ts SIPS_ELR_a(1) TReserveAgent (dn=twei2@, ag=twei2, pl=1112220043, priorities=10,96157,0)
                addStep("06:04", null, "Agent reserve request");

            } catch (Exception exception) {
                Main.logger.error("Cannot create StrategySteps1", exception);
            }
        }

        private String get(String FileLine, String rest) {
            SS ret;
            if ((ret = ss1.get(FileLine)) != null) {
                for (Pattern pattern : ret.pattString.keySet()) {
                    if (pattern == null) {
                        return ret.pattString.get(pattern);
                    } else {
                        Matcher m;
                        if ((m = pattern.matcher(rest)).find()) {
                            return ret.pattString.get(pattern);
                        }
                    }
                }
            }
            return "Misc";
        }

        private void addStep(String fl, String pt, String aliasString) throws Exception {
            SS get1 = ss1.get(fl);
            if (get1 != null) {
                get1.addPatt(pt, aliasString);
            } else {
                ss1.put(fl, new SS(pt, aliasString));
            }
        }

        private static class SS {

            private HashMap<Pattern, String> pattString = new HashMap<>();

            private SS(String Patt, String _ret) throws Exception {
                if (Patt != null && Patt.length() > 0) {
                    addPatt(Patt, _ret);
                } else {
                    pattString.put(null, _ret);
                }
            }

            private void addPatt(String pt, String aliasString) throws Exception {
                if (pt == null) {
                    throw new Exception("With more than one FileSearch, each need to have pattern");
                }
                pattString.put(Pattern.compile(pt), aliasString);
            }
        }
    }
}
