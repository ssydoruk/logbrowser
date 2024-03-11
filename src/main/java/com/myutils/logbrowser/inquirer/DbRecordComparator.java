package com.myutils.logbrowser.inquirer;

import java.sql.SQLException;
import java.util.*;

class DbRecordComparator implements Comparator<ILogRecord> {

    private HashMap m_sipRecords;
    private HashMap m_tlibRecords;
    private HashMap m_anchors;
    private HashMap m_sipAnchors;
    private HashMap m_sipMasters;
    private RuntimeQuery orsQuery = null;
    private RuntimeQuery tlibQuery = null;

    public DbRecordComparator() throws SQLException {
    }

    public DbRecordComparator(HashMap sip_records, HashMap tlib_records) throws SQLException {
        this();
        if (DatabaseConnector.TableExist("ors_logbr")) {
            orsQuery = new RuntimeQuery(
                    "SELECT max(seqno) FROM ors_logbr AS ors"
                            + " INNER JOIN file_logbr AS file ON  file.id=ors.fileId WHERE"
                            + " ors.fileid<=? and ors.line <= ? and "
                            + " file.appnameid in (select id from app where name = ?) "
                            + " and ors.sourceid=?");
        }

        if (DatabaseConnector.TableExist("tlib_logbr")) {
            tlibQuery = new RuntimeQuery(
                    "SELECT max(seqno) FROM tlib_logbr AS tlib "
                            + "INNER JOIN file_logbr AS file ON  file.id=tlib.fileId WHERE "
                            + "fileid<=? and line <= ? "
                            + "and file.appnameid in (select id from app where name = ?) "
            );
        }

        m_sipRecords = sip_records;
        m_tlibRecords = tlib_records;

        m_anchors = new HashMap<>();
        if (m_tlibRecords != null) {
            Set keys = m_tlibRecords.keySet();
            for (Object key : keys) {
                Integer iKey = (Integer) key;
                ILogRecord val = (ILogRecord) m_tlibRecords.get(iKey);
                int anchor = val.GetAnchorId();
                if (anchor == 0) {
                    continue;
                }
                if (!m_anchors.containsKey(anchor)) {
                    m_anchors.put(anchor, new HashMap());
                }
                HashMap current = (HashMap) m_anchors.get(anchor);
                current.put(iKey, val);
            }
        }

        m_sipAnchors = new HashMap();
        m_sipMasters = new HashMap();
        if (m_sipRecords != null) {
            Set sipkeys = m_sipRecords.keySet();
            for (Object key : sipkeys) {
                Integer iKey = (Integer) key;
                ILogRecord val = (ILogRecord) m_sipRecords.get(iKey);
                int anchor = val.GetAnchorId();
                if (!m_sipAnchors.containsKey(anchor)) {
                    m_sipAnchors.put(anchor, new HashMap());
                }
                HashMap current = (HashMap) m_sipAnchors.get(anchor);
                current.put(iKey, val);

                if (val.getFieldValue("comp").equals("CM")) {
                    if (!m_sipMasters.containsKey(anchor)) {
                        m_sipMasters.put(anchor, val);
                    }
                }
            }
        }
    }

    public void Close() {
    }

    protected ILogRecord GetCustomItemOwner(boolean isSip, int id) {
        ILogRecord owner = null;
        if (isSip && m_sipRecords != null && id != 0) {
            if (m_sipRecords.containsKey(id)) {
                owner = (ILogRecord) m_sipRecords.get(id);
            }

        } else if (!isSip && m_tlibRecords != null && id != 0) {
            if (m_tlibRecords.containsKey(id)) {
                owner = (ILogRecord) m_tlibRecords.get(id);
            }
        }
        return owner;
    }

