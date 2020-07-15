package com.myutils.logbrowser.inquirer;

import java.io.IOException;

public abstract class RandomFileReader {

    public abstract int Read(long offset, int bytes, byte[] buf) throws IOException;

    abstract void close() throws IOException;

    public abstract boolean isInited();
}
