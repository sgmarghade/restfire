package com.sgmarghade.restfire.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leansoft.bigqueue.IBigQueue;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.sgmarghade.restfire.models.Document;
import com.sgmarghade.restfire.models.Replay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by swapnil on 10/03/16.
 */

@Path("/v1/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {
    private final IBigQueue bigqueue;
    private final ObjectMapper mapper;
    private final DBCollection collection;
    private Logger logger = LoggerFactory.getLogger(DocumentResource.class);

    public DocumentResource(IBigQueue bigqueue, ObjectMapper mapper,DBCollection collection){
        this.bigqueue = bigqueue;
        this.mapper = mapper;
        this.collection = collection;
    }


    @POST
    public Response saveDocument(Document document){
        logger.info("Input documents are "+document);
        try {
            if(document.getId() == null) {
                document.setId(UUID.randomUUID().toString());
            }

            bigqueue.enqueue(mapper.writeValueAsBytes(document));

            return Response.created(URI.create("/")).build();
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/bulk")
    public Response saveDocuments(List<Document> documents){
        logger.info("Input documents are "+documents);
        try {
            for (Document document : documents) {
                document.setId(UUID.randomUUID().toString());
                bigqueue.enqueue(mapper.writeValueAsBytes(document));
            }
            return Response.created(URI.create("/")).build();
        }catch(IOException e){
            logger.error(e.getMessage(),e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    public Response getFailedDocuments(@QueryParam("fromTime")Long fromTime, @QueryParam("toTime")Long toTime,@QueryParam("id")String id,@QueryParam("methodType")String methodType){
        List<Object> documents = new ArrayList<>();
        BasicDBObject obj = new BasicDBObject();

        if(fromTime != null){
            obj.put("document.enqueueTimestamp",new BasicDBObject("$gte", fromTime));
        }

        if(toTime != null){
            obj.put("document.enqueueTimestamp",new BasicDBObject("$lte", toTime));
        }

        if(id != null){
            obj.put("document.id",new BasicDBObject("$eq",id));
        }

        if(methodType != null){
            obj.put("document.methodType",new BasicDBObject("$eq",methodType));
        }

        DBCursor cursor = collection.find(obj);
        while(cursor.hasNext()) {
            DBObject next = cursor.next();
            Object dbDocument = next.get("document");
            documents.add(dbDocument);

        }
        return Response.status(Response.Status.OK).entity(documents).build();
    }

    @POST
    @Path("/replay")
    public Response replayAndDeleteDocuments(Replay replayParams) throws IOException {

        BasicDBObject obj = new BasicDBObject();

        if(replayParams.getFromTime() != null){
            obj.put("document.enqueueTimestamp",new BasicDBObject("$gte", replayParams.getFromTime()));
        }

        if(replayParams.getToTime() != null){
            obj.put("document.enqueueTimestamp",new BasicDBObject("$lte", replayParams.getToTime()));
        }

        if(replayParams.getIds() != null && replayParams.getIds().size() > 0){
            obj.put("document.id",new BasicDBObject("$in",replayParams.getIds()));
        }

        DBCursor cursor = collection.find(obj);
        logger.info("Replay documents, Found total : "+cursor.size());
        while(cursor.hasNext()) {
            DBObject next = cursor.next();
            Object dbDocument = next.get("document");
            bigqueue.enqueue(mapper.writeValueAsBytes(dbDocument));
            collection.remove(next);
        }

        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/delete")
    public Response deleteDocuments(Replay replayParams){
        BasicDBObject obj = new BasicDBObject();

        if(replayParams.getFromTime() != null){
            obj.put("document.enqueueTimestamp",new BasicDBObject("$gte", replayParams.getFromTime()));
        }

        if(replayParams.getToTime() != null){
            obj.put("document.enqueueTimestamp",new BasicDBObject("$lte", replayParams.getToTime()));
        }

        if(replayParams.getIds() != null && replayParams.getIds().size() > 0){
            obj.put("document.id",new BasicDBObject("$in",replayParams.getIds()));
        }

        DBCursor cursor = collection.find(obj);
        logger.info("Replay documents, Found total : "+cursor.size());
        while(cursor.hasNext()) {
            collection.remove(cursor.next());
        }
        return Response.status(Response.Status.OK).build();
    }
}