    protected int CompareCustomItems1(ILogRecord item1,
                                      ILogRecord item2) throws Exception {
        MsgType itemType1 = item1.GetType();
        MsgType itemType2 = item2.GetType();

        if (itemType1.equals(itemType2)) {
            // both items are custom lines
            ILogRecord owner1 = GetCustomItemOwner(
                    item1.getFieldValue("issip").equals("1"),
                    Integer.parseInt(item1.getFieldValue("ownerid")));

            ILogRecord owner2 = GetCustomItemOwner(
                    item2.getFieldValue("issip").equals("1"),
                    Integer.parseInt(item2.getFieldValue("ownerid")));

            if (owner1 == null && owner2 == null) {
                // if items are in the same file
                // arrange in order they arrear in the file
                if (Objects.equals(item1.GetFileId(), item2.GetFileId())) {
                    return item1.GetLine() - item2.GetLine();
                }
                return 0;
            }

            if (owner1 == null) {
                return 1;
            }

            if (owner2 == null) {
                return -1;
            }

            return compare(owner1, owner2);
        }

//        if (itemType2 == MsgType.CUSTOM) {
//            return -CompareCustomItems(item2, item1);
//        }
        String GetField = item1.getFieldValue("ownerid");
        if (GetField == null || GetField.isEmpty()) {
            return 1;
        }
        int ownerid;
        try {
            ownerid = Integer.parseInt(GetField);
        } catch (NumberFormatException numberFormatException) {
            inquirer.logger.error("Cannot parse [" + GetField + "]", numberFormatException);
            return 1;
        }
        int issip = item1.getFieldValue("issip").equals("1") ? 1 : 0;
        long item2id = item2.getID();

        if (ownerid == item2id && ((issip == 1 && itemType2 == MsgType.SIP)
                || (issip == 0 && itemType2 == MsgType.TLIB))) {
            // item2 is owner of item1, put item1 after item2
            return 1;
        }

        ILogRecord owner = GetCustomItemOwner(issip == 1, ownerid);
        if (owner == null) {
            return 1;
        }
        return compare(owner, item2);
    }

    protected int CompareXsRecords(ILogRecord item1,
                                   ILogRecord item2) {
        // we already know they have same timestamp and one of them is Json in SIP

        if (item2.GetType() == MsgType.JSON) {
            return -CompareXsRecords(item2, item1);
        }

        if (item2.GetType() == MsgType.TLIB) {
            if (((TLibEvent) item2).GetTriggerFileId() == item1.GetFileId()) {
                int trigline = Integer.parseInt(item2.getFieldValue("trigline"));
                int jsonline = item1.GetLine();
                return jsonline - trigline;
            }
        }

        if (item1.hasField("sipid")) {
            int sipid = Integer.parseInt(item1.getFieldValue("sipid"));
            if (m_sipRecords != null && m_sipRecords.containsKey(sipid)) {
                ILogRecord masterSipItem = (ILogRecord) m_sipRecords.get(sipid);
                int sipItemDifference = this.compare(masterSipItem, item2);
                if (sipItemDifference > 0) {
                    return 1;
                }
                return -1;
            }
        }

        // could not find SIP
        if (item1.IsInbound()) {
            return -1;
        } else {
            return 1;
        }
    }

    private String EqCondition(String val) {
        if (val.equals("null")) {
            return " IS NULL";
        } else {
            return "=\"" + val + "\"";
        }
    }

    private int CmpSip(ILogRecord record1, ILogRecord record2) throws Exception {
        // first check if clones
        int anchor1 = record1.GetAnchorId();
        int anchor2 = record2.GetAnchorId();
        inquirer.logger.trace("CmpSip");

        if (anchor1 == anchor2) {
            return 0;
        }

        // quick check if in one file
        int fileId1 = record1.GetFileId();
        int fileId2 = record2.GetFileId();
        int fileIdDiff = fileId1 - fileId2;
        if (fileIdDiff == 0) {
            return (int) (record1.GetFileOffset() - record2.GetFileOffset());
        }

        HashMap hash1 = (m_sipAnchors == null) ? null : (HashMap) m_sipAnchors.get(anchor1);
        HashMap hash2 = (m_sipAnchors == null) ? null : (HashMap) m_sipAnchors.get(anchor2);
        if (hash1 != null && hash2 != null) {
            Set keys1 = hash1.keySet();
            Set keys2 = hash2.keySet();
            for (Object key1 : keys1) {
                for (Object key2 : keys2) {
                    ILogRecord rec1 = (ILogRecord) hash1.get(key1);
                    ILogRecord rec2 = (ILogRecord) hash2.get(key2);
                    fileId1 = rec1.GetFileId();
                    fileId2 = rec2.GetFileId();
                    if (fileId1 == fileId2) {
                        return (int) (rec1.GetFileOffset() - rec2.GetFileOffset());
                    }
                }
            }
        }

        // looks like these two are competely unrelated, lets' sort by file
        return fileIdDiff;
    }

