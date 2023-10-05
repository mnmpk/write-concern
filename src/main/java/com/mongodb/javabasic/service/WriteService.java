package com.mongodb.javabasic.service;

import org.bson.BsonTimestamp;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.client.MongoClient;

@Service
public class WriteService {
    Logger logger = LoggerFactory.getLogger(getClass());
    private boolean replicationError = false;
    private int primaryOpTime;
    @Value("${app.replicationLagThershold}")
    private int replicationLagThershold;

    @Autowired
    private MongoClient mongoClient;

    @Retryable(retryFor = MongoTimeoutException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void performWrite(Runnable runnable) {
        StopWatch sw = new StopWatch();
        logger.info("Start performing write");
        sw.start();
        try {
            runnable.run();
        } catch (MongoWriteConcernException ex) {
            startMonitoring();
        }
        sw.stop();
        logger.info("End, Time: " + sw.getTotalTimeMillis() + "ms");
    }

    @Recover
    private void abort() {
        logger.error("Aborted. Write serice unavailable.");
    }

    private void startMonitoring() {
        logger.warn("waiting for replication timed out while writing");
        replicationError = true;
    }

    @Scheduled(fixedRateString ="${app.interval}")
    public void writeServiceHealthinessProbe() {
        try {
            if (replicationError) {
                Document doc = mongoClient.getDatabase("admin").runCommand(new Document("replSetGetStatus", 1));
                boolean isHealthy = true;
                for (Document member : doc.getList("members", Document.class)) {
                    int ts = member.get("optime", Document.class).get("ts", BsonTimestamp.class).getTime();
                    if (member.getInteger("state") == 1)
                        primaryOpTime = ts;
                    int lagTime = ts - primaryOpTime;
                    if (ts == 0)
                        logger.warn(member.getString("name") + " is not reachable");
                    else
                        logger.warn(member.getString("name")
                                + (member.getInteger("state") == 1 ? "(primary)" : (" replication lag:" + lagTime)));
                    if (Math.abs(lagTime) > replicationLagThershold)
                        isHealthy = false;
                }
                if (isHealthy) {
                    logger.info("Resume normal");
                    replicationError = false;
                }
            }
        } catch (MongoTimeoutException ex) {
            logger.error("Write serice unavailable.");
        }
    }
}