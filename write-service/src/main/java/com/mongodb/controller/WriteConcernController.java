package com.mongodb.controller;

import java.util.Date;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.service.MongoDBWriteService;

@RestController
public class WriteConcernController {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private MongoDBWriteService writeService;

    @GetMapping("/write-without-callback")
    public void write() throws InterruptedException {
        try {
            this.writeService.performWrite(() -> {
                MongoCollection<Document> collection = mongoClient.getDatabase("demo").getCollection("test");
                Document doc = new Document().append("t", new Date());
                logger.info(collection.insertOne(doc).toString());
            });
        } catch (MongoTimeoutException ex) {
            logger.error("Write service unavailable.", ex);
        }
    }
    @GetMapping("/write")
    public void writeWithCallback() throws InterruptedException {
        try {
            Document doc = new Document().append("t", new Date());
            this.writeService.performWriteWithCallback(() -> {
                MongoCollection<Document> collection = mongoClient.getDatabase("demo").getCollection("test");
                
                logger.info(collection.insertOne(doc).toString());
            },(ex) -> {
                if(!this.writeService.isAlertOpen())
                    logger.warn("write concern timeout exception for:"+doc);
            });
        } catch (MongoTimeoutException ex) {
            //Server Selection Timeout, default value is 30s. The value can be change by adding urioption or changing client definition.
            //https://www.mongodb.com/docs/manual/reference/connection-string/#mongodb-urioption-urioption.serverSelectionTimeoutMS
            //e.g. serverSelectionTimeoutMS=10000
            //https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/connection/mongoclientsettings/#cluster-settings
            //e.g. MongoClientSettings.builder().applyToClusterSettings(builder
            //            -> builder.applySettings(ClusterSettings.builder().serverSelectionTimeout(10, TimeUnit.SECONDS).build())
            //        ).build();
            logger.error("Write service unavailable.", ex);
        }
    }
    @GetMapping("/write-with-retry")
    public void writeWithRetry() throws InterruptedException {
        this.writeService.performWriteWithRetry(() -> {
            MongoCollection<Document> collection = mongoClient.getDatabase("demo").getCollection("test");
            Document doc = new Document().append("t", new Date());
            logger.info(collection.insertOne(doc).toString());
        });
    }
}
