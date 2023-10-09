package com.mongodb.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ServerConnectionState;
import com.mongodb.listener.NodeStateListener;

@Service
public class MonogClientService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${spring.data.mongodb.uri}")
    private String uri;

    private MongoClient mongoClient;
    private MongoClient mongoClientMajority;
    private MongoClient mongoClientW4;

    @Autowired
    ApplicationContext applicationContext;

    public MongoClient getMongoClient() {
        if (this.mongoClient == null) {
            this.mongoClient = getMongoClientMajority();
        }
        return this.mongoClient;
    }

    @Bean
    public NodeStateListener nodeStateListener() {
        return new NodeStateListener((Map<ServerConnectionState, Integer> m) -> {
            if (this.mongoClient != null) {
                if (m.get(ServerConnectionState.CONNECTED) > 3) {
                    if (this.mongoClient == getMongoClientMajority()) {
                        logger.info("Active node > 3 Setting default MongoClient to W4");
                        this.mongoClient = getMongoClientW4();
                //DefaultSingletonBeanRegistry beanRegistry = (DefaultSingletonBeanRegistry) applicationContext.getAutowireCapableBeanFactory();
                //beanRegistry.destroySingleton("mongoClient");
                //beanRegistry.registerSingleton("mongoClient", getMongoClientW4());
                    }
                } else {
                    if (this.mongoClient == getMongoClientW4()) {
                        logger.info("Active node <= 3 Setting default MongoClient to Majority");
                        this.mongoClient = getMongoClientMajority();
                //DefaultSingletonBeanRegistry beanRegistry = (DefaultSingletonBeanRegistry) applicationContext.getAutowireCapableBeanFactory();
                //beanRegistry.destroySingleton("mongoClient");
                //beanRegistry.registerSingleton("mongoClient", getMongoClientMajority());
                    }
                }
                //BeanDefinitionRegistry beanRegistry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
                //beanRegistry.removeBeanDefinition("mongoClient");
                //((SingletonBeanRegistry) applicationContext.getAutowireCapableBeanFactory()).registerSingleton("mongoClient", this.mongoClient); 
            }
        });
    }

    public MongoClient getMongoClientMajority() {
        if (mongoClientMajority == null) {
            mongoClientMajority = MongoClients.create(
                    MongoClientSettings.builder()
                            .writeConcern(WriteConcern.MAJORITY)
                            .applyToClusterSettings(builder
                                    -> builder.addClusterListener(nodeStateListener()))
                            .applyConnectionString(new ConnectionString(uri))
                            .applyToConnectionPoolSettings(builder
                                    -> builder.maxWaitTime(10, TimeUnit.SECONDS)
                                    .maxSize(200)).build()
            );
        }
        return mongoClientMajority;
    }

    public MongoClient getMongoClientW4() {
        if (mongoClientW4 == null) {
            mongoClientW4 = MongoClients.create(
                    MongoClientSettings.builder()
                            .writeConcern(new WriteConcern(4, 1000))
                            .applyToClusterSettings(builder
                                    -> builder.addClusterListener(nodeStateListener()))
                            .applyConnectionString(new ConnectionString(uri))
                            .applyToConnectionPoolSettings(builder
                                    -> builder.maxWaitTime(10, TimeUnit.SECONDS)
                                    .maxSize(200)).build()
            );
        }
        return mongoClientW4;
    }
}
