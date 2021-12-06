package com.myutils.logbrowser.indexer;

public abstract class SIPServerBaseMessage extends Message {
    private boolean m_handlerInProgress ;
    private int m_handlerId ;

    public void setM_handlerInProgress(boolean m_handlerInProgress) {
        this.m_handlerInProgress = m_handlerInProgress;
    }

    public void setM_handlerId(int m_handlerId) {
        this.m_handlerId = m_handlerId;
    }

    public boolean isM_handlerInProgress() {
        return m_handlerInProgress;
    }

    public int getM_handlerId() {
        return m_handlerId;
    }

    public SIPServerBaseMessage(TableType type, boolean m_handlerInProgress, int m_handlerId, int fileID) {
        super(type, fileID);
        this.m_handlerInProgress = m_handlerInProgress;
        this.m_handlerId = m_handlerId;
    }
}
