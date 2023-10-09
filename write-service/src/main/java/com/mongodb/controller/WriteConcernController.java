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

    @GetMapping("/write")
    public void writeConcern() throws InterruptedException {
        MongoCollection<Document> collection = mongoClient.getDatabase("demo").getCollection("test");
        this.writeService.setLogger(logger);
        try {

            this.writeService.performWrite(() -> {
                Document doc = new Document().append("t", new Date());
                logger.info(collection.insertOne(doc).toString());
            });
        } catch (MongoTimeoutException ex) {
            //Server Selection Timeout, default value is 30s. The value can be change by adding urioption or changing client definition.
            //https://www.mongodb.com/docs/manual/reference/connection-string/#mongodb-urioption-urioption.serverSelectionTimeoutMS
            //e.g. serverSelectionTimeoutMS=10000
            //https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/connection/mongoclientsettings/#cluster-settings
            //e.g. MongoClientSettings.builder().applyToClusterSettings(builder
            //            -> builder.applySettings(ClusterSettings.builder().serverSelectionTimeout(10, TimeUnit.SECONDS).build())
            //        ).build();
            logger.error("Write serice unavailable.", ex);
        }
    }
}
