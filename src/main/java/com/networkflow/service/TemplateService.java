package com.networkflow.service;

import com.networkflow.template.DeviceConfigTemplate;
import com.networkflow.template.TemplateParameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing device configuration templates
 */
public class TemplateService {
    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);
    
    private final Map<String, DeviceConfigTemplate> templates;
    private final ObjectMapper objectMapper;
    private final String templatesDirectory;

    public TemplateService() {
        this.templates = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.templatesDirectory = "templates";
        
        // Create templates directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(templatesDirectory));
            loadTemplatesFromDisk();
        } catch (IOException e) {
            logger.error("Error creating templates directory", e);
        }
    }

    /**
     * Create a new device configuration template
     */
    public DeviceConfigTemplate createTemplate(String name, String deviceType, String configTemplate) {
        DeviceConfigTemplate template = new DeviceConfigTemplate(name, deviceType, configTemplate);
        templates.put(template.getId(), template);
        saveTemplateToDisk(template);
        logger.info("Created new template: {} ({})", name, template.getId());
        return template;
    }

    /**
     * Get a template by ID
     */
    public DeviceConfigTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }

    /**
     * Get all templates
     */
    public Collection<DeviceConfigTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    /**
     * Get templates by device type
     */
    public List<DeviceConfigTemplate> getTemplatesByDeviceType(String deviceType) {
        return templates.values().stream()
                .filter(template -> deviceType.equals(template.getDeviceType()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Update an existing template
     */
    public boolean updateTemplate(DeviceConfigTemplate template) {
        if (templates.containsKey(template.getId())) {
            templates.put(template.getId(), template);
            saveTemplateToDisk(template);
            logger.info("Updated template: {}", template.getName());
            return true;
        }
        return false;
    }

    /**
     * Delete a template
     */
    public boolean deleteTemplate(String templateId) {
        DeviceConfigTemplate template = templates.remove(templateId);
        if (template != null) {
            deleteTemplateFromDisk(templateId);
            logger.info("Deleted template: {}", template.getName());
            return true;
        }
        return false;
    }

    /**
     * Load predefined templates
     */
    public void loadPredefinedTemplates() {
        // Cisco IOS VLAN Configuration Template
        DeviceConfigTemplate vlanTemplate = createTemplate(
            "Cisco IOS VLAN Configuration",
            "cisco_ios",
            "configure terminal\n" +
            "vlan ${vlan_id}\n" +
            "name ${vlan_name}\n" +
            "exit\n" +
            "interface ${interface}\n" +
            "switchport mode access\n" +
            "switchport access vlan ${vlan_id}\n" +
            "exit\n" +
            "exit"
        );
        
        vlanTemplate.addParameter(new TemplateParameter("vlan_id", TemplateParameter.ParameterType.VLAN_ID, true));
        vlanTemplate.addParameter(new TemplateParameter("vlan_name", TemplateParameter.ParameterType.STRING, true));
        vlanTemplate.addParameter(new TemplateParameter("interface", TemplateParameter.ParameterType.INTERFACE_NAME, true));
        vlanTemplate.setDescription("Creates a VLAN and assigns an interface to it");

        // Cisco IOS Interface Configuration Template
        DeviceConfigTemplate interfaceTemplate = createTemplate(
            "Cisco IOS Interface Configuration",
            "cisco_ios",
            "configure terminal\n" +
            "interface ${interface}\n" +
            "description ${description}\n" +
            "ip address ${ip_address} ${subnet_mask}\n" +
            "no shutdown\n" +
            "exit\n" +
            "exit"
        );
        
        interfaceTemplate.addParameter(new TemplateParameter("interface", TemplateParameter.ParameterType.INTERFACE_NAME, true));
        interfaceTemplate.addParameter(new TemplateParameter("description", TemplateParameter.ParameterType.STRING, false));
        interfaceTemplate.addParameter(new TemplateParameter("ip_address", TemplateParameter.ParameterType.IP_ADDRESS, true));
        interfaceTemplate.addParameter(new TemplateParameter("subnet_mask", TemplateParameter.ParameterType.IP_ADDRESS, true));
        interfaceTemplate.setDescription("Configures an interface with IP address");

        // Cisco IOS Static Route Template
        DeviceConfigTemplate routeTemplate = createTemplate(
            "Cisco IOS Static Route",
            "cisco_ios",
            "configure terminal\n" +
            "ip route ${destination_network} ${subnet_mask} ${next_hop}\n" +
            "exit"
        );
        
        routeTemplate.addParameter(new TemplateParameter("destination_network", TemplateParameter.ParameterType.IP_ADDRESS, true));
        routeTemplate.addParameter(new TemplateParameter("subnet_mask", TemplateParameter.ParameterType.IP_ADDRESS, true));
        routeTemplate.addParameter(new TemplateParameter("next_hop", TemplateParameter.ParameterType.IP_ADDRESS, true));
        routeTemplate.setDescription("Adds a static route");

        // Cisco NX-OS VXLAN Configuration Template
        DeviceConfigTemplate vxlanTemplate = createTemplate(
            "Cisco NX-OS VXLAN Configuration",
            "cisco_nxos",
            "configure terminal\n" +
            "feature nv overlay\n" +
            "feature vn-segment-vlan-based\n" +
            "vlan ${vlan_id}\n" +
            "vn-segment ${vni}\n" +
            "exit\n" +
            "interface nve1\n" +
            "no shutdown\n" +
            "source-interface loopback${loopback_id}\n" +
            "member vni ${vni}\n" +
            "ingress-replication protocol bgp\n" +
            "exit\n" +
            "exit"
        );
        
        vxlanTemplate.addParameter(new TemplateParameter("vlan_id", TemplateParameter.ParameterType.VLAN_ID, true));
        vxlanTemplate.addParameter(new TemplateParameter("vni", TemplateParameter.ParameterType.INTEGER, true));
        vxlanTemplate.addParameter(new TemplateParameter("loopback_id", TemplateParameter.ParameterType.INTEGER, true));
        vxlanTemplate.setDescription("Configures VXLAN on Cisco NX-OS");

        logger.info("Loaded {} predefined templates", 4);
    }

    private void saveTemplateToDisk(DeviceConfigTemplate template) {
        try {
            Path templateFile = Paths.get(templatesDirectory, template.getId() + ".json");
            objectMapper.writeValue(templateFile.toFile(), template);
        } catch (IOException e) {
            logger.error("Error saving template to disk: " + template.getId(), e);
        }
    }

    private void deleteTemplateFromDisk(String templateId) {
        try {
            Path templateFile = Paths.get(templatesDirectory, templateId + ".json");
            Files.deleteIfExists(templateFile);
        } catch (IOException e) {
            logger.error("Error deleting template from disk: " + templateId, e);
        }
    }

    private void loadTemplatesFromDisk() {
        try {
            Path templatesPath = Paths.get(templatesDirectory);
            if (!Files.exists(templatesPath)) {
                return;
            }

            Files.list(templatesPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(this::loadTemplateFile);
                    
            logger.info("Loaded {} templates from disk", templates.size());
            
        } catch (IOException e) {
            logger.error("Error loading templates from disk", e);
        }
    }

    private void loadTemplateFile(Path templateFile) {
        try {
            DeviceConfigTemplate template = objectMapper.readValue(templateFile.toFile(), DeviceConfigTemplate.class);
            templates.put(template.getId(), template);
        } catch (IOException e) {
            logger.error("Error loading template file: " + templateFile, e);
        }
    }

    /**
     * Search templates by name or description
     */
    public List<DeviceConfigTemplate> searchTemplates(String searchTerm) {
        String lowerSearchTerm = searchTerm.toLowerCase();
        return templates.values().stream()
                .filter(template -> 
                    template.getName().toLowerCase().contains(lowerSearchTerm) ||
                    (template.getDescription() != null && 
                     template.getDescription().toLowerCase().contains(lowerSearchTerm)))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Validate a template configuration
     */
    public List<String> validateTemplate(DeviceConfigTemplate template) {
        List<String> errors = new ArrayList<>();
        
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            errors.add("Template name is required");
        }
        
        if (template.getDeviceType() == null || template.getDeviceType().trim().isEmpty()) {
            errors.add("Device type is required");
        }
        
        if (template.getConfigTemplate() == null || template.getConfigTemplate().trim().isEmpty()) {
            errors.add("Configuration template is required");
        }
        
        // Check for parameter validation
        String configText = template.getConfigTemplate();
        if (configText != null) {
            for (TemplateParameter param : template.getParameters()) {
                String placeholder = "${" + param.getName() + "}";
                if (!configText.contains(placeholder)) {
                    errors.add("Parameter '" + param.getName() + "' is defined but not used in template");
                }
            }
        }
        
        return errors;
    }

    /**
     * Clone a template
     */
    public DeviceConfigTemplate cloneTemplate(String templateId, String newName) {
        DeviceConfigTemplate original = getTemplate(templateId);
        if (original == null) {
            return null;
        }
        
        DeviceConfigTemplate clone = new DeviceConfigTemplate(
            newName, 
            original.getDeviceType(), 
            original.getConfigTemplate()
        );
        
        clone.setDescription(original.getDescription());
        clone.setParameters(new ArrayList<>(original.getParameters()));
        clone.setMetadata(new HashMap<>(original.getMetadata()));
        
        templates.put(clone.getId(), clone);
        saveTemplateToDisk(clone);
        
        logger.info("Cloned template {} as {}", original.getName(), newName);
        return clone;
    }
}