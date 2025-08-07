package com.networkflow.model;

import java.util.List;
import java.util.Map;

/**
 * Template for parsing command output and extracting variables
 */
public class OutputParsingTemplate {
    private String id;
    private String name;
    private String description;
    private String deviceType; // cisco, arista, juniper, generic, etc.
    private String commandPattern; // regex to match commands this template applies to
    private List<ParsingRule> rules;
    private Map<String, String> metadata;

    // Constructors
    public OutputParsingTemplate() {}

    public OutputParsingTemplate(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getCommandPattern() { return commandPattern; }
    public void setCommandPattern(String commandPattern) { this.commandPattern = commandPattern; }

    public List<ParsingRule> getRules() { return rules; }
    public void setRules(List<ParsingRule> rules) { this.rules = rules; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    /**
     * Individual parsing rule within a template
     */
    public static class ParsingRule {
        private String variableName;        // Variable to set (e.g., "interface_status")
        private String pattern;             // Regex pattern to match
        private String extractionGroup;     // Which regex group to extract (default: 1)
        private String defaultValue;        // Default value if pattern not found
        private String transform;           // Transformation to apply (uppercase, lowercase, trim, etc.)
        private String description;         // Description of what this rule extracts
        private boolean required;           // Whether this variable is required
        private ParsingType type;           // Type of parsing

        // Constructors
        public ParsingRule() {}

        public ParsingRule(String variableName, String pattern, String description) {
            this.variableName = variableName;
            this.pattern = pattern;
            this.description = description;
            this.extractionGroup = "1";
            this.type = ParsingType.REGEX;
            this.required = false;
        }

        // Getters and setters
        public String getVariableName() { return variableName; }
        public void setVariableName(String variableName) { this.variableName = variableName; }

        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }

        public String getExtractionGroup() { return extractionGroup; }
        public void setExtractionGroup(String extractionGroup) { this.extractionGroup = extractionGroup; }

        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

        public String getTransform() { return transform; }
        public void setTransform(String transform) { this.transform = transform; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }

        public ParsingType getType() { return type; }
        public void setType(ParsingType type) { this.type = type; }
    }

    /**
     * Type of parsing to perform
     */
    public enum ParsingType {
        REGEX,              // Standard regex pattern matching
        GREP,               // Grep-like line extraction
        TABLE,              // Parse table structure
        JSON,               // Parse JSON output
        XML,                // Parse XML output
        KEY_VALUE,          // Parse key: value pairs
        LINE_COUNT,         // Count matching lines
        CONTAINS,           // Check if output contains text
        CUSTOM              // Custom parsing function
    }
}