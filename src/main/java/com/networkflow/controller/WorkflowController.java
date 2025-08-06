package com.networkflow.controller;

import com.networkflow.model.Workflow;
import com.networkflow.model.WorkflowTask;
import com.networkflow.model.TaskConnection;
import com.networkflow.service.WorkflowExecutionService;
import com.networkflow.service.WorkflowExecutionService.WorkflowExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for workflow operations
 */
@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
public class WorkflowController {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private WorkflowExecutionService executionService;

    // In-memory storage for demo purposes
    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();

    @GetMapping
    public ResponseEntity<Collection<Workflow>> getAllWorkflows() {
        return ResponseEntity.ok(workflows.values());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getWorkflow(@PathVariable String id) {
        Workflow workflow = workflows.get(id);
        if (workflow != null) {
            return ResponseEntity.ok(workflow);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@RequestBody Workflow workflow) {
        if (workflow.getId() == null) {
            workflow.setId(UUID.randomUUID().toString());
        }
        workflows.put(workflow.getId(), workflow);
        logger.info("Created workflow: {}", workflow.getName());
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workflow> updateWorkflow(@PathVariable String id, @RequestBody Workflow workflow) {
        if (workflows.containsKey(id)) {
            workflow.setId(id);
            workflows.put(id, workflow);
            logger.info("Updated workflow: {}", workflow.getName());
            return ResponseEntity.ok(workflow);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        if (workflows.remove(id) != null) {
            logger.info("Deleted workflow: {}", id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<WorkflowTask> addTask(@PathVariable String id, @RequestBody WorkflowTask task) {
        Workflow workflow = workflows.get(id);
        if (workflow != null) {
            if (task.getId() == null) {
                task.setId(UUID.randomUUID().toString());
            }
            workflow.addTask(task);
            logger.info("Added task {} to workflow {}", task.getName(), workflow.getName());
            return ResponseEntity.ok(task);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{workflowId}/tasks/{taskId}")
    public ResponseEntity<WorkflowTask> updateTask(@PathVariable String workflowId, 
                                                  @PathVariable String taskId, 
                                                  @RequestBody WorkflowTask task) {
        Workflow workflow = workflows.get(workflowId);
        if (workflow != null) {
            WorkflowTask existingTask = workflow.getTaskById(taskId);
            if (existingTask != null) {
                task.setId(taskId);
                // Update task properties
                existingTask.setName(task.getName());
                existingTask.setType(task.getType());
                existingTask.setParameters(task.getParameters());
                existingTask.setTemplateId(task.getTemplateId());
                existingTask.setPositionX(task.getPositionX());
                existingTask.setPositionY(task.getPositionY());
                
                logger.info("Updated task {} in workflow {}", task.getName(), workflow.getName());
                return ResponseEntity.ok(existingTask);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{workflowId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String workflowId, @PathVariable String taskId) {
        Workflow workflow = workflows.get(workflowId);
        if (workflow != null) {
            workflow.removeTask(taskId);
            logger.info("Deleted task {} from workflow {}", taskId, workflow.getName());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{workflowId}/tasks/{taskId}/insert-after")
    public ResponseEntity<WorkflowTask> insertTaskAfter(@PathVariable String workflowId,
                                                       @PathVariable String taskId,
                                                       @RequestBody WorkflowTask newTask) {
        Workflow workflow = workflows.get(workflowId);
        if (workflow != null) {
            WorkflowTask afterTask = workflow.getTaskById(taskId);
            if (afterTask != null) {
                if (newTask.getId() == null) {
                    newTask.setId(UUID.randomUUID().toString());
                }
                
                // Position the new task to the right of the after task
                newTask.setPositionX(afterTask.getPositionX() + 150);
                newTask.setPositionY(afterTask.getPositionY());
                
                // Find connections from afterTask and redirect them through newTask
                List<TaskConnection> connectionsToRedirect = new ArrayList<>();
                for (TaskConnection connection : workflow.getConnections()) {
                    if (connection.getSourceTaskId().equals(taskId)) {
                        connectionsToRedirect.add(connection);
                    }
                }
                
                // Remove old connections and create new ones
                for (TaskConnection oldConnection : connectionsToRedirect) {
                    workflow.removeConnection(oldConnection.getSourceTaskId(), oldConnection.getTargetTaskId());
                    
                    // Connect afterTask -> newTask
                    workflow.addConnection(new TaskConnection(taskId, newTask.getId()));
                    
                    // Connect newTask -> original target
                    workflow.addConnection(new TaskConnection(newTask.getId(), oldConnection.getTargetTaskId()));
                }
                
                // If no outgoing connections, just connect afterTask -> newTask
                if (connectionsToRedirect.isEmpty()) {
                    workflow.addConnection(new TaskConnection(taskId, newTask.getId()));
                }
                
                workflow.addTask(newTask);
                logger.info("Inserted task {} after task {} in workflow {}", 
                           newTask.getName(), afterTask.getName(), workflow.getName());
                return ResponseEntity.ok(newTask);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/connections")
    public ResponseEntity<TaskConnection> addConnection(@PathVariable String id, @RequestBody TaskConnection connection) {
        Workflow workflow = workflows.get(id);
        if (workflow != null) {
            workflow.addConnection(connection);
            logger.info("Added connection from {} to {} in workflow {}", 
                       connection.getSourceTaskId(), connection.getTargetTaskId(), workflow.getName());
            return ResponseEntity.ok(connection);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{workflowId}/connections")
    public ResponseEntity<Void> deleteConnection(@PathVariable String workflowId,
                                                @RequestParam String sourceTaskId,
                                                @RequestParam String targetTaskId) {
        Workflow workflow = workflows.get(workflowId);
        if (workflow != null) {
            workflow.removeConnection(sourceTaskId, targetTaskId);
            logger.info("Deleted connection from {} to {} in workflow {}", 
                       sourceTaskId, targetTaskId, workflow.getName());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, String>> executeWorkflow(@PathVariable String id) {
        Workflow workflow = workflows.get(id);
        if (workflow != null) {
            CompletableFuture<WorkflowExecutionResult> future = executionService.executeWorkflowAsync(workflow);
            
            // Return execution started response
            Map<String, String> response = new HashMap<>();
            response.put("status", "started");
            response.put("message", "Workflow execution started");
            response.put("workflowId", id);
            
            logger.info("Started execution of workflow: {}", workflow.getName());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus(@PathVariable String id) {
        Workflow workflow = workflows.get(id);
        if (workflow != null) {
            Map<String, Object> status = new HashMap<>();
            status.put("workflowId", id);
            status.put("name", workflow.getName());
            status.put("status", workflow.getStatus());
            status.put("taskCount", workflow.getTasks().size());
            
            // Task status summary
            Map<String, Integer> taskStatusCounts = new HashMap<>();
            for (WorkflowTask task : workflow.getTasks()) {
                String taskStatus = task.getStatus().toString();
                taskStatusCounts.put(taskStatus, taskStatusCounts.getOrDefault(taskStatus, 0) + 1);
            }
            status.put("taskStatusCounts", taskStatusCounts);
            
            return ResponseEntity.ok(status);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/sample")
    public ResponseEntity<Workflow> createSampleWorkflow() {
        Workflow workflow = createSampleNetworkWorkflow();
        workflows.put(workflow.getId(), workflow);
        logger.info("Created sample workflow: {}", workflow.getName());
        return ResponseEntity.ok(workflow);
    }

    private Workflow createSampleNetworkWorkflow() {
        Workflow workflow = new Workflow("Sample Network Configuration");
        workflow.setDescription("Sample workflow showing VLAN configuration across multiple switches");
        
        // Set global variables
        workflow.setGlobalVariable("vlan_id", "100");
        workflow.setGlobalVariable("vlan_name", "Production_VLAN");
        
        // Task 1: Configure VLAN on Switch 1
        WorkflowTask vlanConfig1 = new WorkflowTask("Configure VLAN Switch 1", WorkflowTask.TaskType.DEVICE_CONFIG);
        vlanConfig1.addParameter("device_type", "cisco_ios");
        vlanConfig1.addParameter("host", "192.168.1.10");
        vlanConfig1.addParameter("username", "admin");
        vlanConfig1.addParameter("password", "admin123");
        vlanConfig1.addParameter("config_commands", 
            "configure terminal\n" +
            "vlan ${vlan_id}\n" +
            "name ${vlan_name}\n" +
            "exit\n" +
            "interface GigabitEthernet0/1\n" +
            "switchport mode access\n" +
            "switchport access vlan ${vlan_id}\n" +
            "exit\n" +
            "exit");
        vlanConfig1.setPositionX(100);
        vlanConfig1.setPositionY(100);
        
        // Task 2: Configure VLAN on Switch 2
        WorkflowTask vlanConfig2 = new WorkflowTask("Configure VLAN Switch 2", WorkflowTask.TaskType.DEVICE_CONFIG);
        vlanConfig2.addParameter("device_type", "cisco_ios");
        vlanConfig2.addParameter("host", "192.168.1.11");
        vlanConfig2.addParameter("username", "admin");
        vlanConfig2.addParameter("password", "admin123");
        vlanConfig2.addParameter("config_commands", 
            "configure terminal\n" +
            "vlan ${vlan_id}\n" +
            "name ${vlan_name}\n" +
            "exit\n" +
            "interface GigabitEthernet0/2\n" +
            "switchport mode access\n" +
            "switchport access vlan ${vlan_id}\n" +
            "exit\n" +
            "exit");
        vlanConfig2.setPositionX(100);
        vlanConfig2.setPositionY(250);
        
        // Task 3: Verification
        WorkflowTask verification = new WorkflowTask("Verify Configuration", WorkflowTask.TaskType.DEVICE_CONFIG);
        verification.addParameter("device_type", "cisco_ios");
        verification.addParameter("host", "192.168.1.10");
        verification.addParameter("username", "admin");
        verification.addParameter("password", "admin123");
        verification.addParameter("config_commands", "show vlan id ${vlan_id}\nshow interfaces status");
        verification.setPositionX(350);
        verification.setPositionY(175);
        
        // Add tasks
        workflow.addTask(vlanConfig1);
        workflow.addTask(vlanConfig2);
        workflow.addTask(verification);
        
        // Add connections
        workflow.addConnection(new TaskConnection(vlanConfig1.getId(), verification.getId()));
        workflow.addConnection(new TaskConnection(vlanConfig2.getId(), verification.getId()));
        
        return workflow;
    }
}