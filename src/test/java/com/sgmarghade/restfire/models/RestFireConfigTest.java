package com.sgmarghade.restfire.models;


import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Created by swapnil on 11/03/16.
 */
public class RestFireConfigTest {

    @Test
    public void modelShouldNotHaveNullEnqueueAndLastRetryValues(){
        Document document = new Document();
        Assert.assertNotNull(document.getEnqueueTimestamp());
        Assert.assertNotNull(document.getLastRetryTimestamp());
    }

    @Test
    public void modelShouldHaveCurrentDateAsDefaultValueForRetryAndEnqueueTimestamp(){
        Document document = new Document();
        Period period = new Period(document.getEnqueueTimestamp().getTime(),new Date().getTime());
        Assert.assertEquals(period.getMinutes(), 0);

        period = new Period(document.getLastRetryTimestamp().getTime(),new Date().getTime());
        Assert.assertEquals(period.getMinutes(), 0);
    }

}
