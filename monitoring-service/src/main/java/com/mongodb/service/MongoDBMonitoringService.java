package com.mongodb.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BsonTimestamp;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoClient;

import lombok.Getter;
import lombok.Setter;

@Service
public class MongoDBMonitoringService {
    @Setter
    Logger logger = LoggerFactory.getLogger(getClass());

    @Getter
    private boolean alertOpen;

    private int primaryOpTime;

    @Value("${app.replicationLagThershold}")
    private int replicationLagThershold;

    @Value("${app.requiredNoOfHealthyNode}")
    private int requiredNoOfHealthyNode;

    @Value("${app.independentMonitoringMode}")
    private boolean independentMonitoringMode;

    @Autowired
    private MongoClient mongoClient;

    // @Autowired
    // private MongoTemplate mongoTemplate;

    private enum STATE {
        HEALTHY,
        UNHEALTHY
    }

    @Scheduled(fixedRateString = "${app.interval}")
    public void writeServiceHealthinessProbe() {
        if(independentMonitoringMode){
            Map<STATE, Integer> map = getReplicaSetStatus();
            if (alertOpen && map.get(STATE.HEALTHY) >= requiredNoOfHealthyNode) {
                closeAlert();
            } else if (!alertOpen && map.get(STATE.HEALTHY) < requiredNoOfHealthyNode) {
                triggerAlert();
            }
        }else{
            if(alertOpen){
                Map<STATE, Integer> map = getReplicaSetStatus();
                if (map.get(STATE.HEALTHY) >= requiredNoOfHealthyNode) {
                    closeAlert();
                }
            }
        }
    }

    private Map<STATE, Integer> getReplicaSetStatus() {
        try {
            Document doc = mongoClient.getDatabase("admin").runCommand(new Document("replSetGetStatus", 1));
            // Document doc = mongoTemplate.getDb().runCommand(new Document("replSetGetStatus", 1));
            List<Document> documents = doc.getList("members", Document.class);
            primaryOpTime = documents.stream().filter(m -> m.getInteger("state") == 1)
                    .map(m -> m.get("optime", Document.class).get("ts", BsonTimestamp.class)).findFirst().get()
                    .getTime();
            Map<STATE, Integer> map = new HashMap<>();
            for (Document member : documents) {
                int ts = member.get("optime", Document.class).get("ts", BsonTimestamp.class).getTime();
                int lagTime = ts - primaryOpTime;
                if (ts == 0) {
                    logger.debug(member.getString("name") + " is not reachable");
                } else {
                    logger.debug(member.getString("name")
                            + (member.getInteger("state") == 1 ? "(primary)" : (" replication lag:" + lagTime)));
                }
                if (Math.abs(lagTime) > replicationLagThershold)
                    map.put(STATE.UNHEALTHY, map.get(STATE.UNHEALTHY) != null ? (map.get(STATE.UNHEALTHY) + 1) : 1);
                else
                    map.put(STATE.HEALTHY, map.get(STATE.HEALTHY) != null ? (map.get(STATE.HEALTHY) + 1) : 1);
            }
            return map;
        } catch (MongoTimeoutException ex) {
            logger.error("Write service unavailable.");
        }
        return Map.of(STATE.UNHEALTHY, 0, STATE.HEALTHY, 0);
    }

    public void triggerAlert() {
        logger.warn("Cluster is not healthy/Replication lag exceeds thershold");
        alertOpen = true;
    }
    public void closeAlert() {
        logger.info("Resume normal");
        alertOpen = false;
    }
}