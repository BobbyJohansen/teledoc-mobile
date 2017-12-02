package com.teledoc.teledocmobile.communication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeleDocMessage {

    private UUID uuid = UUID.randomUUID();
    private long timestamp = System.currentTimeMillis();
    private Map<String,String> metadata;
    private List<Double> data;

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public List<Double> getData() {
        return data;
    }

    public void setData(List<Double> data) {
        this.data = data;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
