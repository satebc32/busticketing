package com.networkflow.controller;

import com.networkflow.service.TemplateService;
import com.networkflow.template.DeviceConfigTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * REST controller for template operations
 */
@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class TemplateController {
    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TemplateService templateService;

    @GetMapping
    public ResponseEntity<Collection<DeviceConfigTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceConfigTemplate> getTemplate(@PathVariable String id) {
        DeviceConfigTemplate template = templateService.getTemplate(id);
        if (template != null) {
            return ResponseEntity.ok(template);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/by-device-type/{deviceType}")
    public ResponseEntity<List<DeviceConfigTemplate>> getTemplatesByDeviceType(@PathVariable String deviceType) {
        return ResponseEntity.ok(templateService.getTemplatesByDeviceType(deviceType));
    }

    @PostMapping
    public ResponseEntity<DeviceConfigTemplate> createTemplate(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String deviceType = (String) request.get("deviceType");
        String configTemplate = (String) request.get("configTemplate");
        
        DeviceConfigTemplate template = templateService.createTemplate(name, deviceType, configTemplate);
        
        // Set description if provided
        if (request.containsKey("description")) {
            template.setDescription((String) request.get("description"));
            templateService.updateTemplate(template);
        }
        
        logger.info("Created template: {}", template.getName());
        return ResponseEntity.ok(template);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceConfigTemplate> updateTemplate(@PathVariable String id, 
                                                              @RequestBody DeviceConfigTemplate template) {
        template.setId(id);
        if (templateService.updateTemplate(template)) {
            logger.info("Updated template: {}", template.getName());
            return ResponseEntity.ok(template);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        if (templateService.deleteTemplate(id)) {
            logger.info("Deleted template: {}", id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/load-predefined")
    public ResponseEntity<Map<String, String>> loadPredefinedTemplates() {
        templateService.loadPredefinedTemplates();
        logger.info("Loaded predefined templates");
        
        return ResponseEntity.ok(Map.of(
            "status", "success", 
            "message", "Predefined templates loaded successfully"
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DeviceConfigTemplate>> searchTemplates(@RequestParam String query) {
        return ResponseEntity.ok(templateService.searchTemplates(query));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<DeviceConfigTemplate> cloneTemplate(@PathVariable String id, 
                                                            @RequestParam String newName) {
        DeviceConfigTemplate clonedTemplate = templateService.cloneTemplate(id, newName);
        if (clonedTemplate != null) {
            logger.info("Cloned template {} as {}", id, newName);
            return ResponseEntity.ok(clonedTemplate);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateTemplate(@PathVariable String id) {
        DeviceConfigTemplate template = templateService.getTemplate(id);
        if (template != null) {
            List<String> errors = templateService.validateTemplate(template);
            return ResponseEntity.ok(Map.of(
                "valid", errors.isEmpty(),
                "errors", errors
            ));
        }
        return ResponseEntity.notFound().build();
    }
}