    private int CmpTlib(ILogRecord record1, ILogRecord record2) {
        // quick check if in one file

        // we get here only when different applications
        //!!!! need to account for TServer name
        inquirer.logger.trace("CmpTlib");

        TLibEvent ev1 = (TLibEvent) record1;
        TLibEvent ev2 = (TLibEvent) record2;

        /*
         * todo: verify TServer.
         */
        long seqNo1 = ev1.getSeqNo();
        long seqNo2 = ev2.getSeqNo();

        if (seqNo1 > 0 && seqNo2 > 0) { // apples to apples
            String src = ev1.getSource();
            if (ev1.IsInbound() && src != null && src.equals(ev2.getApp())) {
                long diff = seqNo1 - seqNo2;
                if (diff == 0) // TServer sends event before TClient receives it
                {
                    return 1;
                } else {
                    return (diff > 0) ? 1 : -1;
                }
            }
            src = ev2.getSource();
            if (ev2.IsInbound() && src != null && src.equals(ev1.getApp())) {
                long diff = seqNo1 - seqNo2;
                if (diff == 0) // TServer sends event before TClient receives it
                {
                    return -1;
                } else {
                    return (diff > 0) ? 1 : -1;
                }
            }
        }

        if (record1.GetType() == MsgType.PROXY) {
            return -1;
        }

        if (record2.GetType() == MsgType.PROXY) {
            return 1;
        }

        if ((record1 instanceof TLibEvent)
                && (record2 instanceof TLibEvent)) {
            String name1 = record1.getFieldValue("name");
            String name2 = record1.getFieldValue("name");

            if (name1.startsWith("ISCC")
                    && name1.equalsIgnoreCase(name2)
                    && (((TLibEvent) record1).GetReferenceId() == ((TLibEvent) record2).GetReferenceId())) {
                return (record1.IsInbound() ? 1 : -1);
            }
        }

        //--> stepan. Add here timestamp sort
        Long time1 = record1.GetUnixTime();
        Long time2 = record2.GetUnixTime();

        long timeDifference = time1 - time2;
        if (timeDifference != 0) {
            return (int) timeDifference;
        }
        //<---

        // first check if clones
        int anchor1 = record1.GetAnchorId();
        int anchor2 = record2.GetAnchorId();
        if (anchor1 == anchor2 && anchor1 != 0) {
            return 0;
        }

        if (m_anchors != null) {
            HashMap hash1 = (HashMap) m_anchors.get(anchor1);
            HashMap hash2 = (HashMap) m_anchors.get(anchor2);
            if (hash1 != null && hash2 != null) {
                Set keys1 = hash1.keySet();
                Set keys2 = hash2.keySet();
                for (Object key1 : keys1) {
                    for (Object key2 : keys2) {
                        ILogRecord rec1 = (ILogRecord) hash1.get(key1);
                        ILogRecord rec2 = (ILogRecord) hash2.get(key2);
                        if (Objects.equals(rec1.GetFileId(), rec2.GetFileId())) {
                            return (int) (rec1.GetFileOffset() - rec2.GetFileOffset());
                        }
                    }
                }
            }
        }

        // hack: put request first
        if ((record1 instanceof TLibEvent)
                && (record2 instanceof TLibEvent)) {
            if (record1.getFieldValue("name").startsWith("Request")
                    && record2.getFieldValue("name").startsWith("Event")) {
                return -1;
            }
            if (record2.getFieldValue("name").startsWith("Request")
                    && record1.getFieldValue("name").startsWith("Event")) {
                return 1;
            }
        }

        // looks like these two are competely unrelated, lets' sort by file
        return (record1.GetFileId() - record2.GetFileId());
    }

