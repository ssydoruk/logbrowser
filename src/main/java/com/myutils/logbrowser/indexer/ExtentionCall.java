package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtentionCall {

    private static final Pattern regConn = Pattern.compile("^\\s+'conn-(\\d)+'\\s+'(\\w+)'$");
    private static final Pattern regUUID = Pattern.compile("^\\s+'call-uuid-(\\d)+'\\s+'(\\w+)'$");
    private final String ConnID;
    private final Integer callIdx;
    private String UUID;

    public ExtentionCall(Integer callIdx, String ConnID, String UUID) {
        this.callIdx = callIdx;
        this.ConnID = ConnID;
        this.UUID = UUID;
    }

    private static ExtentionCall findCall(ArrayList<ExtentionCall> ret, int parseInt) {
        for (ExtentionCall extentionCall : ret) {
            Integer callIdx1 = extentionCall.getCallIdx();
            if (callIdx1 != null && callIdx1.equals(parseInt)) {
                return extentionCall;
            }
        }
        return null;
    }

    public static ArrayList<ExtentionCall> getExtentionCalls(ArrayList<String> m_MessageContents) {
        ArrayList<ExtentionCall> ret = null;
        ArrayList<String> ext = Message.getAllExtentions(m_MessageContents);
        if (ext != null) {
            ret = new ArrayList<>();
            for (int i = 0; i < ext.size(); i++) {
                String get = ext.get(i);
                Matcher m;
                if (get != null && !get.isEmpty() && (m = regConn.matcher(get)).find()) {
                    try {
                        ret.add(new ExtentionCall(Integer.parseInt(m.group(1)), m.group(2), null));
                    } catch (NumberFormatException numberFormatException) {
                        Main.logger.error("getExtentionCalls: cannot parse number from [" + m.group(1) + "] (" + get + ")");
                    }
                }
            }

            for (int i = 0; i < ext.size(); i++) {
                String get = ext.get(i);
                Matcher m;
                if (get != null && !get.isEmpty() && (m = regUUID.matcher(get)).find()) {
                    try {
                        int parseInt = Integer.parseInt(m.group(1));
                        ExtentionCall ec = findCall(ret, parseInt);
                        if (ec != null) {
                            ec.setUUID(m.group(2));
                        } else {
                            ret.add(new ExtentionCall(parseInt, null, m.group(2)));
                        }
                    } catch (NumberFormatException numberFormatException) {
                        Main.logger.error("getExtentionCalls: cannot parse number from [" + m.group(1) + "] (" + get + ")");
                    }
                }
            }
        }
        return ret;
    }

    public String getConnID() {
        return ConnID;
    }

    public String getUUID() {
        return UUID;
    }

    private void setUUID(String group) {
        this.UUID = group;
    }

    public Integer getCallIdx() {
        return callIdx;
    }
}
