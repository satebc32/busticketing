package com.networkflow.controller;

import com.networkflow.model.OutputParsingTemplate;
import com.networkflow.service.OutputParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * REST controller for managing output parsing templates
 */
@RestController
@RequestMapping("/api/output-parsing")
public class OutputParsingController {
    private static final Logger logger = LoggerFactory.getLogger(OutputParsingController.class);
    
    @Autowired
    private OutputParsingService outputParsingService;
    
    /**
     * Get all output parsing templates
     */
    @GetMapping("/templates")
    public ResponseEntity<Collection<OutputParsingTemplate>> getAllTemplates() {
        try {
            Collection<OutputParsingTemplate> templates = outputParsingService.getAllTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("Error getting templates: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get specific template by ID
     */
    @GetMapping("/templates/{id}")
    public ResponseEntity<OutputParsingTemplate> getTemplate(@PathVariable String id) {
        try {
            OutputParsingTemplate template = outputParsingService.getTemplate(id);
            if (template != null) {
                return ResponseEntity.ok(template);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting template {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create or update a parsing template
     */
    @PostMapping("/templates")
    public ResponseEntity<OutputParsingTemplate> saveTemplate(@RequestBody OutputParsingTemplate template) {
        try {
            outputParsingService.addTemplate(template);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            logger.error("Error saving template: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update an existing template
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<OutputParsingTemplate> updateTemplate(@PathVariable String id, 
                                                               @RequestBody OutputParsingTemplate template) {
        try {
            template.setId(id);
            outputParsingService.addTemplate(template);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            logger.error("Error updating template {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete a template
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        try {
            outputParsingService.removeTemplate(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting template {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Test template against sample output
     */
    @PostMapping("/templates/{id}/test")
    public ResponseEntity<Map<String, String>> testTemplate(@PathVariable String id,
                                                           @RequestBody TestRequest request) {
        try {
            OutputParsingTemplate template = outputParsingService.getTemplate(id);
            if (template == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, String> results = outputParsingService.applyTemplate(template, request.getOutput());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error testing template {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Parse output using matching templates
     */
    @PostMapping("/parse")
    public ResponseEntity<Map<String, String>> parseOutput(@RequestBody ParseRequest request) {
        try {
            Map<String, String> variables = outputParsingService.parseOutput(
                request.getCommand(), 
                request.getOutput(), 
                request.getDeviceType()
            );
            return ResponseEntity.ok(variables);
        } catch (Exception e) {
            logger.error("Error parsing output: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Request object for testing templates
     */
    public static class TestRequest {
        private String output;
        
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
    }
    
    /**
     * Request object for parsing output
     */
    public static class ParseRequest {
        private String command;
        private String output;
        private String deviceType;
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    }
}