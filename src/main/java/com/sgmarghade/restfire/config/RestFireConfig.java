package com.sgmarghade.restfire.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meltmedia.dropwizard.mongo.MongoConfiguration;
import io.dropwizard.Configuration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by swapnil on 09/03/16.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestFireConfig extends Configuration {

    @JsonProperty
    private MongoConfiguration mongo;
    private Integer workerThreads;
    private String bigQueueFolderPath;
    private String bigQueueName;
    private Integer threadSleepTimeSec;
    private Integer dropMessagePostMinutes;

}
