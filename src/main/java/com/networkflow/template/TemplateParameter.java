package com.networkflow.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a parameter in a device configuration template
 */
public class TemplateParameter {
    private String name;
    private String description;
    private ParameterType type;
    private boolean required;
    private String defaultValue;
    private String validationRegex;

    public enum ParameterType {
        STRING,
        INTEGER,
        BOOLEAN,
        IP_ADDRESS,
        SUBNET,
        VLAN_ID,
        INTERFACE_NAME
    }

    @JsonCreator
    public TemplateParameter(@JsonProperty("name") String name,
                           @JsonProperty("type") ParameterType type,
                           @JsonProperty("required") boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public TemplateParameter(String name, ParameterType type) {
        this(name, type, false);
    }

    /**
     * Validate a value against this parameter's constraints
     */
    public boolean validateValue(Object value) {
        if (value == null) {
            return !required || defaultValue != null;
        }

        String stringValue = value.toString();

        // Type-specific validation
        switch (type) {
            case INTEGER:
                try {
                    Integer.parseInt(stringValue);
                } catch (NumberFormatException e) {
                    return false;
                }
                break;
            case BOOLEAN:
                if (!"true".equalsIgnoreCase(stringValue) && !"false".equalsIgnoreCase(stringValue)) {
                    return false;
                }
                break;
            case IP_ADDRESS:
                if (!isValidIPAddress(stringValue)) {
                    return false;
                }
                break;
            case VLAN_ID:
                try {
                    int vlanId = Integer.parseInt(stringValue);
                    if (vlanId < 1 || vlanId > 4094) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
                break;
        }

        // Regex validation if specified
        if (validationRegex != null && !stringValue.matches(validationRegex)) {
            return false;
        }

        return true;
    }

    private boolean isValidIPAddress(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ParameterType getType() {
        return type;
    }

    public void setType(ParameterType type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValidationRegex() {
        return validationRegex;
    }

    public void setValidationRegex(String validationRegex) {
        this.validationRegex = validationRegex;
    }

    @Override
    public String toString() {
        return "TemplateParameter{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", required=" + required +
                '}';
    }
}