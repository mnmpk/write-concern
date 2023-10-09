package com.mongodb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.MongoWriteConcernException;

import lombok.Setter;

@Service
public class MongoDBWriteService {
    @Setter
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoDBMonitoringService mongoDBMonitoringService;

    //@Retryable(retryFor = MongoTimeoutException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void performWrite(Runnable runnable) {
        //StopWatch sw = new StopWatch();
        //logger.info("Start performing write");
        //sw.start();
        try {
            runnable.run();
        } catch (MongoWriteConcernException ex) {
            //Notes to integrate MongoDBMonitoringService 
            // Option 1: Integrate application with MongoDBMonitoringService to provide comperhensive trigger/resume of monitoring (require read access to 'admin' database)
            // Option 2: Run MongoDBMonitoringService separately to monitor the cluster replication status
            if(!mongoDBMonitoringService.isAlertOpen())
                mongoDBMonitoringService.triggerAlert();
        }
        //sw.stop();
        //logger.info("End, Time: " + sw.getTotalTimeMillis() + "ms");
    }

    //@Recover
    //private void abort() {
    //    logger.error("Aborted. Write serice unavailable.");
    //}
}