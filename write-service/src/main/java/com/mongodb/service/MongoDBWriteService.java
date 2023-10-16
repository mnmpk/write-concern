package com.mongodb.service;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoWriteConcernException;

import lombok.Getter;
import lombok.Setter;

@Service
public class MongoDBWriteService {
    @Setter
    Logger logger = LoggerFactory.getLogger(getClass());

    @Getter
    boolean alertOpen;

    //@Autowired
    //MongoDBMonitoringService mongoDBMonitoringService;

    //Default case
    public void performWrite(Runnable runnable) {
        try {
            runnable.run();
            if(alertOpen)
                logger.info("Resume normal");
            alertOpen=false;
        } catch (MongoWriteConcernException ex) {
            //Notes to integrate MongoDBMonitoringService 
            // Option 1: Integrate application with MongoDBMonitoringService to provide comperhensive trigger/resume of monitoring (require read access to 'admin' database)
            //if(!mongoDBMonitoringService.isAlertOpen()){
            //    mongoDBMonitoringService.triggerAlert();
            //}
            // [choosen] Option 2: Run MongoDBMonitoringService separately to monitor the cluster replication status
            if(!alertOpen)
                logger.warn("MongoWriteConcernException", ex);
            alertOpen=true;
        }
    }

    //Pass app logger to MongoDBMonitoringService
    //public void performWriteWithLogger(Runnable runnable, Logger logger) {
    //    try {
    //        runnable.run();
    //    } catch (MongoWriteConcernException ex) {
    //        if(!mongoDBMonitoringService.isAlertOpen()){
    //            mongoDBMonitoringService.setLogger(logger);
    //            mongoDBMonitoringService.triggerAlert();
    //        }
    //    }
    //}

    //Handle MongoWriteConcernException yourself
    public void performWriteWithCallback(Runnable runnable, Consumer<Exception> consumer) {
        try {
            runnable.run();
            if(alertOpen)
                logger.info("Resume normal");
            alertOpen=false;
        } catch (MongoWriteConcernException ex) {
            //if(!mongoDBMonitoringService.isAlertOpen()){
            //    mongoDBMonitoringService.triggerAlert();
            //}
            consumer.accept(ex);
            alertOpen=true;
        }
    }

    //With automatic retry
    //@Retryable(retryFor = MongoTimeoutException.class, maxAttemptsExpression = "${app.maxAttempts}", backoff = @Backoff(delayExpression = "${app.delay}"))
    @Retryable(include = MongoTimeoutException.class, maxAttemptsExpression = "${app.maxAttempts}", backoff = @Backoff(delayExpression = "${app.delay}"))
    public void performWriteWithRetry(Runnable runnable) {
        try {
            runnable.run();
        } catch (MongoWriteConcernException ex) {
            //if(!mongoDBMonitoringService.isAlertOpen()){
            //    mongoDBMonitoringService.triggerAlert();
            //}
        }
    }

    @Recover
    private void abort() {
        logger.error("Aborted. Write service unavailable.");
    }
}