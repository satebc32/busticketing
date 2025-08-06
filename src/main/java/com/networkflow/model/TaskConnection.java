package com.networkflow.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a connection between two workflow tasks
 */
public class TaskConnection {
    private String sourceTaskId;
    private String targetTaskId;
    private String condition;
    private ConnectionType type;

    public enum ConnectionType {
        NORMAL,      // Standard flow connection
        SUCCESS,     // Execute on success
        FAILURE,     // Execute on failure
        CONDITIONAL  // Execute based on condition
    }

    @JsonCreator
    public TaskConnection(@JsonProperty("sourceTaskId") String sourceTaskId,
                         @JsonProperty("targetTaskId") String targetTaskId) {
        this.sourceTaskId = sourceTaskId;
        this.targetTaskId = targetTaskId;
        this.type = ConnectionType.NORMAL;
    }

    public TaskConnection(String sourceTaskId, String targetTaskId, ConnectionType type) {
        this.sourceTaskId = sourceTaskId;
        this.targetTaskId = targetTaskId;
        this.type = type;
    }

    // Getters and Setters
    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public String getTargetTaskId() {
        return targetTaskId;
    }

    public void setTargetTaskId(String targetTaskId) {
        this.targetTaskId = targetTaskId;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public ConnectionType getType() {
        return type;
    }

    public void setType(ConnectionType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TaskConnection{" +
                "sourceTaskId='" + sourceTaskId + '\'' +
                ", targetTaskId='" + targetTaskId + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskConnection that = (TaskConnection) o;

        if (!sourceTaskId.equals(that.sourceTaskId)) return false;
        return targetTaskId.equals(that.targetTaskId);
    }

    @Override
    public int hashCode() {
        int result = sourceTaskId.hashCode();
        result = 31 * result + targetTaskId.hashCode();
        return result;
    }
}