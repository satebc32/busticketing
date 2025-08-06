package com.networkflow.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Represents a single task in the workflow that can be executed
 */
public class WorkflowTask {
    private String id;
    private String name;
    private TaskType type;
    private Map<String, Object> parameters;
    private String templateId;
    private TaskStatus status;
    private String output;
    private String errorMessage;
    private int positionX;
    private int positionY;

    public enum TaskType {
        DEVICE_CONFIG,
        TEMPLATE_CONFIG,
        CONDITION,
        LOOP,
        VARIABLE_SET,
        SCRIPT_EXECUTION
    }

    public enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED
    }

    @JsonCreator
    public WorkflowTask(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("type") TaskType type) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.parameters = new HashMap<>();
        this.status = TaskStatus.PENDING;
        this.positionX = 0;
        this.positionY = 0;
    }

    public WorkflowTask(String name, TaskType type) {
        this(null, name, type);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public Object getParameter(String key) {
        return this.parameters.get(key);
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    @Override
    public String toString() {
        return "WorkflowTask{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}