package com.networkflow.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/**
 * Represents a complete workflow with tasks and their connections
 */
public class Workflow {
    private String id;
    private String name;
    private String description;
    private List<WorkflowTask> tasks;
    private List<TaskConnection> connections;
    private Map<String, Object> globalVariables;
    private WorkflowStatus status;
    private Date createdAt;
    private Date lastModified;

    public enum WorkflowStatus {
        DRAFT,
        READY,
        RUNNING,
        COMPLETED,
        FAILED,
        PAUSED
    }

    @JsonCreator
    public Workflow(@JsonProperty("id") String id,
                   @JsonProperty("name") String name) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.tasks = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.globalVariables = new HashMap<>();
        this.status = WorkflowStatus.DRAFT;
        this.createdAt = new Date();
        this.lastModified = new Date();
    }

    public Workflow(String name) {
        this(null, name);
    }

    // Task management methods
    public void addTask(WorkflowTask task) {
        this.tasks.add(task);
        updateLastModified();
    }

    public void removeTask(String taskId) {
        this.tasks.removeIf(task -> task.getId().equals(taskId));
        // Remove connections related to this task
        this.connections.removeIf(conn -> 
            conn.getSourceTaskId().equals(taskId) || conn.getTargetTaskId().equals(taskId));
        updateLastModified();
    }

    public void insertTask(WorkflowTask newTask, String afterTaskId) {
        // Find the task to insert after
        int insertIndex = -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(afterTaskId)) {
                insertIndex = i + 1;
                break;
            }
        }
        
        if (insertIndex != -1) {
            tasks.add(insertIndex, newTask);
        } else {
            tasks.add(newTask);
        }
        updateLastModified();
    }

    public WorkflowTask getTaskById(String taskId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    // Connection management methods
    public void addConnection(TaskConnection connection) {
        this.connections.add(connection);
        updateLastModified();
    }

    public void removeConnection(String sourceTaskId, String targetTaskId) {
        this.connections.removeIf(conn -> 
            conn.getSourceTaskId().equals(sourceTaskId) && 
            conn.getTargetTaskId().equals(targetTaskId));
        updateLastModified();
    }

    public List<WorkflowTask> getNextTasks(String taskId) {
        List<WorkflowTask> nextTasks = new ArrayList<>();
        for (TaskConnection connection : connections) {
            if (connection.getSourceTaskId().equals(taskId)) {
                WorkflowTask nextTask = getTaskById(connection.getTargetTaskId());
                if (nextTask != null) {
                    nextTasks.add(nextTask);
                }
            }
        }
        return nextTasks;
    }

    public List<WorkflowTask> getPreviousTasks(String taskId) {
        List<WorkflowTask> previousTasks = new ArrayList<>();
        for (TaskConnection connection : connections) {
            if (connection.getTargetTaskId().equals(taskId)) {
                WorkflowTask previousTask = getTaskById(connection.getSourceTaskId());
                if (previousTask != null) {
                    previousTasks.add(previousTask);
                }
            }
        }
        return previousTasks;
    }

    private void updateLastModified() {
        this.lastModified = new Date();
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
        updateLastModified();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateLastModified();
    }

    public List<WorkflowTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<WorkflowTask> tasks) {
        this.tasks = tasks;
        updateLastModified();
    }

    public List<TaskConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<TaskConnection> connections) {
        this.connections = connections;
        updateLastModified();
    }

    public Map<String, Object> getGlobalVariables() {
        return globalVariables;
    }

    public void setGlobalVariables(Map<String, Object> globalVariables) {
        this.globalVariables = globalVariables;
        updateLastModified();
    }

    public void setGlobalVariable(String key, Object value) {
        this.globalVariables.put(key, value);
        updateLastModified();
    }

    public Object getGlobalVariable(String key) {
        return this.globalVariables.get(key);
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
        updateLastModified();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "Workflow{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", tasks=" + tasks.size() +
                ", connections=" + connections.size() +
                '}';
    }
}