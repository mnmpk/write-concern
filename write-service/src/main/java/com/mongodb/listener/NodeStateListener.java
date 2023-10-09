package com.mongodb.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.connection.ServerConnectionState;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterListener;

public class NodeStateListener implements ClusterListener {
    Logger logger = LoggerFactory.getLogger(getClass());

    Consumer<Map<ServerConnectionState, Integer>> callback;

    public NodeStateListener(Consumer<Map<ServerConnectionState, Integer>> callback) {
        this.callback = callback;
    }


    @Override
    public synchronized void clusterDescriptionChanged(final ClusterDescriptionChangedEvent event) {
        Map<ServerConnectionState, Integer> nodeState = new HashMap<ServerConnectionState, Integer>();
        event.getNewDescription().getServerDescriptions().stream().forEach(d -> {
            d.getTagSet().forEach(t -> {
                logger.info(t.getName() + ":" + t.getValue());
            });
            if (!nodeState.containsKey(d.getState())) {
                nodeState.put(d.getState(), 1);
            } else {
                nodeState.put(d.getState(), nodeState.get(d.getState()) + 1);
            }
        });
        logger.info(nodeState.toString());
        callback.accept(nodeState);
    }
}
