package com.mongodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.service.MonogClientService;


@Configuration
@EnableRetry
public class AppConfig {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    MonogClientService service;
    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Bean("mongoClient")
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
        //return service.getMongoClient();
    }

}
