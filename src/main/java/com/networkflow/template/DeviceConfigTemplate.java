package com.networkflow.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/**
 * Represents a device configuration template that can be used in workflows
 */
public class DeviceConfigTemplate {
    private String id;
    private String name;
    private String description;
    private String deviceType;
    private String configTemplate;
    private List<TemplateParameter> parameters;
    private Map<String, String> metadata;
    private Date createdAt;
    private Date lastModified;

    @JsonCreator
    public DeviceConfigTemplate(@JsonProperty("id") String id,
                               @JsonProperty("name") String name,
                               @JsonProperty("deviceType") String deviceType,
                               @JsonProperty("configTemplate") String configTemplate) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.deviceType = deviceType;
        this.configTemplate = configTemplate;
        this.parameters = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.createdAt = new Date();
        this.lastModified = new Date();
    }

    public DeviceConfigTemplate(String name, String deviceType, String configTemplate) {
        this(null, name, deviceType, configTemplate);
    }

    /**
     * Process the template by replacing placeholders with actual values
     */
    public String processTemplate(Map<String, Object> values) {
        String processed = configTemplate;
        
        // Replace placeholders in the format ${parameterName}
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processed = processed.replace(placeholder, value);
        }
        
        // Also replace any parameter default values for parameters not provided
        for (TemplateParameter param : parameters) {
            String placeholder = "${" + param.getName() + "}";
            if (processed.contains(placeholder) && param.getDefaultValue() != null) {
                processed = processed.replace(placeholder, param.getDefaultValue());
            }
        }
        
        return processed;
    }

    /**
     * Validate that all required parameters are provided
     */
    public List<String> validateParameters(Map<String, Object> values) {
        List<String> missingParams = new ArrayList<>();
        
        for (TemplateParameter param : parameters) {
            if (param.isRequired() && !values.containsKey(param.getName())) {
                missingParams.add(param.getName());
            }
        }
        
        return missingParams;
    }

    public void addParameter(TemplateParameter parameter) {
        this.parameters.add(parameter);
        updateLastModified();
    }

    public void removeParameter(String parameterName) {
        this.parameters.removeIf(param -> param.getName().equals(parameterName));
        updateLastModified();
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

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
        updateLastModified();
    }

    public String getConfigTemplate() {
        return configTemplate;
    }

    public void setConfigTemplate(String configTemplate) {
        this.configTemplate = configTemplate;
        updateLastModified();
    }

    public List<TemplateParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<TemplateParameter> parameters) {
        this.parameters = parameters;
        updateLastModified();
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
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
        return "DeviceConfigTemplate{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", parameters=" + parameters.size() +
                '}';
    }
}