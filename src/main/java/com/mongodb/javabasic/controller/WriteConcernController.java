package com.mongodb.javabasic.controller;

import java.util.Date;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.javabasic.service.WriteService;

@RestController
public class WriteConcernController {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private WriteService writeService;

    //@Autowired
    //MonogClientService service;

    //MongoDatabase db = mongoClient.getDatabase("demo").withWriteConcern(WriteConcern.MAJORITY).withReadConcern(ReadConcern.LOCAL).withReadPreference(ReadPreference.primary());
    //MongoCollection<Document> collection = db.getCollection("test").withWriteConcern(WriteConcern.MAJORITY).withReadConcern(ReadConcern.LOCAL).withReadPreference(ReadPreference.primary());
    @GetMapping("/write")
    public void writeConcern() throws InterruptedException {
        MongoCollection<Document> collection = mongoClient.getDatabase("demo").getCollection("test");
        //this.writeService.write(collection.withWriteConcern(new WriteConcern(0)));
        //this.writeService.write(collection.withWriteConcern(WriteConcern.MAJORITY));
        //this.writeService.write(collection.withWriteConcern(WriteConcern.W3));
        //this.writeService.write(collection.withWriteConcern(new WriteConcern(4, wtimeout)));
        //this.writeService.write(collection.withWriteConcern(new WriteConcern(5, 5000)));
        this.writeService.performWrite(()->{
            Document doc = new Document().append("t", new Date());
            logger.info(collection.insertOne(doc).toString());
        });
    }
@GetMapping("/get-w")
    public void getWriteConcern(){
        MongoCollection<Document> collection = mongoClient.getDatabase("demo").getCollection("test");
logger.info(collection.getWriteConcern().toString());
    }
}
