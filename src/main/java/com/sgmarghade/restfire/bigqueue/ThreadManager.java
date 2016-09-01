package com.sgmarghade.restfire.bigqueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leansoft.bigqueue.IBigQueue;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.sgmarghade.restfire.config.RestFireConfig;
import com.sgmarghade.restfire.exceptions.BadRequestException;
import com.sgmarghade.restfire.models.Document;
import com.sgmarghade.restfire.models.Header;
import io.dropwizard.lifecycle.Managed;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by swapnil on 10/03/16.
 * This will launch threads to pull data from bigqueue and push to Destination URLs.
 * This will take care of enquing back requests to bigqueue in case something goes wrong (status != 2xx).
 * No of threads are configurable which will parallely push data to Destination Server.
 */

public class ThreadManager implements Managed {
    private static final Logger logger = LoggerFactory.getLogger(ThreadManager.class);
    private static AtomicBoolean isRunning = new AtomicBoolean(true);
    private final RestFireConfig restFireConfig;
    private final IBigQueue bigQueue;
    private final ObjectMapper objectMapper;
    private ScheduledExecutorService executorService;
    private PoolingHttpClientConnectionManager connectionManager;
    private DBCollection collection;

    public ThreadManager(RestFireConfig restFireConfig, IBigQueue bigQueue, ObjectMapper objectMapper, DBCollection collection) {
        this.restFireConfig = restFireConfig;
        this.bigQueue = bigQueue;
        this.objectMapper = objectMapper;
        this.collection = collection;
    }

    /**
     * @throws Exception
     */

    @Override
    public void start() throws Exception {
        logger.info("Starting Thread manager");
        executorService = Executors.newScheduledThreadPool(restFireConfig.getWorkerThreads() + 1);
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(10);
        executorService.scheduleWithFixedDelay(new QueueCleaner(bigQueue), 2, 60, TimeUnit.SECONDS);

        for (int i = 0; i < restFireConfig.getWorkerThreads(); i++) {
            executorService.scheduleWithFixedDelay(new MessageSender(bigQueue, restFireConfig, objectMapper, connectionManager, collection), 2, restFireConfig.getThreadSleepTimeSec(), TimeUnit.SECONDS);
        }

        logger.info("Successfuly started thread manager with config " + restFireConfig);
    }

    /**
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        logger.info("Stopping server..... !");
        isRunning.set(false);
        Thread.sleep(3000);
        executorService.shutdown();
        connectionManager.shutdown();
    }

    /**
     * Message sender is the thread which will be launched to push data to final URL as unit task.
     */

    private static final class MessageSender implements Runnable {

        private final IBigQueue bigQueue;
        private final RestFireConfig restFireConfig;
        private final ObjectMapper objectMapper;
        private final PoolingHttpClientConnectionManager connectionManager;
        private final DBCollection collection;

        /**
         * @param bigQueue
         * @param restFireConfig
         * @param objectMapper
         * @param connectionManager
         * @param collection
         */
        private MessageSender(IBigQueue bigQueue, RestFireConfig restFireConfig, ObjectMapper objectMapper, PoolingHttpClientConnectionManager connectionManager, DBCollection collection) {
            this.bigQueue = bigQueue;
            this.restFireConfig = restFireConfig;
            this.objectMapper = objectMapper;
            this.connectionManager = connectionManager;
            this.collection = collection;
        }


