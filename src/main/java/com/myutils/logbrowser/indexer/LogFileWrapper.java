/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;

/**
 * @author Stepan
 */
public abstract class LogFileWrapper {

    private final File file;
    private final HashMap<FileInfo, File> fileInfoFileMap = new HashMap<>();
    private final String baseDir;
    private boolean ignoreLog = false;

    LogFileWrapper(File file, String baseDir) throws IOException {
        this.file = file;
        this.baseDir = baseDir;
    }

    public static LogFileWrapper getContainer(File file, String baseDir) {
        LogFileWrapper ret = null;
        try {
            String name=file.getName().toLowerCase();
            if (name.endsWith(".log")) {
                ret = new TextLog(file, baseDir);
            } else if (!Main.getMain().isIgnoreZIP()) {
                if(name.endsWith(".tgz") || name.endsWith(".tar.gz")){
                    return new TarGZLog(file, baseDir);
                } else if (name.endsWith(".zip")) {
                    return new ZIPLog(file, baseDir);
                } else if (name.endsWith(".gz")) {
                    return new GZLog(file, baseDir);
                }
//                String ext=FilenameUtils.getExtension(file.getName()).toLowerCase();
//                if (ext.equals("zip") ) {
//                    return new ZIPLog(file, baseDir);
//                } else if (ext.equals("gz") || ext.equals("tgz")) {
//                    try(FileInputStream fin = new FileInputStream(file);
//                        BufferedInputStream bin = new BufferedInputStream(fin) ){
//                        CompressorInputStream input = new CompressorStreamFactory()
//                                .createCompressorInputStream(bin);
//                        byte[] bytes = input.readNBytes(1000);
//                        String s = new String(bytes);
//                        System.out.println(s);
//
//                    } catch (CompressorException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    return new GZLog(file, baseDir);
//                }
            }
        } catch (IOException ex) {
            Main.logger.error("Cannot determine type of file " + file, ex);
        }
        if (ret != null && !ret.isIgnoreLog()) {
            return ret;
        }

        return null;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public String getRelativeFile(File f) {
        return Paths.get(getBaseDir()).relativize(Paths.get(f.getAbsolutePath())).toString();
    }

    public File getFile() {
        return file;
    }

    public abstract void close();

    public abstract void open() throws IOException;

    public boolean isIgnoreLog() {
        return ignoreLog;
    }

    public void setIgnoreLog(boolean ignoreLog) {
        this.ignoreLog = ignoreLog;
    }

    protected void addFileInfo(FileInfo fi, File file) {
        fileInfoFileMap.put(fi, file);
    }

    public Set<FileInfo> getFileInfos() {
        return fileInfoFileMap.keySet();
    }

    abstract InputStream getInputStream(FileInfo aThis) throws FileNotFoundException;

    /**
     * Is called for each file if Wrapper contains several
     *
     * @throws Exception
     */
    abstract void startParsing() throws Exception;

    /**
     * Is called for each file if Wrapper contains several
     *
     * @throws Exception
     */
    abstract void doneParsing() throws Exception;

    public String getName() {
        return file.getPath();
    }

}
