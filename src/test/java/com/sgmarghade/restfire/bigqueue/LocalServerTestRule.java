package com.sgmarghade.restfire.bigqueue;

import com.google.common.collect.ImmutableMap;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by swapnil on 23/12/15.
 */
public class LocalServerTestRule implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger(LocalServerTestRule.class.getSimpleName());

    private final ImmutableMap<String, HttpRequestHandler> handlers;
    private final TestHostPort hostPort;

    public LocalServerTestRule(TestHostPort hostPort,  ImmutableMap<String, HttpRequestHandler> handlers) {
        this.handlers = handlers;
        this.hostPort = hostPort;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                LocalTestServer localTestServer = new LocalTestServer(null, null);
                for(Map.Entry<String, HttpRequestHandler> handler : handlers.entrySet()) {
                    localTestServer.register(handler.getKey(), handler.getValue());
                }
                localTestServer.start();
                logger.info("Started test server");
                try {
                    hostPort.setHostName(localTestServer.getServiceAddress().getHostName());
                    hostPort.setPort(localTestServer.getServiceAddress().getPort());
                    base.evaluate();
                } finally {
                    localTestServer.stop();
                    logger.info("Stopped test server");
                }
            }
        };
    }
}