        @Override
        public void run() {

            logger.info("Queue : size is {}, is runnin status is {} ", bigQueue.size(), isRunning.get());
            while (bigQueue.size() > 0 && isRunning.get()) {
                logger.info("Queue : size when queue is not empty {} ", bigQueue.size());
                try {

                    byte[] data = bigQueue.dequeue();

                    if (data != null && data.length > 0) {
                        Document document = objectMapper.readValue(data, Document.class);
                        try {
                            sendMessages(document);
                        } catch (BadRequestException e) {
                            logger.error(e.getMessage(), e);
                            checkFailedMessage(document, e.getMessage(),true);
                        } catch (Exception e){
                            logger.error(e.getMessage(), e);
                            checkFailedMessage(document, e.getMessage(),false);
                        }
                        if (bigQueue.size() == 1) {
                            break;  //It's same element which was enqueued.
                        }
                    } else {
                        logger.info("Null data retrieved from queue ... Skipping record. This is due to some other thread " +
                                "has read data. It's safe...!");
                        break;
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        /**
         * @param document
         * @param message
         */
        protected void checkFailedMessage(Document document, String message,Boolean is4xx) throws JsonProcessingException {

            document.setTotalRetry(document.getTotalRetry() + 1);
            document.setLastRetryTimestamp(new Date());

            Integer minutes = (int)(new Date().getTime() - document.getEnqueueTimestamp().getTime()) / 60000;
            logger.info("Queue : Enqueuing back messages back to queue timeElapsed in Minutes is {}, for document {} ", minutes, document.toString());

            if (minutes > new Integer(restFireConfig.getDropMessagePostMinutes()) || is4xx) {
                logger.info("Queue : Dropping message due to elapsed time {} ", document.toString());
                pushDocumentToMongo(document, message);
            } else {
                enqueueMessage(document);
            }
        }

        private void pushDocumentToMongo(Document document, String message) throws JsonProcessingException {
            document.setResponseMessage(message);
            BasicDBObject dbDocument = new BasicDBObject();
            dbDocument.put("document", org.bson.Document.parse(objectMapper.writeValueAsString(document)));
            collection.insert(dbDocument);
            logger.info("Writing document to MONGO with id : " + dbDocument.get("_id"));
        }

        /**
         * @param document
         */

        protected void enqueueMessage(Document document) {
            try {
                bigQueue.enqueue(objectMapper.writeValueAsBytes(document));
            } catch (IOException e) {
                logger.error("Queue : Dropping message ", document);
                logger.error("Queue : Cant help in this case,  silently dropping message", e.getMessage(), e);
            }
        }

        /**
         * @param document
         * @throws Exception
         */
        private void sendMessages(Document document) throws BadRequestException, IOException {
            logger.info("Http : Sending message {} ", document.toString());
            String url = document.getUrl();
            if (!url.startsWith("http")) {
                url = "http://" + url;
            }

            JsonNode body = document.getBody();
            if (document.getMethodType() == null) {
                document.setMethodType(Document.MethodType.POST);
            }

            if (document.getHeaders() == null) {
                document.setHeaders(new ArrayList<>());
            }

            if (document.getHeaders().size() == 0) {
                document.getHeaders().add(Header.builder().key("Content-Type").value("application/json").build());
            }

            switch (document.getMethodType()) {
                case POST:
                    HttpPost post = new HttpPost(url);
                    if (document.getBody() != null) {
                        post.setEntity(new StringEntity(objectMapper.writeValueAsString(body)));
                    }
                    handleRequest(document, post);
                    break;
                case PUT:
                    HttpPut put = new HttpPut(url);
                    if (document.getBody() != null) {
                        put.setEntity(new StringEntity(objectMapper.writeValueAsString(body)));
                    }
                    handleRequest(document, put);
                    break;
            }
        }

        /**
         * Method is protected, To stub method in Test cases.
         *
         * @param document
         * @param request
         * @throws Exception
         */
        protected void handleRequest(Document document, HttpUriRequest request) throws BadRequestException, IOException {
            if (document.getHeaders() != null && document.getHeaders().size() > 0) {
                for (Header header : document.getHeaders()) {
                    request.addHeader(header.getKey(), header.getValue());
                }
            }
            HttpResponse response = null;

            try {
                HttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
                long requestStartTime = System.currentTimeMillis();
                response = httpClient.execute(request);
                logger.info("Http : Time taken to execute request in ms {},for methodType {},  for url {}, response is {}", System.currentTimeMillis() - requestStartTime, document.getMethodType(), document.getUrl(), response.getStatusLine().getStatusCode());


                if (!(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299)) {
                    document.setStatusCode(response.getStatusLine().getStatusCode());
                    if(response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() <= 499){
                        throw new BadRequestException("Http :  Something went wrong, status code is " + response.getStatusLine().getStatusCode() + " With response : " + EntityUtils.toString(response.getEntity()));
                    }else {
                        throw new RuntimeException("Http :  Something went wrong, status code is " + response.getStatusLine().getStatusCode() + " With response : " + EntityUtils.toString(response.getEntity()));
                    }
                }

            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
                throw e;
            } finally {
                if (response != null) {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        }
    }


    protected void enableIsRunning() {
        isRunning.set(true);
    }

    /**
     * Bigqueue cleaner.
     */
    private static final class QueueCleaner implements Runnable {
        private IBigQueue messageQueue;

        /**
         * @param messageQueue
         */

        private QueueCleaner(IBigQueue messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                this.messageQueue.gc();
                logger.info("Ran_gc_on_queue_message took={}", System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                logger.error("gc_failed_on_message_queue", e);
            }
        }
    }
}