    private int CmpSipTlib(ILogRecord record1, ILogRecord record2) {

        inquirer.logger.trace("CmpSipTlib1");
        if (record1.GetType() == MsgType.SIP) {
            return -CmpSipTlib(record2, record1);
        }
        // First let's check if TLib is proxied
        if (record1.GetType() == MsgType.PROXY) {
            return -1;
        }
        // OK, TLib is first one. Let's find an initiator
        inquirer.logger.trace("-00-");
        int masterSipId = record1.getFieldValue("sipid", 0);
        inquirer.logger.trace("-0-" + masterSipId);
        if (masterSipId == 0 && record1.GetType() == MsgType.TLIB && record1 instanceof TLibEvent) {
            inquirer.logger.trace("-1-");
            if (((TLibEvent) record1).GetTriggerFileId() == record2.GetFileId()) {
                inquirer.logger.trace("-2-");
                int trigline = Integer.parseInt(record1.getFieldValue("trigline"));
                int sipline = record2.GetLine();
                return trigline - sipline;
            }
            inquirer.logger.trace("-3-");
            return record1.getFieldValue("name").startsWith("Event") ? 1 : -1;
        }

        long sipId = record2.getID();
        if (sipId == masterSipId) {
            // TLIB item was initiated by SIP item, so place SIP item before TLIB one
            inquirer.logger.trace("-4-");
            return 1;
        }

        // retrieve initiator SIP record
        // if not in hash, place TLIB item first
        if (m_sipRecords != null && !m_sipRecords.containsKey(masterSipId) && record1.GetType() == MsgType.TLIB) {
            inquirer.logger.trace("-5-");
            if (((TLibEvent) record1).GetTriggerFileId() == record2.GetFileId()) {
                inquirer.logger.trace("-6-");
                int trigline = Integer.parseInt(record1.getFieldValue("trigline"));
                int sipline = record2.GetLine();
                inquirer.logger.trace("-7-");
                return trigline - sipline;
            }
            inquirer.logger.trace("-8-");
            return record1.getFieldValue("name").startsWith("Event") ? 1 : -1;
        }
        inquirer.logger.trace("-9-");
        ILogRecord masterSipItem = (m_sipRecords == null) ? null : (ILogRecord) m_sipRecords.get(masterSipId);
        if (masterSipItem != null) {
            int sipItemDifference = this.compare(masterSipItem, record2);
            if (sipItemDifference >= 0) {
                // SIP initiator's placed AFTER our SIP item, so our SIP item goes
                // before TLIB one as well
                inquirer.logger.trace("-10-");
                return 1;
            }
        }

        // place TLIB before SIP by default
        inquirer.logger.trace("-11-");
        return -1;
    }

