package com.networkflow.service;

import com.networkflow.model.OutputParsingTemplate;
import com.networkflow.model.OutputParsingTemplate.ParsingRule;
import com.networkflow.model.OutputParsingTemplate.ParsingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing command output using predefined templates
 */
@Service
public class OutputParsingService {
    private static final Logger logger = LoggerFactory.getLogger(OutputParsingService.class);
    
    private final Map<String, OutputParsingTemplate> templates = new HashMap<>();
    
    public OutputParsingService() {
        loadPredefinedTemplates();
    }
    
    /**
     * Parse command output using appropriate template and extract variables
     */
    public Map<String, String> parseOutput(String command, String output, String deviceType) {
        Map<String, String> extractedVariables = new HashMap<>();
        
        if (output == null || output.trim().isEmpty()) {
            logger.warn("Empty output for command: {}", command);
            return extractedVariables;
        }
        
        // Find matching templates
        List<OutputParsingTemplate> matchingTemplates = findMatchingTemplates(command, deviceType);
        
        // Apply templates in order of specificity
        for (OutputParsingTemplate template : matchingTemplates) {
            Map<String, String> templateResults = applyTemplate(template, output);
            extractedVariables.putAll(templateResults);
            
            logger.debug("Applied template '{}' to command '{}', extracted {} variables", 
                        template.getName(), command, templateResults.size());
        }
        
        return extractedVariables;
    }
    
    /**
     * Apply a specific template to command output
     */
    public Map<String, String> applyTemplate(OutputParsingTemplate template, String output) {
        Map<String, String> extractedVariables = new HashMap<>();
        
        if (template.getRules() == null) {
            return extractedVariables;
        }
        
        for (ParsingRule rule : template.getRules()) {
            String value = extractVariable(rule, output);
            if (value != null) {
                extractedVariables.put(rule.getVariableName(), value);
                logger.debug("Extracted variable '{}' = '{}'", rule.getVariableName(), value);
            } else if (rule.isRequired()) {
                logger.warn("Required variable '{}' not found in output", rule.getVariableName());
                extractedVariables.put(rule.getVariableName(), rule.getDefaultValue() != null ? rule.getDefaultValue() : "");
            }
        }
        
        return extractedVariables;
    }
    
    /**
     * Extract a single variable using a parsing rule
     */
    private String extractVariable(ParsingRule rule, String output) {
        try {
            String result = null;
            
            switch (rule.getType()) {
                case REGEX:
                    result = extractByRegex(rule, output);
                    break;
                case GREP:
                    result = extractByGrep(rule, output);
                    break;
                case TABLE:
                    result = extractFromTable(rule, output);
                    break;
                case KEY_VALUE:
                    result = extractKeyValue(rule, output);
                    break;
                case LINE_COUNT:
                    result = countMatchingLines(rule, output);
                    break;
                case CONTAINS:
                    result = checkContains(rule, output);
                    break;
                case JSON:
                    result = extractFromJson(rule, output);
                    break;
                default:
                    logger.warn("Unsupported parsing type: {}", rule.getType());
                    return rule.getDefaultValue();
            }
            
            // Apply transformations
            if (result != null && rule.getTransform() != null) {
                result = applyTransformation(result, rule.getTransform());
            }
            
            return result != null ? result : rule.getDefaultValue();
            
        } catch (Exception e) {
            logger.error("Error extracting variable '{}': {}", rule.getVariableName(), e.getMessage());
            return rule.getDefaultValue();
        }
    }
    
    /**
     * Extract variable using regex pattern
     */
    private String extractByRegex(ParsingRule rule, String output) {
        Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            int groupIndex = 1;
            if (rule.getExtractionGroup() != null && !rule.getExtractionGroup().isEmpty()) {
                try {
                    groupIndex = Integer.parseInt(rule.getExtractionGroup());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid extraction group '{}', using group 1", rule.getExtractionGroup());
                }
            }
            
            if (groupIndex <= matcher.groupCount()) {
                return matcher.group(groupIndex);
            }
        }
        
