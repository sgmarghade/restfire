package com.sgmarghade.restfire.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by swapnil on 10/03/16.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Document {

    private String id;

    private String groupId;

    private Date enqueueTimestamp = new Date();

    private Date lastRetryTimestamp = new Date();

    private Integer totalRetry = 0;

    @NotNull
    private String url;

    @NotNull
    private MethodType methodType;

    private List<Header> headers = new ArrayList<>();

    @NotNull
    private JsonNode body;

    private String callbackUrl;

    private Integer statusCode;

    private String responseMessage;

    public enum MethodType {
        POST("POST"), GET("GET"), PUT("PUT");
        String type;

        MethodType(String type) {
            this.type = type;
        }
    }


}
