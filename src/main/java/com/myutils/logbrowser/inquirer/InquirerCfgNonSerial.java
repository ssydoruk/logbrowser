/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import static com.myutils.logbrowser.inquirer.QueryTools.getRefNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import com.myutils.logbrowser.indexer.ReferenceType;

/**
 *
 * @author ssydoruk
 */
public class InquirerCfgNonSerial {

    private HashMap<ReferenceType, ArrayList<OptionNode>> refsChecked = new HashMap<>(20);

    public Set<ReferenceType> getRefTypes() {
        return refsChecked.keySet();
    }

    public InquirerCfgNonSerial() {
        LoadRefs();
    }

    public HashMap<String, Boolean> getRefsHash(ReferenceType refType) {
        HashMap<String, Boolean> ret = null;

        ArrayList<OptionNode> refs = getRefs(refType);
        if (refs != null) {
            ret = new HashMap<>(refs.size());
            for (OptionNode ref : refs) {
                ret.put(ref.getName(), ref.isChecked());
            }
        }
        return ret;
    }

    public ArrayList<OptionNode> getRefs(ReferenceType refType) {
        return refsChecked.get(refType);
    }

    public void LoadRefs() {
        LoadRef(ReferenceType.TEvent);
        LoadRef(ReferenceType.METRIC);
        LoadRef(ReferenceType.SIPMETHOD);
        LoadRef(ReferenceType.ORSMETHOD);
        LoadRef(ReferenceType.ORSMODULE);
        LoadRef(ReferenceType.StatType);
    }

    private void LoadRef(ReferenceType refType) {
        try {
            String[] refNames = getRefNames(this, refType);
            ArrayList<OptionNode> refs = refsChecked.get(refType);
            if (refs == null) {
                CreateRefs(refType, refNames);
            } else {
                AddRefs(refType, refNames, refs);
            }
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
    }

    private void CreateRefs(ReferenceType refType, String[] refNames) {
        ArrayList<OptionNode> refs = new ArrayList<>(refNames.length);

        for (String ref : refNames) {
            refs.add(new OptionNode(true, ref));
        }
        refsChecked.put(refType, refs);
    }

    private void AddRefs(ReferenceType refType, String[] refNames, ArrayList<OptionNode> refs) {
        for (String ref : refNames) {
            if (!RefExists(refs, ref)) {
                refs.add(new OptionNode(true, ref));
            }
        }

    }

    private boolean RefExists(ArrayList<OptionNode> refs, String ref) {
        for (OptionNode ref1 : refs) {
            if (ref.equals(ref1.getName())) {
                return true;
            }
        }
        return false;
    }

    String getProperty(String tLibNameTail) {
        if (tLibNameTail.equals("TlibFilter")) {
            return "RequestAttachUserData,EventCallDataChanged,EventCallPartyAdded,EventCallPartyState,RequestUpdateUserData,EventAttachedDataChanged,RequestDistributeEvent,EventCallPartyDeleted,EventCallCreated,EventCallDeleted,RequestDeletePair,EventCallPartyMoved,ISCCEventAttachedDataChanged,EventACK,EventReserved_2";
        }
        return null;
    }
}
