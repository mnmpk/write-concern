package com.mongodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;


@Configuration
public class AppConfig {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Bean("mongoClient")
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
    }

}
