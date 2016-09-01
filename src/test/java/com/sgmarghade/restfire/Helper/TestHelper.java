package com.sgmarghade.restfire.Helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgmarghade.restfire.models.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by swapnil on 11/03/16.
 */
public class TestHelper {
    public static Document getDocument(String host, int port, String apiUrl, ObjectMapper mapper) throws IOException {
        Map<String,String> map = new HashMap<>();
        map.put("key1","value1");

        Document document = new Document();
        document.setUrl("http://"+host+":"+port+"/"+apiUrl);
        document.setMethodType(Document.MethodType.POST);
        document.setBody(mapper.readValue(mapper.writeValueAsBytes(map), JsonNode.class));
        return document;

    }
}
