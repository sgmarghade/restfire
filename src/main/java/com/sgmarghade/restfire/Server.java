package com.sgmarghade.restfire;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import com.meltmedia.dropwizard.mongo.MongoBundle;
import com.mongodb.DBCollection;
import com.sgmarghade.restfire.bigqueue.ThreadManager;
import com.sgmarghade.restfire.config.RestFireConfig;
import com.sgmarghade.restfire.resources.DocumentResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import minisu.dropwizard.interpolation.EnvironmentVariableInterpolationBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for server.
 *
 */
public class Server extends Application<RestFireConfig>
{
    private Logger logger = LoggerFactory.getLogger(Server.class);
    private MongoBundle<RestFireConfig> mongoBundle;
    private static String COLLECTION = "documents";
    public static void main( String[] args ) throws Exception {
        new Server().run(args);
    }

    public void initialize(Bootstrap bootstrap) {
        bootstrap.addBundle(new EnvironmentVariableInterpolationBundle());
        bootstrap.addBundle(mongoBundle = MongoBundle.<RestFireConfig>builder()
                .withConfiguration(RestFireConfig::getMongo)
                .build());
    }

    @Override
    public void run(RestFireConfig restFireConfig, Environment environment) throws Exception {
        ObjectMapper objectMapper = environment.getObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        SubtypeResolver subtypeResolver = new StdSubtypeResolver();
        objectMapper.setSubtypeResolver(subtypeResolver);
        logger.info("Bigqueue path is : "+ restFireConfig.getBigQueueFolderPath()+ " : "+ restFireConfig.getDropMessagePostMinutes());
        IBigQueue bigQueue = new BigQueueImpl(restFireConfig.getBigQueueFolderPath(), restFireConfig.getBigQueueName());
        DBCollection collection = mongoBundle.getDB().getCollection(COLLECTION);
        ThreadManager threadManager = new ThreadManager(restFireConfig,bigQueue,objectMapper, collection);
        environment.lifecycle().manage(threadManager);

        environment.jersey().register(new DocumentResource(bigQueue,objectMapper,collection));
    }
}
