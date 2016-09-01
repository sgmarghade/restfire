package com.sgmarghade.restfire.bigqueue;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by swapnil on 23/12/15.
 */
public class DummyEventHandler implements HttpRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(DummyEventHandler.class.getSimpleName());
    private AtomicInteger counter = new AtomicInteger(0);


    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
        try {
            counter.addAndGet(1);
        } catch (Exception e) {
            logger.error("Error: ", e);
            response.setStatusCode(500);
            return;
        }
        response.setStatusCode(201);
    }

    public AtomicInteger getCounter() {
        return counter;
    }
}
