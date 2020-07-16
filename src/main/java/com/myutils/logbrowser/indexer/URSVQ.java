/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class URSVQ extends Message {
    //    _M_I_018a02adfd64324f [17:0f] VQ 7f00218acf00 [at all 71 0/0 0] 1 Target(s), flag=700000408a, guid: 0Resources|BH_SalesGeneral_VQ_multimediaswitch||1|m38|1|00|0|0|---|||||||01StatAgentLoading|00{}{}{}[]?BH_SalesInbound_AG:Chat_SK>0@hc1_statsrvr_urs_p.GA
    private static final Pattern ptGuid = Pattern.compile("^VQ\\s+(\\S+).+ guid:(.+)$");

    private String FileLine;
    private String ConnID = null;

    private String VQID = null;
    private String vqName;
    private String target;

    public URSVQ() {
        super(TableType.URSVQ);
    }
    public String getVQID() {
        return VQID;
    }
    public String getVqName() {
        return vqName;
    }
    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "URSVQ{" + "FileLine=" + FileLine + ", ConnID=" + ConnID + ", VQID=" + VQID + ", vqName=" + vqName + ", target=" + target + '}';
    }

    String GetConnID() {
        return ConnID;
    }

    String getFileFun() {
        return FileLine;
    }

    void setConnID(String lastConnID) {
        this.ConnID = lastConnID;
    }


    void setGUIDMsg(String GUIDMsg) {
        Matcher m;
        if ((m = ptGuid.matcher(GUIDMsg)).find()) {
            this.VQID = m.group(1);
            String guid = m.group(2);
            String[] split = StringUtils.split(guid, '|');
            if (split != null) {
                if (split.length >= 1) {
                    this.vqName = split[1];
                }
                if (split.length >= 0) {
                    this.target = split[split.length - 1];
                }
            }

        } else {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
