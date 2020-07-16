/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Util;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

public final class URSRlib extends Message {

    final private static Pattern regrefid = Pattern.compile("^\\srefid.+\\s(\\d+)");

    final private static Pattern regModule = Pattern.compile("^\\sfm.+\\s('[^']+')");

    final private static Pattern regMethod = Pattern.compile("^\\sfunction.+\\s('[^']+')");

    final private static Pattern regErrorMsg = Pattern.compile("^\\s+errmsg.+\\s'([^']+)'");
    final private static Pattern regErrorCode = Pattern.compile("^\\serror.+\\s(\\d+)");

    final private static Pattern regCall = Pattern.compile("^\\scallid.+\\s'\\**([\\w~]+)'");

    final private static Pattern regSid = Pattern.compile("^\\ssessionid.+\\s'\\**([\\w~]+)'");
    final private static Pattern regSource = Pattern.compile("RLIB: (request to|received from) \\d+\\((\\w+)\\) message (\\w+)");
    final private static Pattern prnResult = Pattern.compile("^\\s*result\\(1000000004\\)\\s+'(.+)'$");
    final private static Pattern prnParam = Pattern.compile("^\\s*args\\(1000000003\\)\\s+'(.+)'$");
    private String CallID = null;
    private String sid = null;
    private String source;
    private String message = null;
    private String result;
    private Integer reqID;
    private String method;
    private String module;

    public URSRlib(ArrayList messageLines) {
        super(TableType.URSRlib, messageLines);
        Matcher m;
        if (messageLines.size() > 0 && (m = regSource.matcher((String) messageLines.get(0))).find()) {
            this.source = m.group(2);
            SetInbound(m.group(1).endsWith("m")); // from
            this.message = m.group(3);
        }
    }

    public String getMessage() {
        return message;
    }

    int GetRefId() {
        return FindByRx(regrefid, 1, -1);
    }

    private void parseParams() {

        if (method == null) {
            module = FindByRx(regModule, 1, null);
            String FindByRx1 = FindByRx(regMethod, 1, null);
            if (module != null || FindByRx1 != null) {
                method = StringUtils.join(new String[]{module, FindByRx1}, '/');
            } else {
                String errorMsg = FindByRx(regErrorMsg, 1, null);
                String errorCode = FindByRx(regErrorCode, 1, null);
                if (errorCode != null || errorMsg != null) {
                    method = StringUtils.join(new String[]{"Error", errorCode, errorMsg}, '/');
                } else {
                    method = message;
                    if (message != null) {
                        if (message.equals("ResultOfInvoke")) {
                            method = "Result";
                            reqID = Util.intOrDef(FindByRx(prnResult, 1, null), (Integer) null);
                            result = "";
                        } else if (message.equals("RequestDeleteVCall") || message.equals("RequestUpdateVCall")) {
                            sid = GetCall();
                        } else if (message.equals("EventOnInvoke")) {
                            this.result = FindByRx(prnResult, 1, "");
                            try {
                                JSONObject _jsonBody = new JSONObject(result);

                                String name = jsonStringOrDef(_jsonBody, "name", (String) null);
                                if (name != null) {
                                    method = name;
                                }
                                JSONObject obj = jsonOrDef(_jsonBody, "data");
                                if (obj != null) {
                                    StringBuilder dataRest = new StringBuilder();
                                    for (Iterator<String> iterator = obj.keys(); iterator.hasNext();) {
                                        String key = iterator.next();
                                        if (key.equals("requestid")) {
                                            reqID = obj.getInt("requestid");
                                        } else {
                                            if (dataRest.length() > 0) {
                                                dataRest.append(", ");
                                            }
                                            dataRest.append(key).append(":").append(obj.get(key));
                                        }
                                    }
                                    if (dataRest.length() > 0) {
                                        result = dataRest.toString();
                                    } else {
                                        result = "";
                                    }
                                }

                            } catch (JSONException ex) {
                                Main.logger.info("Not JSON: [" + result + "]", ex);
                            }
                        }
                    }

                }
            }
            if (result == null) {
                this.result = FindByRx(prnResult, 1, "");
            }
            if (sid == null) {
                sid = FindByRx(regSid, 1, "");
            }
        }

    }

    Integer GetReqId() {
        parseParams();
        return reqID;
    }

    String GetModule() {
        parseParams();
        return module;
    }

    String GetMethod() {
        parseParams();
        return method;
    }

    String GetCall() {
        if (CallID == null) {
            CallID = FindByRx(regCall, 1, "");
//            if (CallID.length() > 0 && CallID.equals(GetSid())) // for multimedia interactions CallID=SID; do not want SID for them in UUID refTable
//            {
//                CallID = "";
//            }
        }
        return CallID;
    }

    String GetSid() {
        parseParams();
        return sid;
    }

    String getSource() {
        return source;
    }

    String getResult() {
        parseParams();
        return result;
    }

    String getParam() {
        return FindByRx(prnParam, 1, "");
    }

}
