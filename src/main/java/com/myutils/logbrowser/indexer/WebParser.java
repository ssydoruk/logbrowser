/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Pattern;

/**
 * @author stepan_sydoruk
 */
abstract class WebParser extends Parser {

    protected static final String PATH_UUID = "<UUID>";
    protected static final String PATH_DeviceID = "<DeviceID>";
    protected static final String PATH_IxnID = "<IxnID>";
    protected static final String PATH_ContactID = "<ContactID>";
    protected static final String PATH_DBID = "<DBID>";
    protected static final String PATH_STATS = "<STATSID>";
    protected static final String PATH_GROUPS = "<GROUPS>";
    protected static final String PATH_USERID = "<UserID>";
    protected static final String PATH_SKILLS = "<skill>";
    protected static final String PATH_AGENT_LOGINS = "<loginID>";
    protected static final String PATH_OFFSET = "<num>";

    protected static final String QUERY_userName = "userName";
    protected static final String QUERY_filterParameters = "filterParameters";
    protected static final String QUERY_name = "name";
    protected static final String QUERY_objectDBID = "object_dbid";
    protected static final String QUERY_ID = "id";
    protected static final String QUERY_NONCE = "nonce";
    protected static final String QUERY_phoneNumber = "phoneNumber";
    protected static final String QUERY_personDBID = "person_dbid";
    protected static final String QUERY_ownerDBID = "owner_dbid";

    protected static URLProcessor urlProcessor = new URLProcessor();

    public WebParser(FileInfoType type, DBTables tables) {
        super(type, tables);
        initURLProcessor();

    }

    private void initURLProcessor() {
        urlProcessor.addPathProcessor(PATH_UUID, Pattern.compile("^/api/v2/me/calls/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_DeviceID, Pattern.compile("^/api/v2/(?:me/)?devices/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_IxnID, Pattern.compile("^/api/v2/me/chats/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_IxnID, Pattern.compile("^/api/v2/me/openmedia/(?:messaging_inbound|messaging_outbound)/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_IxnID, Pattern.compile("^/internal-api/ucs/interactions/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_ContactID, Pattern.compile("^/api/v2/contacts/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_DBID, Pattern.compile("^/api/v2/platform/configuration/(?:[^/\\&]+)/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_STATS, Pattern.compile("^/api/v1/stats/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_GROUPS, Pattern.compile("^/api/v2/groups/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_USERID, Pattern.compile("^/api/v2/users/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_SKILLS, Pattern.compile("^/api/v2/platform/configuration/skills/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_AGENT_LOGINS, Pattern.compile("^/api/v2/platform/configuration/agent-logins/([^/\\&]+)", Pattern.CASE_INSENSITIVE));
        urlProcessor.addPathProcessor(PATH_OFFSET, Pattern.compile("^/api/v2/users?offset=(\\d+)", Pattern.CASE_INSENSITIVE));

        urlProcessor.addQueryProcessor(QUERY_userName, "<user>");
        urlProcessor.addQueryProcessor(QUERY_filterParameters, "<param>");
        urlProcessor.addQueryProcessor(QUERY_name, "<name>");
        urlProcessor.addQueryProcessor(QUERY_objectDBID, "<object_dbid>");
        urlProcessor.addQueryProcessor(QUERY_ID, "<id(s)>");
        urlProcessor.addQueryProcessor(QUERY_NONCE, "<nonce>");
        urlProcessor.addQueryProcessor(QUERY_phoneNumber, "<phoneNumber(s)>");
        urlProcessor.addQueryProcessor(QUERY_personDBID, "<person_dbid>");
        urlProcessor.addQueryProcessor(QUERY_ownerDBID, "<owner_dbid>");
    }

}
