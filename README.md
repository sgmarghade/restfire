RestFire is service which reliably makes async rest calls to given end point. 

#### ***Following are some of the use cases for RestFire.*** ####

> * Sending Sms Reliably.
> * Sending Emails
> * Making POST call to end service
> * Event flow across services. 


#### ***Architecture*** ####

##### ***Tech*** #####
> 1. Dropwizard
> 2. Mongo
> 3. [BigQueue] (https://github.com/bulldog2011/bigqueue)
 
##### ***WorkFlow*** #####

> 1. Rest end point accepts request with body and method POST,GET or PUT
> 2. It persist request in bigqueu.  
> 3. Configurable number of threads pickup task and make rest call to end point.
> 4. On 4XX it sidelines it to Mongo immediately. 
> 5. On 5XX it persist it back to Bigqueue and retries for given amount of time.

#### ***Running with Docker*** ####
> 1. Build image : ```docker build -t restfire .```
> 2. Running docker image : ```docker run -e "JAVA_PROCESS_MIN_HEAP=2g" -e "JAVA_PROCESS_MAX_HEAP=2g" -e "BIGQUEUE_PATH=/data" -e "DROP_MESSAGE_POST_MINUTES=5" -e "MONGO_HOST=flo-mongo-0.opinio.5673.mongodbdns.com" -e "MONGO_PORT=27000" -p 18000:18000 -v /data:/data <image-id>```

#### ***API*** ####

##### ***POST Single Document*** #####

***1. POST Single Document*** : POST  /v1/documents
***Header*** : Content-Type:application/json
***JsonFields*** :
>1. url : End point URL
>2. methodType : POST/PUT/GET
>3. headers : Headers for end point.
>4. body : Body to payload for end point.

***Example Payload*** :
 
```json
{	"url": "http://localhost:1221/api/orders",
	"methodType": "POST",
	"headers": [{
		"key": "Content-Type",
		"value": "application/json"
	}, {
		"key": "Authorization",
		"value": "<TOKEN>"
	}],
	"body": {
		"customerName": "Swapnil Marghade",
		"orderId": "1234",
		"details": {
			"name": "sourceName 1",
			"phone": "1222121"
		}
	}
}
```

***2. POST multiple documents*** : POST /v1/documents/bulk

***Header*** : Content-Type:application/json

***Example Payload*** :
 
```json
[
    {	"url": "http://localhost:1221/api/orders",
        "methodType": "POST",
        "headers": [{
            "key": "Content-Type",
            "value": "application/json"
        }, {
            "key": "Authorization",
            "value": "<TOKEN>"
        }],
        "body": {
                		"customerName": "Swapnil Marghade",
                		"orderId": "1234",
                		"details": {
                			"name": "sourceName 1",
                			"phone": "1222121"
                		}
        }
     }
]
```

***3. GET Sidelined documents*** GET /v1/documents
***Query Params***
>1. fromTime : Enqueue from time (in Millis)
>2. toTime : Enqueue to time (in Millis)
>3. id : Id of document 
>4. methodType : POST/PUT/GET

***Response***
```json
[{
	"id": "611e5fc1-6bc7-4121-9d88-b89441b5a55b",
	"enqueueTimestamp": 1467799512052,
	"lastRetryTimestamp": 1468412992617,
	"totalRetry": 60,
	"url": "http://localhost:3000/v1/orders/23",
	"methodType": "PUT",
	"headers": [{
		"key": "Content-Type",
		"value": "application/json"
	}],
	"body": {
		"status": "created"
	},
	"statusCode": 400,
	"responseMessage": "Http :  Something went wrong, status code is 400 With response : {\"statusCode\":400,\"error\":\"Bad Request\",\"message\":\"Cannot read property 'customerId' of null\",\"details\":null}"
}]
```

***4. POST Replay sidelined document*** POST /v1/documents/replay

***Header*** : Content-Type:application/json

***JsonFields*** :
>1. fromTime : Enqueue from time (in Millis)
>2. toTime : Enqueue to time (in Millis)
>3. ids : List of sidelined document ids.
 
***Example Payload*** :
```json
{
	"fromTime": 1469092108822,
	"toTime": 1469092208822,
	"ids": ["611e5fc1-6bc7-4121-9d88-b89441b5a55b", "611e5fc1-6bc7-4121-9d88-b89441b512b"]
}
```

***5. POST Delete sidelined document*** POST /v1/documents/delete

***Header*** : Content-Type:application/json

***JsonFields*** :
>1. fromTime : Enqueue from time (in Millis)
>2. toTime : Enqueue to time (in Millis)
>3. ids : List of sidelined document ids.
 
***Example Payload*** :
```json
{
	"fromTime": 1469092108822,
	"toTime": 1469092208822,
	"ids": ["611e5fc1-6bc7-4121-9d88-b89441b5a55b", "611e5fc1-6bc7-4121-9d88-b89441b512b"]
}
```