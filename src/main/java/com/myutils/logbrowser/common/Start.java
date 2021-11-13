/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import com.myutils.logbrowser.indexer.Main;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author stepan_sydoruk
 */
public class Start {
    public static Logger logger = LogManager.getLogger();

    static void helpExit() {
        System.out.println("\nUsage: <Application> <inquirer|indexer> <app parameters>\n");
        System.exit(1);
    }

    private static void initLogger(String sLoaderLog1) {
        if (sLoaderLog1 == null || sLoaderLog1.isEmpty()) {
            sLoaderLog1 = "./applog";
        } else {
            System.setProperty("logPath", sLoaderLog1);

        }
        System.setProperty("log4j2.saveDirectory", "true");
        String s = System.getProperty("log4j.configurationFile");
        if (s != null && !s.isEmpty()) {
//            s = System.getProperty("program.name") + ".xml";
            logger = LogManager.getLogger();
        } else {

            ConfigurationBuilder<BuiltConfiguration> builder
                    = ConfigurationBuilderFactory.newConfigurationBuilder();

            builder.addProperty("LogFileName", sLoaderLog1);

            AppenderComponentBuilder console
                    = builder.newAppender("stdout", "Console");

            ComponentBuilder triggeringPolicies = builder.newComponent("Policies")
                    .addComponent(builder.newComponent("OnStartupTriggeringPolicy"))
                    .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                            .addAttribute("size", "20M"));

            AppenderComponentBuilder rollingFile
                    = builder.newAppender("rolling", "RollingFile");
            rollingFile.addAttribute("fileName", "${LogFileName}.log");
            rollingFile.addAttribute("filePattern", "${LogFileName}-%d{yyyyMMdd-HHmmss_SSS}.log");
            rollingFile.addComponent(triggeringPolicies);

//        FilterComponentBuilder flow = builder.newFilter(
//                "MarkerFilter",
//                Filter.Result.ACCEPT,
//                Filter.Result.DENY);
//        flow.addAttribute("marker", "FLOW");
//        console.add(flow);
            LayoutComponentBuilder standard
                    = builder.newLayout("PatternLayout");
            standard.addAttribute("pattern", "%d %5.5p %30.30C [%t] %m%n");

            console.add(standard);
            rollingFile.add(standard);

            builder.add(console);
            builder.add(rollingFile);
//        Appender appe = MyCustomAppenderImpl.createAppender("appe1", null, null, null);
//        AppenderComponentBuilder newAppender = builder.newAppender("appe", "appe1");
//        builder.add(appe);

            RootLoggerComponentBuilder rootLogger
                    = builder.newRootLogger(Level.DEBUG);
            rootLogger.add(builder.newAppenderRef("stdout"));
            rootLogger.add(builder.newAppenderRef("rolling"));
            builder.add(rootLogger);

            Configurator.initialize(builder.build());
//            System.out.println(builder.toXmlConfiguration());
            logger = LogManager.getLogger();
        }
        logger.info("log initialized");
    }
    public static void main(String[] args) throws Exception {
        Path destFile;
        File dst;
        String outputDir= "C:\\GCTI\\work\\test1";
        String newFilesDir = outputDir+"\\new";
        Files.walk(Paths.get(newFilesDir)).forEach(path -> {
            if(Files.isRegularFile(path)) {
                System.out.println(path+" deleted" );
                    FileUtils.deleteQuietly(path.toFile());
            }
        });

        ExecutionEnvironment ee = new ExecutionEnvironment();
        ee.setXmlCFG("C:\\GCTI\\etc\\logbrowser\\backend.xml");
        ee.setDbname(outputDir + "\\logbr");
        ee.setBaseDir(outputDir);
        ee.setAlias("logbr");
        System.setProperty("logPath", ee.getLogbrowserDir());
        System.setProperty("log4j2.saveDirectory", ee.getLogbrowserDir());

        Main indexer = Main.getInstance().init(ee);
//        initLogger("");

        Main.logger.info("info");
        Main.logger.debug("debug");
        Main.logger.error("error");

        Files.walk(Paths.get("C:\\GCTI\\work\\test_src")).forEach(path -> {
            if(Files.isRegularFile(path)) {
                Main.logger.info("starting copy "+path);
                try {
                    FileUtils.copyFileToDirectory(path.toFile(), new File(newFilesDir));
                    Main.logger.info("done copy "+path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                indexer.processAddedFile(
                        Paths.get(newFilesDir, path.getFileName().toString()));
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
//        Thread.sleep(10000);
        indexer.finishParsing();
    }
}