    @Override
    public int compare(ILogRecord o1, ILogRecord o2) {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeInterruptException();
        }
        try {
            ILogRecord record1 = (ILogRecord) o1;
            ILogRecord record2 = (ILogRecord) o2;
            MsgType itemType1 = record1.GetType();
            MsgType itemType2 = record2.GetType();

            //stepan-->
            inquirer.logger.trace("compare: >" + record1);
            inquirer.logger.trace("compare: <" + record2);
            // if the same file - compare line numbers, does not matter what record type is
            if (Objects.equals(record1.GetFileId(), record2.GetFileId())) {
                inquirer.logger.trace("Same file; return " + (record1.GetFileOffset() - record2.GetFileOffset()));
                return (int) (record1.GetFileOffset() - record2.GetFileOffset());
            }
            // same app, different log files
            if (record1.getAppnameid() == record2.getAppnameid()) {
                inquirer.logger.trace("Same app");
                if (record1.getM_component() == record2.getM_component())// for SIP multithreaded SIP Server 
                {
                    inquirer.logger.trace("\tSame component");
                    return (int) (record1.GetUnixTime() - record2.GetUnixTime());
                } else {
                    inquirer.logger.trace("\tSame component else");
                    Long time1 = record1.GetUnixTime();
                    Long time2 = record2.GetUnixTime();

                    long timeDifference = time1 - time2;
                    if (timeDifference != 0) {
                        inquirer.logger.trace("rely on time difference " + timeDifference);
                        return (int) timeDifference;
                    }

                    inquirer.logger.trace("detailed compare");
                    if ((itemType1 == MsgType.TLIB && itemType2 == MsgType.SIP)
                            || (itemType2 == MsgType.TLIB && itemType1 == MsgType.SIP)) {
                        return CmpSipTlib(record1, record2);
                    } else {
                        inquirer.logger.trace("not Same comp ret:" + ((int) (record1.GetUnixTime() - record2.GetUnixTime())));
                        return (int) (record1.GetUnixTime() - record2.GetUnixTime());
                    }
                }
            }
            //stepan<--

            inquirer.logger.trace("detailed compare");

            if (record1.isTLibType() && record2.isTLibType()) {
                return CmpTlib(record1, record2);
            }

            if ((record1.isTLibType() && itemType2 == MsgType.ORSM)
                    || (record2.isTLibType() && itemType1 == MsgType.ORSM)) {
                return CmpTlibOrsMetric(record1, record2);
            }

            // postpone sort by time as of yet
            Long time1 = record1.GetUnixTime();
            Long time2 = record2.GetUnixTime();

            long timeDifference = time1 - time2;
            if (timeDifference != 0) {
                return (int) timeDifference;
            }

//            if (itemType1 == MsgType.CUSTOM
//                    || itemType2 == MsgType.CUSTOM) {
//                return CompareCustomItems(record1, record2);
//            }
            if (itemType1 == MsgType.JSON
                    || itemType2 == MsgType.JSON) {
                return CompareXsRecords(record1, record2);
            }

            if (itemType1 == MsgType.PROXY) {
                int id = Integer.parseInt(record1.getFieldValue("tlib_id"));
                if (id != 0 && m_tlibRecords.containsKey(id)) {
                    record1 = (ILogRecord) m_tlibRecords.get(id);
                }
            }
            if (itemType2 == MsgType.PROXY) {
                int id = Integer.parseInt(record2.getFieldValue("tlib_id"));
                if (id != 0 && m_tlibRecords.containsKey(id)) {
                    record2 = (ILogRecord) m_tlibRecords.get(id);
                }
            }

            boolean isSip1 = (itemType1 == MsgType.SIP);
            boolean isSip2 = (itemType2 == MsgType.SIP);

            if (isSip1 && isSip2) {
                return CmpSip(record1, record2);
//            } else if (!isSip1 && !isSip2) {
//                return CmpTlib(record1, record2);
//            } else {
//                return CmpSipTlib(record1, record2);
            } else {
                return 0;
            }
        } catch (Exception ex) {
            inquirer.logger.error("Exception while sorting", ex);
            return 0;
        }
    }

    private int BlockSortSip(ArrayList<ILogRecord> list, int start) throws Exception {
        int end = start;
        ILogRecord first = (ILogRecord) list.get(start);
        ILogRecord last = (ILogRecord) list.get(end);
        int anchor1 = first.GetAnchorId();
        int anchor2 = last.GetAnchorId();
        while ((anchor1 == anchor2) && (anchor1 != 0)) {
            end++;
            if (end == list.size()) {
                break;
            }
            last = (ILogRecord) list.get(end);
            anchor2 = last.GetAnchorId();
        }

        if (end - start <= 1) {
            return 1;
        }

        ArrayList<ILogRecord> result = new ArrayList<>();

        int srv = -1;
        for (int i = start; i < end; i++) {
            ILogRecord current = (ILogRecord) list.get(i);
            int cmp = Integer.parseInt(current.getFieldValue("component"));
            if (cmp == 1 || cmp == 0) {
                srv = i;
                break;
            }
        }
        if (srv == -1) {
            //don't know, what to do without SipServer record
            return end - start;
        }

        ILogRecord rec = (ILogRecord) list.get(srv);

        if (!rec.IsInbound()) {
            // starts with sipserver, then proxies
            result.add(rec);
            for (int i = start; i < end; i++) {
                String via = rec.getFieldValue("via");
                ILogRecord current = (ILogRecord) list.get(i);
                if (current.getFieldValue("via").equals(via)
                        && current.IsInbound()) {
                    ILogRecord next = current;
                    result.add(next);
                    for (int j = start; j < end; j++) {
                        current = (ILogRecord) list.get(j);
                        if (Objects.equals(current.GetFileId(), next.GetFileId())
                                && !current.IsInbound()) {
                            result.add(current);
                            rec = current;
                        }
                    }
                }
            }

            for (int i = 0; i < end - start; i++) {
                list.set(start + i, result.get(i));
            }
        } else {
            result.add(rec);
            for (int i = start; i < end; i++) {
                String via = rec.getFieldValue("via");
                ILogRecord current = (ILogRecord) list.get(i);
                if (current.getFieldValue("via").equals(via)
                        && !current.IsInbound()) {
                    ILogRecord next = current;
                    result.add(next);
                    for (int j = start; j < end; j++) {
                        current = (ILogRecord) list.get(j);
                        if (Objects.equals(current.GetFileId(), next.GetFileId())
                                && current.IsInbound()) {
                            result.add(current);
                            rec = current;
                        }
                    }
                }
            }

            for (int i = 0; i < end - start; i++) {
                list.set(end - i - 1, result.get(i));
            }
        }

        return end - start;
    }

    private int BlockSortTlib(ArrayList<ILogRecord> list, int start) throws Exception {
        int end = start;
        ILogRecord first = (ILogRecord) list.get(start);
        ILogRecord last = (ILogRecord) list.get(end);
        int anchor1 = first.GetAnchorId();
        int anchor2 = last.GetAnchorId();
        while ((anchor1 == anchor2) && (anchor1 != 0)) {
            end++;
            if (end == list.size()) {
                break;
            }
            last = (ILogRecord) list.get(end);
            anchor2 = last.GetAnchorId();
        }

        if (end - start <= 1) {
            return 1;
        }

        ArrayList<ILogRecord> result = new ArrayList<>();
        ArrayList<ILogRecord> unused = new ArrayList<>();
        for (int i = start; i < end; i++) {
            unused.add((ILogRecord) list.get(i));
        }
        Deque<ILogRecord> queue = new LinkedList();

        // find originator
        ILogRecord orig = null;
        if (first.getFieldValue("name").startsWith("Request")) {
            for (ILogRecord rec : unused) {
                if ((Integer.parseInt(rec.getFieldValue("comptype")) < 5)
                        && rec.IsInbound()
                        && !rec.getFieldValue("source").contains("tController")
                        && !rec.getFieldValue("source").contains("sessionController")
                        && !rec.getFieldValue("source").contains("interactionProxy")) {
                    orig = rec;
                    break;
                }
            }
        } else {
            for (ILogRecord rec : unused) {
                if ((Integer.parseInt(rec.getFieldValue("comptype")) < 5)
                        && !rec.IsInbound()) {
                    ILogRecord prev = null;
                    for (ILogRecord curr : unused) {
                        if (Objects.equals(curr.GetFileId(), rec.GetFileId())
                                && curr.GetLine() < rec.GetLine()) {
                            prev = curr;
                            break;
                        }
                    }
                    if (prev == null) {
                        orig = rec;
                        break;
                    }
                }
            }
        }

        if (orig == null) {
            return end - start; // nothing to do here
        }

        if (orig.getFieldValue("name").startsWith("Request")) {
            result.add(orig);
            unused.remove(orig);
            // for request, try to find its outbound
            ArrayList<ILogRecord> temp = new ArrayList<>();
            for (ILogRecord rec : unused) {
                if (!rec.IsInbound() && Objects.equals(rec.GetFileId(), orig.GetFileId())) {
                    temp.add(rec);
                }
            }
            for (ILogRecord rec : temp) {
                result.add(rec);
                unused.remove(rec);
                queue.addLast(rec);
            }
        } else {
            queue.addLast(orig);
            result.add(orig);
            unused.remove(orig);
        }

        while (!queue.isEmpty()) {
            ILogRecord out = queue.removeFirst();
            ArrayList<ILogRecord> temp = new ArrayList<>();
            for (ILogRecord inb : unused) {
                if (inb.getFieldValue("source").equals(out.getFieldValue("component"))
                        || inb.getFieldValue("source").equals("00000000 sessionController")
                        && out.getFieldValue("component").contains("sessionController")) {
                    // this is from that outbound
                    temp.add(inb);
                    // search for outbounds
                    for (ILogRecord rec : unused) {
                        if (!rec.IsInbound() && Objects.equals(rec.GetFileId(), inb.GetFileId())) {
                            temp.add(rec);
                            queue.addLast(rec);
                        }
                    }
                }
            }
            for (ILogRecord rec : temp) {
                result.add(rec);
                unused.remove(rec);
            }
        }

        if (orig.getFieldValue("name").startsWith("Request")) {
            // for requests, put unused first
            int i = start;
            for (ILogRecord rec : unused) {
                list.set(i++, rec);
            }
            for (ILogRecord rec : result) {
                list.set(i++, rec);
            }
        } else {
            int i = start;
            for (ILogRecord rec : result) {
                list.set(i++, rec);
            }
            for (ILogRecord rec : unused) {
                list.set(i++, rec);
            }
        }

        return end - start;
    }

    public void BlockSort(ArrayList<ILogRecord> list) throws Exception {
        int start = 0;
        int len = list.size();
        while (start < len) {
            ILogRecord item = (ILogRecord) list.get(start);
            MsgType type = item.GetType();
            if (type == MsgType.SIP) {
                try {
                    start += BlockSortSip(list, start);
                } catch (Exception e) {
                    start++;
                }
            } else if (type == MsgType.TLIB) {
                try {
                    start += BlockSortTlib(list, start);
                } catch (Exception e) {
                    start++;
                }
            } else {
                start++;
            }
        }
    }

    private int CmpTlibOrsMetric(ILogRecord record1, ILogRecord record2) throws SQLException {
        long seqNo1;
        long seqNo2;

        if (record1.GetType() == MsgType.ORSM) {
            seqNo1 = GetORSSeqNo(record1.GetFileId(), record1.GetLine(), record1.getM_appName(), ((TLibEvent) record2).getApp());
            seqNo2 = EvGetSeqNo(record2.GetFileId(), record2.GetLine(), (TLibEvent) record2);

            if (seqNo1 > 0 && seqNo2 > 0) { // apples to apples
                long ret = seqNo1 - seqNo2;
                if (ret == 0) // same event 
                {
                    return -1; // TServer event first
                } else {
                    return (ret > 0) ? 1 : -1;
                }
            }
        } else if (record2.GetType() == MsgType.ORSM) {
            seqNo1 = EvGetSeqNo(record1.GetFileId(), record1.GetLine(), (TLibEvent) record1);
            seqNo2 = GetORSSeqNo(record2.GetFileId(), record2.GetLine(), record2.getM_appName(), ((TLibEvent) record1).getApp());

            if (seqNo1 > 0 && seqNo2 > 0) { // apples to apples
                long ret = seqNo1 - seqNo2;
                if (ret == 0) // same event 
                {
                    return 1; // TServer event first
                } else {
                    return (ret > 0) ? 1 : -1;
                }
            }
        }

        return 0;
    }

    private long EvGetSeqNo(int FileID, int Line, TLibEvent ev) throws SQLException {
        if (!ev.IsInbound()) // request
        {
            return ev.getSeqNo();
        } else {
            if (tlibQuery != null) {
                Object[] queryParams = {
                        FileID,
                        Line,
                        ev.getApp()
                };
                return tlibQuery.getSingleValue(queryParams);
            } else {
                return 0;
            }
        }
    }

    private int GetORSSeqNo(int GetFileId, int GetLine, String ORSName, String TServerName) throws SQLException {
        if (orsQuery != null) {
            Object[] queryParams = {
                    GetFileId,
                    GetLine,
                    ORSName,
                    TServerName
            };
            return orsQuery.getSingleValue(queryParams);
        } else {
            return 0;
        }
    }
}
