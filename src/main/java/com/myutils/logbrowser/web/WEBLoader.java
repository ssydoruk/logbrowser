package com.myutils.logbrowser.web;

import com.myutils.logbrowser.indexer.EnvIndexer;
import com.myutils.logbrowser.indexer.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;
import java.net.URI;

/**
 * Main class.
 */
public class WEBLoader {
    // Base URI the Grizzly HTTP server will listen on
    private final EnvIndexer env;
    com.myutils.logbrowser.indexer.Main indexer;
    private static Logger logger = LogManager.getLogger(WEBLoader.class);

    HttpServer server = null;

    final String base_uri;

    public WEBLoader(EnvIndexer ee) {
        this.env = ee;
        base_uri = "http://localhost:" + ee.getHttpPort() + "/";
    }

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
     * application.
     *
     * @return Grizzly HTTP server.
     */
    public HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.mycompany.grizzlytest package
        // final ResourceConfig rc = new
        // ResourceConfig().packages("com.myutils.logbrowser.web");
        final ResourceConfig rc = new ResourceConfig().register(MyResource.class);

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(base_uri), rc);

    }

    public void listen() throws Exception {
        indexer = Main.getNewInstance().init(env);
        if (!indexer.kickQueueManager()) {
            logger.error("Failed to init logbrowser");
        } else {
            server = startServer();
            logger.info(String.format("Web server listens at %s", base_uri));
        }
    }

    public void addFile(String fileName) {
        logger.info("WEB: added file [" + fileName + "]");
        indexer.processAddedFile(new File(fileName));
    }

    public void stop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (indexer != null) {
                        logger.info("WEB: stop ");
                        indexer.finishParsing();
                    }
                } catch (Exception e) {
                    logger.error("Exception while finishing parsing: " + e.getMessage(), e);
                }
                server.shutdownNow();
                System.exit(-1);
            }
        }).start();
    }

    public void die() {
        System.exit(-1);
    }
}
