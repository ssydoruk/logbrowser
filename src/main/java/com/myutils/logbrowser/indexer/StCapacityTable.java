/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class StCapacityTable extends DBTable {

    private static final int MAX_MEDIA = 5;

    public StCapacityTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("stcapacity");
        addIndex("time");
        addIndex("FileId");

        /* standard first */
        addIndex("typeid");
        addIndex("agentid");
        addIndex("placeid");
        addIndex("dnid");
        addIndex("capacityid");
        for (int i = 0; i < MAX_MEDIA; i++) {
            addIndex("media" + i + "nameid");
        }
        dropIndexes();

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < MAX_MEDIA; i++) {
            buf.append(",media").append(i).append("nameid integer");
            buf.append(",media").append(i).append("state character(2)");
            buf.append(",media").append(i).append("curnumber tinyint");
            buf.append(",media").append(i).append("maxnumber character(2)");
        }

        String query = "create table if not exists stcapacity (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",typeid integer"
                + ",agentid integer"
                + ",placeid integer"
                + ",dnid integer"
                + ",capacityid integer"
                + buf
                + ");";

        buf = new StringBuilder();
        for (int i = 0; i < MAX_MEDIA; i++) {
            buf.append(",?,?,?,?");
        }

        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO stcapacity VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + buf
                + ");"
        );

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        StCapacity rec = (StCapacity) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, StStatus.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.StatServerStatusType, rec.StatusType()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, rec.AgentName()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.Place, rec.PlaceName()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, rec.SingleQuotes(rec.getDn())));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.Capacity, rec.getCapacityRule()));

            int curRecNum = 11;

            ArrayList<StCapacity.MediaStatuses> mediaStatuses = rec.getMediaStatuses();
            for (int i = 0; i < MAX_MEDIA; i++) {
                int baseNo = curRecNum + i * 4;
                if (i < mediaStatuses.size()) {
                    StCapacity.MediaStatuses stat = mediaStatuses.get(i);
                    Main.logger.trace("adding stat: " + stat.toString());
                    setFieldInt(stmt, baseNo, Main.getRef(ReferenceType.Media, stat.getMedia()));
                    setFieldString(stmt, baseNo + 1, stat.getRoutable());
                    setFieldInt(stmt, baseNo + 2, stat.getCurNumber());
                    setFieldInt(stmt, baseNo + 3, stat.getMaxNumber());

                } else {
                    setFieldInt(stmt, baseNo, null);
                    setFieldString(stmt, baseNo + 1, null);
                    setFieldInt(stmt, baseNo + 2, null);
                    setFieldInt(stmt, baseNo + 3, null);
                }
            }

//            for (StCapacity.MediaStatuses stat : rec.getMediaStatuses()) {
//                stmt.setInt(curRecNum++, Main.getRef(ReferenceType.Media, stat.getMedia()));
//                stmt.setString(curRecNum++, stat.getRoutable());
//                stmt.setInt(curRecNum++, stat.getCurNumber());
//                stmt.setInt(curRecNum++, stat.getMaxNumber());
//            }
                        }
        });
    }



}
