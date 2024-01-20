/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang3.RegExUtils;

/**
 * @author stepan_sydoruk
 */
public class RegexFieldSettings implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Matcher rxVar = Pattern.compile("\\\\(\\d+)").matcher("");
    private static final Logger logger = LogManager.getLogger(RegexFieldSettings.class);
    private String searchString;
    private String retValue;
    private boolean makeWholeWorld;
    private boolean caseSensitive;
    transient private Matcher selectedRegEx = null;

    public RegexFieldSettings(String searchString, String retValue, boolean makeWholeWorld, boolean caseSensitive) {

        this.searchString = searchString;
        this.retValue = retValue;
        this.makeWholeWorld = makeWholeWorld;
        this.caseSensitive = caseSensitive;
    }

    public RegexFieldSettings(RegexFieldSettings currentRegexSetting) {
        this.searchString = currentRegexSetting.getSearchString();
        this.retValue = currentRegexSetting.getRetValue();
        this.makeWholeWorld = currentRegexSetting.isMakeWholeWorld();
        this.caseSensitive = currentRegexSetting.isCaseSensitive();
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getRetValue() {
        return retValue;
    }

    public void setRetValue(String retValue) {
        this.retValue = retValue;
    }

    public boolean isMakeWholeWorld() {
        return makeWholeWorld;
    }

    public void setMakeWholeWorld(boolean makeWholeWorld) {
        this.makeWholeWorld = makeWholeWorld;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    void updateParams(String rx, String retVal, boolean isCase, boolean isWord) {
        searchString = rx;
        retValue = retVal;
        caseSensitive = isCase;
        makeWholeWorld = isWord;
        selectedRegEx = getRegex(searchString, makeWholeWorld, caseSensitive);
    }

    private Matcher getRegex() {
        if (selectedRegEx == null) {
            selectedRegEx = getRegex(searchString, makeWholeWorld, caseSensitive);
        }
        return selectedRegEx;
    }

    private Matcher getRegex(String lastRegEx, boolean wholeWord, boolean matchCase) throws PatternSyntaxException {
        if (lastRegEx != null && !lastRegEx.isEmpty()) {
            int flags = 0;
            if (!matchCase) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            if (wholeWord) {
                StringBuilder rx = new StringBuilder(lastRegEx.length() + 2);
                if (lastRegEx.charAt(0) != '^') {
                    rx.append('^');
                }
                rx.append(lastRegEx);
                if (lastRegEx.charAt(lastRegEx.length() - 1) != '$') {
                    rx.append('$');
                }
                inquirer.logger.debug("Searching for [" + rx + "]");
                return Pattern.compile(rx.toString(), flags).matcher("");
            } else {
                inquirer.logger.debug("Searching for [" + lastRegEx + "]");
                return Pattern.compile(lastRegEx, flags).matcher("");
            }
        }
        return null;
    }

    private String doRepl(String text, ArrayList<Repl> repl, Matcher m) {
        if (!repl.isEmpty()) {
            StringBuilder ret = new StringBuilder();
            int idx = 0;
            for (int i = 0; i < repl.size(); i++) {
                Repl r = repl.get(i);
                ret.append(text, idx, r.start);
                ret.append(m.group(r.id));
                idx = r.end;
            }
            if (idx < text.length()) {
                ret.append(text.substring(idx));
            }
            return ret.toString();
        }
        return text;
    }

    public boolean checkMatch(String toString) {
        if (selectedRegEx != null) {
            if (toString == null) {
                return selectedRegEx.reset("").find();
            } else {
                return selectedRegEx.reset(toString).find();
            }

        } else {
            if (toString == null) {
                return false;
            }
            if (makeWholeWorld) {
                return retValue.equalsIgnoreCase(toString);
            } else {
                return toString.toLowerCase().contains(retValue.toLowerCase());
            }
        }
    }

    private StringBuilder extendString(StringBuilder s, String add) {
        if (s.length() > 0) {
            s.append(" | ");
        }
        s.append(add);
        return s;
    }

    public SearchSample findRxSample(String bytes) {
        if (bytes != null && bytes.length() > 0 && selectedRegEx != null) {
            SearchSample ret = new SearchSample();
            String[] split = StringUtils.split(bytes, "\r\n");
            String text = retValue;
            Matcher m1 = rxVar.reset(text);
            ArrayList<Repl> repl = new ArrayList<>();
            while (m1.find()) {
                repl.add(new Repl(m1.start(0), m1.end(0), Integer.parseInt(m1.group(1))));
            }
            StringBuilder s = new StringBuilder();
            for (String string : split) {
                Matcher m = selectedRegEx.reset(string);
                boolean found = false;
                inquirer.logger.trace("matching [" + selectedRegEx + "] against [" + string + "]");
                while (m.find()) {
                    inquirer.logger.debug("found! [");
                    extendString(s, doRepl(text, repl, m));
                    found = true;
                }
                if (found) {
                    ret.appendSearched(string);
                    inquirer.logger.debug("matched! ret=[" + ret + "]");
                }
            }

            if (ret.hasMathes()) {
                ret.setResult(s.toString());
                inquirer.logger.debug("returning ret=[" + ret + "]");

                return ret;
            }
        }
        return null;
    }

    public String evalValue(String bytes) {
        Matcher rx;
        logger.debug(this.getClass());
        if ((rx = getRegex()) == null) {
            logger.error("Empty RX");
        }
        if (bytes != null && bytes.length() > 0) {
            String[] split = StringUtils.split(bytes, "\r\n");
            String text = retValue;
            Matcher m1 = rxVar.reset(text);
            ArrayList<Repl> repl = new ArrayList<>();
            while (m1.find()) {
                repl.add(new Repl(m1.start(0), m1.end(0), Integer.parseInt(m1.group(1))));
            }
            StringBuilder s = new StringBuilder();
            for (String string : split) {
                Matcher m = rx.reset(string);
                while (m.find()) {
                    extendString(s, doRepl(text, repl, m));
                }
            }
            return s.toString();
        }
        return null;
    }

    private static class Repl {

        private final int start;
        private final int end;
        private final int id;

        Repl(int start, int end, int id) {
            this.start = start;
            this.end = end;
            this.id = id;
        }
    }

    public static class SearchSample extends Pair<String, String> {

        public SearchSample() {
        }

        private void appendSearched(String string) {
            String s = RegExUtils.replaceAll(string, "\t", "        ");
            String key = getKey();
            if (key == null || key.isEmpty()) {
                setKey(s);
            } else {
                setKey(key + " | " + s);
            }
        }

        private boolean hasMathes() {
            String key = getKey();
            return !(key == null || key.isEmpty());
        }

        private void setResult(String toString) {
            setValue(toString);
        }

        @Override
        public String toString() {
            String key = getKey();
            String val = getValue();
            return ((key == null) ? "null" : key) + "->" + ((val == null) ? "null" : val);
        }

    }
}
