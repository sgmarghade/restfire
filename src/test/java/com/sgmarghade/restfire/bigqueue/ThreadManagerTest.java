package com.sgmarghade.restfire.bigqueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.sgmarghade.restfire.Helper.TestHelper;
import com.sgmarghade.restfire.config.RestFireConfig;
import com.sgmarghade.restfire.models.Document;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by swapnil on 11/03/16.
 */
public class ThreadManagerTest {

    private DummyEventHandler eventHandler;
    private TestHostPort testHostPort = new TestHostPort();
    private IBigQueue bigQueue;
    private Logger logger = LoggerFactory.getLogger(ThreadManagerTest.class);
    private String API_PATH = "/v1/document/destination";
    private ObjectMapper mapper = new ObjectMapper();
    private DBCollection collection;
    @Rule
    public LocalServerTestRule testRule() throws IOException {
        eventHandler =  Mockito.spy(new DummyEventHandler());
        bigQueue = new BigQueueImpl(System.getProperty("java.io.tmpdir"), "" + System.currentTimeMillis() + "-" + Math.random());
        collection = Mockito.mock(DBCollection.class);
        return new LocalServerTestRule(testHostPort,
                ImmutableMap.of(API_PATH, eventHandler));
    }

    @Test
    public void serverShouldGetTotal20Requests() throws Exception {
        RestFireConfig config = RestFireConfig.builder().threadSleepTimeSec(2).workerThreads(2).dropMessagePostMinutes(2).build();

        ThreadManager manager = new ThreadManager(config, bigQueue, mapper, collection);
        manager.enableIsRunning();
        manager.start();

        logger.info("" + testHostPort);

        for (int i = 0; i < 20; i++) {
            bigQueue.enqueue(mapper.writeValueAsBytes(TestHelper.getDocument(testHostPort.getHostName(),testHostPort.getPort(),API_PATH,mapper)));

        }

        Thread.sleep(5000);
        manager.stop();

        Assert.assertEquals(20, eventHandler.getCounter().get());
        Assert.assertEquals(0,bigQueue.size());

    }

    @Test
    public void serverShouldHaveBeenHitMultipleTimesForError() throws Exception {
        Mockito.doThrow(HttpException.class).when(eventHandler).handle(Mockito.any(HttpRequest.class),Mockito.any(HttpResponse.class),Mockito.any(HttpContext.class));

        RestFireConfig config = RestFireConfig.builder().threadSleepTimeSec(2).workerThreads(2).dropMessagePostMinutes(2).build();

        ThreadManager manager = new ThreadManager(config, bigQueue, mapper, collection);
        manager.enableIsRunning();
        manager.start();

        logger.info("" + testHostPort);

        for (int i = 0; i < 20; i++) {
            bigQueue.enqueue(mapper.writeValueAsBytes(TestHelper.getDocument(testHostPort.getHostName(),testHostPort.getPort(),API_PATH,mapper)));
        }
        Thread.sleep(5000);
        manager.stop();
        Assert.assertEquals(20, bigQueue.size());
    }

    @Test
    public void documentShouldHaveFailedCountIncreased() throws Exception{
        Mockito.doThrow(HttpException.class).when(eventHandler).handle(Mockito.any(HttpRequest.class),Mockito.any(HttpResponse.class),Mockito.any(HttpContext.class));

        RestFireConfig config = RestFireConfig.builder().threadSleepTimeSec(2).workerThreads(1).dropMessagePostMinutes(2).build();

        ThreadManager manager = new ThreadManager(config, bigQueue, mapper,collection);
        manager.enableIsRunning();
        manager.start();

        logger.info("" + testHostPort);
        for (int i = 0; i < 1; i++) {
            bigQueue.enqueue(mapper.writeValueAsBytes(TestHelper.getDocument(testHostPort.getHostName(),testHostPort.getPort(),API_PATH,mapper)));
        }
        Thread.sleep(5000);
        manager.stop();

        Document document = mapper.readValue(bigQueue.dequeue(),Document.class);
        logger.info(document.toString());
        Assert.assertNotEquals(new Integer(0), document.getTotalRetry());
    }


    @Test
    public void shouldStoreDocumentInMongoForElapsedTime() throws Exception{
        Mockito.doThrow(HttpException.class).when(eventHandler).handle(Mockito.any(HttpRequest.class),Mockito.any(HttpResponse.class),Mockito.any(HttpContext.class));
        Mockito.doReturn(null).when(collection).insert(Mockito.any(BasicDBObject.class));

        RestFireConfig config = RestFireConfig.builder().threadSleepTimeSec(2).workerThreads(1).dropMessagePostMinutes(2).build();
        ThreadManager manager = new ThreadManager(config, bigQueue, mapper,collection);
        manager.enableIsRunning();
        manager.start();

        logger.info("" + testHostPort);
        for (int i = 0; i < 1; i++) {
            Calendar cal = Calendar.getInstance(); // creates calendar
            cal.setTime(new Date()); // sets calendar time/date

            cal.add(Calendar.HOUR_OF_DAY, -1); // adds one hour
            System.out.println("Calender time : "+cal.getTime());
            Document document = TestHelper.getDocument(testHostPort.getHostName(), testHostPort.getPort(), API_PATH, mapper);
            document.setEnqueueTimestamp(cal.getTime());

            bigQueue.enqueue(mapper.writeValueAsBytes(document));
        }
        Thread.sleep(5000);
        manager.stop();
        Mockito.verify(collection,Mockito.times(1)).insert(Mockito.any(BasicDBObject.class));
        Assert.assertEquals(0, bigQueue.size());
    }
}
