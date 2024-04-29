package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateParsed {

    private static final ZoneId zo = ZoneId.systemDefault();
    String date;
    String rest;
    LocalDateTime fmtDate = null;
    String orig;

    DateParsed(DateParsed d) {
        date = d.date;
        rest = StringUtils.trimToEmpty(rest);
        orig = d.orig;
        fmtDate = d.fmtDate;
    }

    DateParsed(String orig, String date, String rest) {
        this.date = date;
        this.rest = StringUtils.trimToEmpty(rest);
        this.orig = orig;
    }

    DateParsed(String orig, String date, String rest, LocalDateTime f) {
        this.orig = orig;
        this.date = date;
        this.rest = StringUtils.trimToEmpty(rest);
        this.fmtDate = f;
        Main.logger.trace(toString());
    }

    @Override
    public String toString() {
        return "DateParsed{" + "date=" + date + ", rest=" + rest + ", fmtDate=" + fmtDate + '}';
    }

    void addDay() {
        fmtDate = fmtDate.plusDays(1);
        Main.logger.trace("added day to " + this);
    }

    long getUTCms() {
        return fmtDate.atZone(zo).toEpochSecond() * 1000 + fmtDate.getNano() / 1000 / 1000;

    }

}
