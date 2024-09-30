/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

public final class GREClientMessage extends Message {

    private final static Message.Regexs IxnIDs = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*attr_itx_id .+ \"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*'InteractionId'.+= \"([^\"]+)\""), 1));
            add(new Pair<>(Pattern.compile("^\\s*'Id' .+\"([^\"]+)\"$"), 1));
            add(new Pair<>(Pattern.compile("^\\s*'ORSI:.+:([\\w~]+)' \\[list\\]"), 1));
        }
    }
    );

    private static final Regexs refIDs = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*ReferenceId.+ = (\\d+)"), 1));
        }
    }
    );

    private static final Regexs reQueue = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s'Queue.+= \"(.+)\"$"), 1));
        }
    }
    );

    private static final Regexs reRequestorAppName = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s+'RequestorAppName.+= \"(.+)\"$"), 1));
        }
    }
    );

    private static final Regexs reRequestorClientId = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s+'RequestorClientId.+= \"(.+)\"$"), 1));
        }
    }
    );

    private static final Regexs reMediaType = new Regexs(
            new ArrayList<Pair<Pattern, Integer>>() {
        {
            add(new Pair<>(Pattern.compile("^\\s*MediaType.+\"([^\"]+)\"$"), 1));
        }
    }
    );

    private final String clientName = null;
    private final RegExAttribute reIxnID = new RegExAttribute(IxnIDs);
    private final RegExAttribute refID = new RegExAttribute(refIDs);
    private final RegExAttribute QueueID = new RegExAttribute(reQueue);
    private final RegExAttribute mediaType = new RegExAttribute(reMediaType);
    private final RegExAttribute requestorAppName = new RegExAttribute(reRequestorAppName);
    private final RegExAttribute requestorClientId = new RegExAttribute(reRequestorClientId);

    private String messageName = null;

    public GREClientMessage(String msgName, ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.GREClient, fileID);
        this.messageName = msgName;
        parseAttributes(
                m_MessageContents,
                reIxnID,
                refID,
                QueueID,
                mediaType,
                requestorClientId,
                requestorAppName);
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, reIxnID.getStringValue()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.IxnMedia, mediaType.getStringValue()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, QueueID.getStringValue()));
        stmt.setLong(10, refID.getIntValue());
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.App, requestorAppName.getStringValue()));
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.URSStrategyName, requestorClientId.getStringValue()));
        return true;
    }

    String GetMessageName() {
        return (messageName != null && !messageName.isEmpty())
                ? messageName : null;
    }

}
