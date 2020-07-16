/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 *
 * @author ssydoruk
 */
public class ConnIdRecord extends Message {

    private final String m_connId;

    private final boolean m_created;
    private boolean isTemp;
    private String newID;

    ConnIdRecord(String oldId, String newId, boolean b) {
        this(oldId, b);
        this.newID = newId;
//        Main.logger.info("ConnIdRecord: ["+oldId+"] new["+newId+"]");
    }
    public ConnIdRecord(String connId, boolean created) {
        super(TableType.ConnID);
        this.isTemp = false;
        m_connId = connId;
        m_created = created;
    }

    public String getNewID() {
        return newID;
    }

    public boolean isIsTemp() {
        return isTemp;
    }


    @Override
    public String toString() {
        return "ConnIdRecord{" + "m_connId=" + m_connId + ", m_created=" + m_created + '}' + super.toString();
    }

//    public void AddToDB(DBAccessor accessor) {
//        PreparedStatement stmt = accessor.GetStatement(m_batchId);
//        try {
//            setFieldString(stmt,1, m_connId);
//            stmt.setBoolean(2, m_created);
//            stmt.setInt(3, m_fileId);
//            accessor.SubmitStatement(m_batchId);
//        } catch (Exception e) {
//            System.out.println("Could not add ConnId manipulations record: "+e);
//        }
//    }
    public String getM_connId() {
        return m_connId;
    }

    public boolean isM_created() {
        return m_created;
    }

    void setTempConnid(boolean b) {
        this.isTemp = b;
    }

}
