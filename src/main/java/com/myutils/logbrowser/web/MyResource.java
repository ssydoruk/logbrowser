package com.myutils.logbrowser.web;

import com.myutils.logbrowser.indexer.Main;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("/")
public class MyResource {
    private static Logger logger = LogManager.getLogger(MyResource.class);

    /**
     * Method handling HTTP GET requests. The returned object will be sent to the
     * client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("stop")
    @Produces(MediaType.TEXT_PLAIN)
    public String stop() {
        Main.getWebLoader().stop();
        return "Got it!";
    }

    @GET
    @Path("die")
    @Produces(MediaType.TEXT_PLAIN)
    public String die() {
        Main.getWebLoader().die();
        return "Got it!";
    }

    /**
     * add file to the queue
     * 
     * @return
     */
    @POST
    @Path("add")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String add(String fileName) {
        Main.getWebLoader().addFile(fileName);
        return "OK";
    }

}