        return null;
    }
    
    /**
     * Extract lines matching pattern (grep-like functionality)
     */
    private String extractByGrep(ParsingRule rule, String output) {
        Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE);
        
        return output.lines()
                .filter(line -> pattern.matcher(line).find())
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Extract value from table structure
     */
    private String extractFromTable(ParsingRule rule, String output) {
        // Implementation for table parsing
        // This would parse structured table output and extract specific columns/cells
        String[] lines = output.split("\n");
        Pattern pattern = Pattern.compile(rule.getPattern());
        
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find() && matcher.groupCount() >= 1) {
                return matcher.group(1).trim();
            }
        }
        
        return null;
    }
    
    /**
     * Extract key-value pairs
     */
    private String extractKeyValue(ParsingRule rule, String output) {
        // Look for "key: value" or "key = value" patterns
        String keyPattern = rule.getPattern();
        Pattern pattern = Pattern.compile(keyPattern + "\\s*[:=]\\s*(.+)", Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
    
    /**
     * Count lines matching pattern
     */
    private String countMatchingLines(ParsingRule rule, String output) {
        Pattern pattern = Pattern.compile(rule.getPattern());
        
        long count = output.lines()
                .filter(line -> pattern.matcher(line).find())
                .count();
        
        return String.valueOf(count);
    }
    
    /**
     * Check if output contains pattern
     */
    private String checkContains(ParsingRule rule, String output) {
        Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE);
        boolean contains = pattern.matcher(output).find();
        return contains ? "true" : "false";
    }
    
    /**
     * Extract from JSON output (basic implementation)
     */
    private String extractFromJson(ParsingRule rule, String output) {
        // Basic JSON extraction - would need proper JSON library for complex cases
        Pattern pattern = Pattern.compile("\"" + rule.getPattern() + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Apply transformations to extracted value
     */
    private String applyTransformation(String value, String transformation) {
        if (value == null || transformation == null) return value;
        
        switch (transformation.toLowerCase()) {
            case "uppercase":
                return value.toUpperCase();
            case "lowercase":
                return value.toLowerCase();
            case "trim":
                return value.trim();
            case "replace_spaces":
                return value.replaceAll("\\s+", "_");
            case "remove_special":
                return value.replaceAll("[^a-zA-Z0-9_]", "");
            case "extract_number":
                Pattern numberPattern = Pattern.compile("(\\d+)");
                Matcher matcher = numberPattern.matcher(value);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                return value;
            default:
                return value;
        }
    }
    
    /**
     * Find templates that match the command and device type
     */
    private List<OutputParsingTemplate> findMatchingTemplates(String command, String deviceType) {
        return templates.values().stream()
                .filter(template -> matchesTemplate(template, command, deviceType))
                .sorted((t1, t2) -> getTemplateSpecificity(t2) - getTemplateSpecificity(t1)) // Most specific first
                .collect(Collectors.toList());
    }
    
    /**
     * Check if template matches command and device type
     */
    private boolean matchesTemplate(OutputParsingTemplate template, String command, String deviceType) {
        // Check device type match
        if (template.getDeviceType() != null && !template.getDeviceType().equals("generic")) {
            if (deviceType == null || !deviceType.equalsIgnoreCase(template.getDeviceType())) {
                return false;
            }
        }
        
        // Check command pattern match
        if (template.getCommandPattern() != null) {
            Pattern pattern = Pattern.compile(template.getCommandPattern(), Pattern.CASE_INSENSITIVE);
            return pattern.matcher(command).find();
        }
        
        return true; // Generic template matches all
    }
    
    /**
     * Calculate template specificity for sorting
     */
    private int getTemplateSpecificity(OutputParsingTemplate template) {
        int specificity = 0;
        
        if (template.getDeviceType() != null && !template.getDeviceType().equals("generic")) {
            specificity += 10;
        }
        
        if (template.getCommandPattern() != null && !template.getCommandPattern().equals(".*")) {
            specificity += 5;
        }
        
        return specificity;
    }
    
    /**
     * Add or update a parsing template
     */
    public void addTemplate(OutputParsingTemplate template) {
        templates.put(template.getId(), template);
        logger.info("Added parsing template: {}", template.getName());
    }
    
    /**
     * Get all templates
     */
    public Collection<OutputParsingTemplate> getAllTemplates() {
        return templates.values();
    }
    
    /**
     * Get template by ID
     */
    public OutputParsingTemplate getTemplate(String id) {
        return templates.get(id);
    }
    
    /**
     * Remove template
     */
    public void removeTemplate(String id) {
        OutputParsingTemplate removed = templates.remove(id);
        if (removed != null) {
            logger.info("Removed parsing template: {}", removed.getName());
        }
    }
    
    /**
     * Load predefined parsing templates
     */
    private void loadPredefinedTemplates() {
        // VLAN Status Template
        OutputParsingTemplate vlanTemplate = createVlanStatusTemplate();
        addTemplate(vlanTemplate);
        
        // Interface Status Template
        OutputParsingTemplate interfaceTemplate = createInterfaceStatusTemplate();
        addTemplate(interfaceTemplate);
        
        // Routing Table Template
        OutputParsingTemplate routingTemplate = createRoutingTableTemplate();
        addTemplate(routingTemplate);
        
        // System Info Template
        OutputParsingTemplate systemTemplate = createSystemInfoTemplate();
        addTemplate(systemTemplate);
        
        // Generic Show Commands Template
        OutputParsingTemplate genericTemplate = createGenericShowTemplate();
        addTemplate(genericTemplate);
        
        logger.info("Loaded {} predefined parsing templates", templates.size());
    }
    
    /**
     * Create VLAN status parsing template
     */
    private OutputParsingTemplate createVlanStatusTemplate() {
        OutputParsingTemplate template = new OutputParsingTemplate("vlan_status", "VLAN Status Parser", 
                "Extract VLAN information from show vlan commands");
        template.setDeviceType("cisco");
        template.setCommandPattern("show\\s+vlan");
        
        List<ParsingRule> rules = Arrays.asList(
            new ParsingRule("vlan_count", "^(\\d+)\\s+\\w+", "Count of VLANs"),
            new ParsingRule("vlan_list", "^(\\d+)\\s+(\\w+)\\s+(\\w+)", "List of VLAN IDs"),
            new ParsingRule("active_vlans", "^\\d+\\s+\\w+\\s+(active)", "Active VLANs only")
        );
        
        // Configure rule types
        rules.get(0).setType(ParsingType.GREP);
        rules.get(1).setType(ParsingType.REGEX);
        rules.get(2).setType(ParsingType.GREP);
        
        template.setRules(rules);
        return template;
    }
    
    /**
     * Create interface status parsing template
     */
    private OutputParsingTemplate createInterfaceStatusTemplate() {
        OutputParsingTemplate template = new OutputParsingTemplate("interface_status", "Interface Status Parser",
                "Extract interface information from show interface commands");
        template.setDeviceType("generic");
        template.setCommandPattern("show\\s+interface");
        
        List<ParsingRule> rules = Arrays.asList(
            new ParsingRule("interface_name", "^(\\w+\\d+/\\d+)\\s+is", "Interface name"),
            new ParsingRule("interface_status", "is\\s+(up|down)", "Interface status"),
            new ParsingRule("line_protocol", "line protocol is\\s+(up|down)", "Line protocol status"),
            new ParsingRule("ip_address", "Internet address is\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)", "IP address")
        );
        
        rules.forEach(rule -> rule.setType(ParsingType.REGEX));
        template.setRules(rules);
        return template;
    }
    
    /**
     * Create routing table parsing template
     */
    private OutputParsingTemplate createRoutingTableTemplate() {
        OutputParsingTemplate template = new OutputParsingTemplate("routing_table", "Routing Table Parser",
                "Extract routing information from show ip route");
        template.setDeviceType("generic");
        template.setCommandPattern("show\\s+ip\\s+route");
        
        List<ParsingRule> rules = Arrays.asList(
            new ParsingRule("route_count", "C|S|R|O|D", "Count of routes"),
            new ParsingRule("default_route", "0\\.0\\.0\\.0/0", "Default route presence"),
            new ParsingRule("connected_routes", "C\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+)", "Connected routes")
        );
        
        rules.get(0).setType(ParsingType.LINE_COUNT);
        rules.get(1).setType(ParsingType.CONTAINS);
        rules.get(2).setType(ParsingType.GREP);
        
        template.setRules(rules);
        return template;
    }
    
    /**
     * Create system info parsing template
     */
    private OutputParsingTemplate createSystemInfoTemplate() {
        OutputParsingTemplate template = new OutputParsingTemplate("system_info", "System Information Parser",
                "Extract system information from show version");
        template.setDeviceType("generic");
        template.setCommandPattern("show\\s+version");
        
        List<ParsingRule> rules = Arrays.asList(
            new ParsingRule("ios_version", "IOS.*Version\\s+([\\d\\.\\w\\(\\)]+)", "IOS Version"),
            new ParsingRule("uptime", "uptime is\\s+(.+)", "System uptime"),
            new ParsingRule("hostname", "^(\\w+)\\s+uptime", "Device hostname"),
            new ParsingRule("model", "cisco\\s+(\\w+)", "Device model")
        );
        
        rules.forEach(rule -> rule.setType(ParsingType.REGEX));
        template.setRules(rules);
        return template;
    }
    
    /**
     * Create generic show command template
     */
    private OutputParsingTemplate createGenericShowTemplate() {
        OutputParsingTemplate template = new OutputParsingTemplate("generic_show", "Generic Show Command Parser",
                "Generic parsing for any show command");
        template.setDeviceType("generic");
        template.setCommandPattern("show");
        
        List<ParsingRule> rules = Arrays.asList(
            new ParsingRule("line_count", ".*", "Total lines in output"),
            new ParsingRule("has_error", "error|invalid|failed", "Check for errors"),
            new ParsingRule("has_data", ".+", "Check if output has data")
        );
        
        rules.get(0).setType(ParsingType.LINE_COUNT);
        rules.get(1).setType(ParsingType.CONTAINS);
        rules.get(2).setType(ParsingType.CONTAINS);
        
        template.setRules(rules);
        return template;
    }
}