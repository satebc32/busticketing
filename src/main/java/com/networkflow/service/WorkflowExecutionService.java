package com.networkflow.service;

import com.networkflow.model.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.networkflow.model.WorkflowTask;
import com.networkflow.model.TaskConnection;
import com.networkflow.template.DeviceConfigTemplate;
import com.networkflow.service.NetmikoService.DeviceConnection;
import com.networkflow.service.NetmikoService.NetmikoResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service for executing workflows with support for templates and dynamic task insertion
 */
@Service
public class WorkflowExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionService.class);
    
    @Autowired
    private NetmikoService netmikoService;
    
    @Autowired
    private TemplateService templateService;
    
    @Autowired
    private OutputParsingService outputParsingService;
    
    private final ExecutorService executorService;
    private final Map<String, WorkflowExecution> activeExecutions;

    public WorkflowExecutionService() {
        this.executorService = Executors.newFixedThreadPool(10);
        this.activeExecutions = new ConcurrentHashMap<>();
    }

    /**
     * Execute a workflow asynchronously
     */
    public CompletableFuture<WorkflowExecutionResult> executeWorkflowAsync(Workflow workflow) {
        String executionId = UUID.randomUUID().toString();
        WorkflowExecution execution = new WorkflowExecution(executionId, workflow);
        activeExecutions.put(executionId, execution);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeWorkflow(execution);
            } finally {
                activeExecutions.remove(executionId);
            }
        }, executorService);
    }

    /**
     * Execute a workflow synchronously
     */
    public WorkflowExecutionResult executeWorkflow(Workflow workflow) {
        String executionId = UUID.randomUUID().toString();
        WorkflowExecution execution = new WorkflowExecution(executionId, workflow);
        return executeWorkflow(execution);
    }

    private WorkflowExecutionResult executeWorkflow(WorkflowExecution execution) {
        Workflow workflow = execution.getWorkflow();
        logger.info("Starting execution of workflow: {}", workflow.getName());

        try {
            workflow.setStatus(Workflow.WorkflowStatus.RUNNING);
            
            // Find starting tasks (tasks with no predecessors)
            List<WorkflowTask> startingTasks = findStartingTasks(workflow);
            
            if (startingTasks.isEmpty()) {
                return new WorkflowExecutionResult(false, "No starting tasks found", execution);
            }

            // Execute tasks in order
            Queue<WorkflowTask> taskQueue = new LinkedList<>(startingTasks);
            Set<String> completedTasks = new HashSet<>();
            
            while (!taskQueue.isEmpty()) {
                WorkflowTask currentTask = taskQueue.poll();
                
                if (completedTasks.contains(currentTask.getId())) {
                    continue;
                }

                // Check if all predecessor tasks are completed
                if (!arePrerequisitesCompleted(workflow, currentTask, completedTasks)) {
                    taskQueue.offer(currentTask); // Re-queue for later
                    continue;
                }

                // Execute the task
                boolean taskSuccess = executeTask(execution, currentTask);
                completedTasks.add(currentTask.getId());

                if (!taskSuccess && isTaskCritical(currentTask)) {
                    workflow.setStatus(Workflow.WorkflowStatus.FAILED);
                    return new WorkflowExecutionResult(false, 
                        "Critical task failed: " + currentTask.getName(), execution);
                }

                // Add next tasks to queue
                List<WorkflowTask> nextTasks = getNextEligibleTasks(workflow, currentTask, taskSuccess);
                taskQueue.addAll(nextTasks);
            }

            workflow.setStatus(Workflow.WorkflowStatus.COMPLETED);
            logger.info("Workflow execution completed successfully: {}", workflow.getName());
            
            return new WorkflowExecutionResult(true, "Workflow completed successfully", execution);

        } catch (Exception e) {
            logger.error("Error executing workflow: " + workflow.getName(), e);
            workflow.setStatus(Workflow.WorkflowStatus.FAILED);
            return new WorkflowExecutionResult(false, "Execution error: " + e.getMessage(), execution);
        }
    }

    /**
     * Execute a single task
     */
    private boolean executeTask(WorkflowExecution execution, WorkflowTask task) {
        logger.info("Executing task: {} ({})", task.getName(), task.getType());
        task.setStatus(WorkflowTask.TaskStatus.RUNNING);

        try {
            boolean success = false;
            String output = "";

            switch (task.getType()) {
                case DEVICE_CONFIG:
                    success = executeDeviceConfigTask(execution, task);
                    break;
                case TEMPLATE_CONFIG:
                    success = executeTemplateConfigTask(execution, task);
                    break;
                case VARIABLE_SET:
                    success = executeVariableSetTask(execution, task);
                    break;
                case CONDITION:
                    success = executeConditionTask(execution, task);
                    break;
                case SCRIPT_EXECUTION:
                    success = executeScriptTask(execution, task);
                    break;
                default:
                    logger.warn("Unknown task type: {}", task.getType());
                    success = false;
            }

            task.setStatus(success ? WorkflowTask.TaskStatus.COMPLETED : WorkflowTask.TaskStatus.FAILED);
            
            if (success) {
                logger.info("Task completed successfully: {}", task.getName());
            } else {
                logger.error("Task failed: {}", task.getName());
            }

            return success;

        } catch (Exception e) {
            logger.error("Error executing task: " + task.getName(), e);
            task.setStatus(WorkflowTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            return false;
        }
    }

    private boolean executeDeviceConfigTask(WorkflowExecution execution, WorkflowTask task) {
        // Execute device configuration task
        try {
            Map<String, Object> netmikoParams = new HashMap<>();
            netmikoParams.put("device_type", task.getParameter("device_type"));
            netmikoParams.put("host", task.getParameter("host"));
            netmikoParams.put("username", task.getParameter("username"));
            netmikoParams.put("password", task.getParameter("password"));
            netmikoParams.put("commands", task.getParameter("commands"));

            String output = netmikoService.executeCommands(netmikoParams);
            
            // Apply template-based output parsing to extract variables
            String deviceType = (String) task.getParameter("device_type");
            String commands = (String) task.getParameter("commands");
            
            // Parse output using templates for each command
            if (commands != null) {
                String[] commandList = commands.split("\n");
                for (String command : commandList) {
                    if (command.trim().isEmpty()) continue;
                    
                    Map<String, String> parsedVariables = outputParsingService.parseOutput(
                        command.trim(), output, deviceType);
                    
                    // Set all extracted variables in the execution context
                    for (Map.Entry<String, String> variable : parsedVariables.entrySet()) {
                        execution.setVariable(variable.getKey(), variable.getValue());
                        logger.debug("Set template variable '{}' = '{}'", variable.getKey(), variable.getValue());
                    }
                }
            }
            
            // Set task-specific variables with variable name based on task name
            String variableName = createVariableNameFromTask(task);
            execution.setVariable(variableName + "_result", "success");
            execution.setVariable(variableName + "_output", output);
            
            // Set specialized variables based on task content
            setSpecializedVariables(execution, task, output);
            
            return true;
        } catch (Exception e) {
            logger.error("Error executing device config task: {}", e.getMessage());
            String variableName = createVariableNameFromTask(task);
            execution.setVariable(variableName + "_result", "failed");
            execution.setVariable(variableName + "_output", e.getMessage());
            return false;
        }
    }

    private boolean executeTemplateConfigTask(WorkflowExecution execution, WorkflowTask task) {
        try {
            Object templateIdParam = task.getParameter("template_id");
            if (templateIdParam == null) {
                task.setErrorMessage("No template specified");
                return false;
            }

            String templateId = (String) templateIdParam;
            DeviceConfigTemplate template = templateService.getTemplate(templateId);
            if (template == null) {
                task.setErrorMessage("Template not found: " + templateId);
                return false;
            }

            // Apply template with variables
            String configCommands = templateService.applyTemplate(template, execution.getVariables());

            Map<String, Object> netmikoParams = new HashMap<>();
            netmikoParams.put("device_type", task.getParameter("device_type"));
            netmikoParams.put("host", task.getParameter("host"));
            netmikoParams.put("username", task.getParameter("username"));
            netmikoParams.put("password", task.getParameter("password"));
            netmikoParams.put("commands", configCommands);

            String output = netmikoService.executeCommands(netmikoParams);
            
            // Apply template-based output parsing to extract variables
            String deviceType = (String) task.getParameter("device_type");
            
            // Parse output using templates for each command in the applied template
            if (configCommands != null) {
                String[] commandList = configCommands.split("\n");
                for (String command : commandList) {
                    if (command.trim().isEmpty()) continue;
                    
                    Map<String, String> parsedVariables = outputParsingService.parseOutput(
                        command.trim(), output, deviceType);
                    
                    // Set all extracted variables in the execution context
                    for (Map.Entry<String, String> variable : parsedVariables.entrySet()) {
                        execution.setVariable(variable.getKey(), variable.getValue());
                        logger.debug("Set template variable '{}' = '{}'", variable.getKey(), variable.getValue());
                    }
                }
            }

            // Set task-specific variables with variable name based on task name
            String variableName = createVariableNameFromTask(task);
            execution.setVariable(variableName + "_result", "success");
            execution.setVariable(variableName + "_output", output);
            
            // Set specialized variables based on task content
            setSpecializedVariables(execution, task, output);

            return true;
        } catch (Exception e) {
            logger.error("Error executing template config task: {}", e.getMessage());
            String variableName = createVariableNameFromTask(task);
            execution.setVariable(variableName + "_result", "failed");
            execution.setVariable(variableName + "_output", e.getMessage());
            return false;
        }
    }

    private boolean executeVariableSetTask(WorkflowExecution execution, WorkflowTask task) {
        try {
            String variableName = (String) task.getParameter("variable_name");
            Object variableValue = task.getParameter("variable_value");
            
            if (variableName == null) {
                task.setErrorMessage("Variable name not specified");
                return false;
            }

            // Process the value to handle variable substitution
            if (variableValue instanceof String) {
                variableValue = processVariables(execution, (String) variableValue);
            }

            execution.setVariable(variableName, variableValue);
            task.setOutput("Variable set: " + variableName + " = " + variableValue);
            
            return true;

        } catch (Exception e) {
            task.setErrorMessage("Variable set error: " + e.getMessage());
            return false;
        }
    }

    private boolean executeConditionTask(WorkflowExecution execution, WorkflowTask task) {
        try {
            String condition = (String) task.getParameter("condition");
            if (condition == null) {
                task.setErrorMessage("No condition specified");
                return false;
            }

            // Simple condition evaluation (can be extended)
            boolean result = evaluateCondition(execution, condition);
            task.setOutput("Condition result: " + result);
            
            return result;

        } catch (Exception e) {
            task.setErrorMessage("Condition evaluation error: " + e.getMessage());
            return false;
        }
    }

    private boolean executeScriptTask(WorkflowExecution execution, WorkflowTask task) {
        // Placeholder for script execution
        task.setOutput("Script execution not implemented yet");
        return true;
    }

    // Helper methods
    private DeviceConnection createDeviceConnection(WorkflowTask task) {
        String deviceType = (String) task.getParameter("device_type");
        String host = (String) task.getParameter("host");
        String username = (String) task.getParameter("username");
        String password = (String) task.getParameter("password");
        
        DeviceConnection connection = new DeviceConnection(deviceType, host, username, password);
        
        String secret = (String) task.getParameter("secret");
        if (secret != null) {
            connection.setSecret(secret);
        }
        
        Integer port = (Integer) task.getParameter("port");
        if (port != null) {
            connection.setPort(port);
        }
        
        return connection;
    }

    private String processVariables(WorkflowExecution execution, String text) {
        String processed = text;
        for (Map.Entry<String, Object> entry : execution.getVariables().entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processed = processed.replace(placeholder, value);
        }
        return processed;
    }

    private boolean evaluateCondition(WorkflowExecution execution, String condition) {
        // Enhanced condition evaluation with pattern matching
        try {
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts.length == 2) {
                    String varName = parts[0].trim().replace("${", "").replace("}", "");
                    String expectedValue = parts[1].trim().replace("\"", "");
                    Object actualValue = execution.getVariable(varName);
                    
                    // Special handling for VLAN verification
                    if (varName.contains("vlan") && expectedValue.equals("success")) {
                        return evaluateVlanSuccess(String.valueOf(actualValue));
                    }
                    
                    // Special handling for interface verification  
                    if (varName.contains("interface") && expectedValue.equals("success")) {
                        return evaluateInterfaceSuccess(String.valueOf(actualValue));
                    }
                    
                    // Standard string comparison
                    return expectedValue.equals(String.valueOf(actualValue));
                }
            }
            
            // Support for contains operations
            if (condition.contains("contains")) {
                String[] parts = condition.split("contains");
                if (parts.length == 2) {
                    String varName = parts[0].trim().replace("${", "").replace("}", "");
                    String searchText = parts[1].trim().replace("\"", "").replace("'", "");
                    Object actualValue = execution.getVariable(varName);
                    return String.valueOf(actualValue).toLowerCase().contains(searchText.toLowerCase());
                }
            }
            
            // Support for greater than operations
            if (condition.contains(">")) {
                String[] parts = condition.split(">");
                if (parts.length == 2) {
                    String varName = parts[0].trim().replace("${", "").replace("}", "");
                    String thresholdStr = parts[1].trim();
                    Object actualValue = execution.getVariable(varName);
                    try {
                        double actual = Double.parseDouble(String.valueOf(actualValue));
                        double threshold = Double.parseDouble(thresholdStr);
                        return actual > threshold;
                    } catch (NumberFormatException e) {
                        logger.warn("Could not parse numeric values for comparison: {} > {}", actualValue, thresholdStr);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error evaluating condition: {}", condition, e);
        }
        return false;
    }
    
    /**
     * Evaluate VLAN configuration success based on command output
     */
    private boolean evaluateVlanSuccess(String output) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        
        String lowerOutput = output.toLowerCase();
        
        // Check for VLAN existence indicators
        boolean vlanExists = lowerOutput.contains("vlan") && 
                           (lowerOutput.contains("active") || lowerOutput.contains("operational"));
        
        // Check for error indicators
        boolean hasErrors = lowerOutput.contains("error") || 
                          lowerOutput.contains("invalid") || 
                          lowerOutput.contains("failed") ||
                          lowerOutput.contains("not found");
        
        return vlanExists && !hasErrors;
    }
    
    /**
     * Evaluate interface configuration success based on command output  
     */
    private boolean evaluateInterfaceSuccess(String output) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        
        String lowerOutput = output.toLowerCase();
        
        // Check for successful interface configuration
        boolean interfaceUp = lowerOutput.contains("up") || lowerOutput.contains("operational");
        boolean switchportConfigured = lowerOutput.contains("switchport") && lowerOutput.contains("access");
        
        // Check for error indicators
        boolean hasErrors = lowerOutput.contains("error") || 
                          lowerOutput.contains("down") || 
                          lowerOutput.contains("failed");
        
        return (interfaceUp || switchportConfigured) && !hasErrors;
    }

    /**
     * Generic success verification based on command output analysis
     */
    private boolean evaluateGenericSuccess(String output, String taskName) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        
        String lowerOutput = output.toLowerCase();
        String lowerTaskName = taskName != null ? taskName.toLowerCase() : "";
        
        // Calculate success score based on multiple factors
        int successScore = 0;
        int failureScore = 0;
        
        // Generic success indicators (each adds +1 to success score)
        String[] successIndicators = {
            "success", "successful", "ok", "up", "active", "operational", 
            "enabled", "configured", "applied", "completed", "established",
            "connected", "reachable", "online", "ready", "valid", "accepted"
        };
        
        // Generic failure indicators (each adds +1 to failure score)
        String[] failureIndicators = {
            "error", "failed", "failure", "down", "inactive", "disabled",
            "unreachable", "timeout", "invalid", "rejected", "denied",
            "not found", "not configured", "not reachable", "connection refused",
            "authentication failed", "command not found", "syntax error"
        };
        
        // Count success indicators
        for (String indicator : successIndicators) {
            if (lowerOutput.contains(indicator)) {
                successScore++;
            }
        }
        
        // Count failure indicators  
        for (String indicator : failureIndicators) {
            if (lowerOutput.contains(indicator)) {
                failureScore++;
            }
        }
        
        // Context-specific success patterns
        successScore += evaluateContextualSuccess(lowerOutput, lowerTaskName);
        
        // Context-specific failure patterns
        failureScore += evaluateContextualFailures(lowerOutput, lowerTaskName);
        
        // Decision logic: success if more success indicators than failure indicators
        // and at least one positive indicator
        return successScore > failureScore && successScore > 0;
    }
    
    /**
     * Evaluate context-specific success patterns based on task type and output
     */
    private int evaluateContextualSuccess(String lowerOutput, String lowerTaskName) {
        int score = 0;
        
        // VLAN-specific success patterns
        if (lowerTaskName.contains("vlan") || lowerOutput.contains("vlan")) {
            if (lowerOutput.matches(".*vlan\\s+\\d+.*active.*")) score += 2;
            if (lowerOutput.contains("switchport access vlan")) score += 2;
            if (lowerOutput.contains("vlan database")) score += 1;
        }
        
        // Interface-specific success patterns
        if (lowerTaskName.contains("interface") || lowerOutput.contains("interface")) {
            if (lowerOutput.matches(".*interface.*up.*up.*")) score += 2; // "up/up" status
            if (lowerOutput.contains("line protocol is up")) score += 2;
            if (lowerOutput.contains("switchport mode")) score += 1;
        }
        
        // Routing-specific success patterns
        if (lowerTaskName.contains("route") || lowerOutput.contains("route")) {
            if (lowerOutput.contains("connected") && lowerOutput.contains("via")) score += 2;
            if (lowerOutput.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+.*")) score += 1; // IP/mask pattern
        }
        
        // Configuration save success
        if (lowerTaskName.contains("save") || lowerTaskName.contains("write")) {
            if (lowerOutput.contains("building configuration")) score += 2;
            if (lowerOutput.contains("[ok]") || lowerOutput.contains("saved")) score += 2;
        }
        
        // Connectivity tests
        if (lowerTaskName.contains("ping") || lowerOutput.contains("ping")) {
            if (lowerOutput.matches(".*\\d+\\s+packets.*\\d+\\s+received.*")) score += 2;
            if (lowerOutput.contains("reply from")) score += 1;
        }
        
        // Show commands generally successful if they return structured data
        if (lowerTaskName.startsWith("show") || lowerOutput.contains("show")) {
            if (lowerOutput.length() > 50 && !lowerOutput.contains("invalid")) score += 1;
            if (lowerOutput.contains("|") || lowerOutput.contains("----")) score += 1; // Table format
        }
        
        // Configuration mode success
        if (lowerOutput.contains("config)#") || lowerOutput.contains("(config")) {
            score += 1;
        }
        
        return score;
    }
    
    /**
     * Evaluate context-specific failure patterns
     */
    private int evaluateContextualFailures(String lowerOutput, String lowerTaskName) {
        int score = 0;
        
        // Network-specific error patterns
        if (lowerOutput.contains("% invalid") || lowerOutput.contains("% incomplete")) score += 2;
        if (lowerOutput.contains("% ambiguous command")) score += 2;
        if (lowerOutput.contains("% unknown command")) score += 2;
        
        // Authentication failures
        if (lowerOutput.contains("authentication failed") || 
            lowerOutput.contains("login incorrect") ||
            lowerOutput.contains("access denied")) score += 3;
        
        // Connection failures  
        if (lowerOutput.contains("connection timed out") ||
            lowerOutput.contains("no route to host") ||
            lowerOutput.contains("connection refused")) score += 3;
        
        // Configuration errors
        if (lowerOutput.contains("configuration error") ||
            lowerOutput.contains("syntax error") ||
            lowerOutput.contains("invalid input")) score += 2;
        
        // Device-specific failures
        if (lowerOutput.contains("device not found") ||
            lowerOutput.contains("no such interface") ||
            lowerOutput.contains("vlan does not exist")) score += 2;
        
        return score;
    }
    
    /**
     * Enhanced success evaluation that combines specialized and generic checks
     */
    private boolean evaluateOutputSuccess(String output, String taskName, String taskType) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        
        // Try specialized evaluation first
        switch (taskType.toLowerCase()) {
            case "vlan":
                return evaluateVlanSuccess(output);
            case "interface":
                return evaluateInterfaceSuccess(output);
            case "ping":
            case "connectivity":
                return evaluatePingSuccess(output);
            default:
                // Fall back to generic evaluation
                return evaluateGenericSuccess(output, taskName);
        }
    }
    
    /**
     * Enhanced ping success evaluation
     */
    private boolean evaluatePingSuccess(String output) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        
        String lowerOutput = output.toLowerCase();
        
        // Positive ping indicators
        boolean hasSuccess = lowerOutput.contains("reply from") ||
                           lowerOutput.contains("64 bytes from") ||
                           lowerOutput.contains("ping statistics") ||
                           lowerOutput.matches(".*\\d+\\s+packets.*\\d+\\s+received.*0%\\s+packet\\s+loss.*");
        
        // Negative ping indicators
        boolean hasFailure = lowerOutput.contains("destination host unreachable") ||
                           lowerOutput.contains("request timed out") ||
                           lowerOutput.contains("100% packet loss") ||
                           lowerOutput.contains("ping: cannot resolve");
        
        return hasSuccess && !hasFailure;
    }

    private List<WorkflowTask> findStartingTasks(Workflow workflow) {
        Set<String> tasksWithPredecessors = new HashSet<>();
        for (TaskConnection connection : workflow.getConnections()) {
            tasksWithPredecessors.add(connection.getTargetTaskId());
        }
        
        return workflow.getTasks().stream()
                .filter(task -> !tasksWithPredecessors.contains(task.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean arePrerequisitesCompleted(Workflow workflow, WorkflowTask task, Set<String> completedTasks) {
        List<WorkflowTask> predecessors = workflow.getPreviousTasks(task.getId());
        return predecessors.stream().allMatch(pred -> completedTasks.contains(pred.getId()));
    }

    private List<WorkflowTask> getNextEligibleTasks(Workflow workflow, WorkflowTask currentTask, boolean taskSuccess) {
        List<WorkflowTask> nextTasks = new ArrayList<>();
        
        for (TaskConnection connection : workflow.getConnections()) {
            if (connection.getSourceTaskId().equals(currentTask.getId())) {
                // Check connection type
                boolean shouldExecute = false;
                switch (connection.getType()) {
                    case NORMAL:
                        shouldExecute = true;
                        break;
                    case SUCCESS:
                        shouldExecute = taskSuccess;
                        break;
                    case FAILURE:
                        shouldExecute = !taskSuccess;
                        break;
                    case CONDITIONAL:
                        // Evaluate condition if specified
                        shouldExecute = true; // Simplified for now
                        break;
                }
                
                if (shouldExecute) {
                    WorkflowTask nextTask = workflow.getTaskById(connection.getTargetTaskId());
                    if (nextTask != null) {
                        nextTasks.add(nextTask);
                    }
                }
            }
        }
        
        return nextTasks;
    }

    private boolean isTaskCritical(WorkflowTask task) {
        Boolean critical = (Boolean) task.getParameter("critical");
        return critical != null && critical;
    }

    /**
     * Create a variable name from task name by removing spaces and special characters
     */
    private String createVariableNameFromTask(WorkflowTask task) {
        String taskName = task.getName();
        if (taskName == null || taskName.trim().isEmpty()) {
            return "task_" + task.getId();
        }
        
        // Convert to lowercase and replace spaces/special chars with underscores
        return taskName.toLowerCase()
                      .replaceAll("[^a-z0-9]+", "_")
                      .replaceAll("^_+|_+$", ""); // Remove leading/trailing underscores
    }
    
    /**
     * Set specialized variables based on task content and output patterns
     */
    private void setSpecializedVariables(WorkflowExecution execution, WorkflowTask task, String output) {
        String taskName = task.getName().toLowerCase();
        String outputLower = output.toLowerCase();
        
        // Use enhanced generic success evaluation
        boolean genericSuccess = evaluateGenericSuccess(output, taskName);
        execution.setVariable("generic_status", genericSuccess ? "success" : "failed");
        
        // VLAN-specific variable detection
        if (taskName.contains("vlan")) {
            boolean vlanSuccess = evaluateOutputSuccess(output, taskName, "vlan");
            execution.setVariable("vlan_status", vlanSuccess ? "success" : "failed");
            
            // Extract VLAN ID if present in task parameters
            Object vlanIdParam = task.getParameter("vlan_id");
            if (vlanIdParam != null) {
                String vlanId = String.valueOf(vlanIdParam);
                execution.setVariable("vlan_" + vlanId + "_status", vlanSuccess ? "active" : "failed");
            }
        }
        
        // Interface-specific variable detection
        if (taskName.contains("interface") || outputLower.contains("interface")) {
            boolean interfaceSuccess = evaluateOutputSuccess(output, taskName, "interface");
            execution.setVariable("interface_status", interfaceSuccess ? "success" : "failed");
        }
        
        // Ping/connectivity detection
        if (taskName.contains("ping") || outputLower.contains("ping")) {
            boolean pingSuccess = evaluateOutputSuccess(output, taskName, "ping");
            execution.setVariable("ping_status", pingSuccess ? "success" : "failed");
        }
        
        // Configuration verification detection
        if (taskName.contains("verify") || taskName.contains("check")) {
            boolean verificationSuccess = evaluateGenericSuccess(output, taskName);
            execution.setVariable("verification_status", verificationSuccess ? "success" : "failed");
        }
        
        // Route/routing detection
        if (taskName.contains("route") || taskName.contains("routing")) {
            boolean routeSuccess = evaluateGenericSuccess(output, taskName);
            execution.setVariable("route_status", routeSuccess ? "success" : "failed");
        }
        
        // Configuration save detection  
        if (taskName.contains("save") || taskName.contains("write") || taskName.contains("copy")) {
            boolean saveSuccess = evaluateGenericSuccess(output, taskName);
            execution.setVariable("save_status", saveSuccess ? "success" : "failed");
        }
        
        // Show command detection
        if (taskName.startsWith("show") || taskName.contains("display")) {
            boolean showSuccess = evaluateGenericSuccess(output, taskName);
            execution.setVariable("show_status", showSuccess ? "success" : "failed");
        }
        
        // Generic command success (always available)
        execution.setVariable("command_status", genericSuccess ? "success" : "failed");
        
        // Set confidence score for debugging
        int successScore = countSuccessIndicators(output, taskName);
        int failureScore = countFailureIndicators(output, taskName);
        execution.setVariable("success_confidence", successScore);
        execution.setVariable("failure_confidence", failureScore);
    }
    
    /**
     * Count success indicators for confidence scoring
     */
    private int countSuccessIndicators(String output, String taskName) {
        if (output == null) return 0;
        
        String lowerOutput = output.toLowerCase();
        String lowerTaskName = taskName != null ? taskName.toLowerCase() : "";
        
        int score = 0;
        String[] indicators = {
            "success", "successful", "ok", "up", "active", "operational", 
            "enabled", "configured", "applied", "completed", "established",
            "connected", "reachable", "online", "ready", "valid", "accepted"
        };
        
        for (String indicator : indicators) {
            if (lowerOutput.contains(indicator)) score++;
        }
        
        return score + evaluateContextualSuccess(lowerOutput, lowerTaskName);
    }
    
    /**
     * Count failure indicators for confidence scoring
     */
    private int countFailureIndicators(String output, String taskName) {
        if (output == null) return 0;
        
        String lowerOutput = output.toLowerCase();
        String lowerTaskName = taskName != null ? taskName.toLowerCase() : "";
        
        int score = 0;
        String[] indicators = {
            "error", "failed", "failure", "down", "inactive", "disabled",
            "unreachable", "timeout", "invalid", "rejected", "denied",
            "not found", "not configured", "not reachable", "connection refused",
            "authentication failed", "command not found", "syntax error"
        };
        
        for (String indicator : indicators) {
            if (lowerOutput.contains(indicator)) score++;
        }
        
        return score + evaluateContextualFailures(lowerOutput, lowerTaskName);
    }

    // Inner classes
    public static class WorkflowExecution {
        private final String executionId;
        private final Workflow workflow;
        private final Map<String, Object> variables;
        private final Date startTime;

        public WorkflowExecution(String executionId, Workflow workflow) {
            this.executionId = executionId;
            this.workflow = workflow;
            this.variables = new HashMap<>(workflow.getGlobalVariables());
            this.startTime = new Date();
        }

        public String getExecutionId() { return executionId; }
        public Workflow getWorkflow() { return workflow; }
        public Map<String, Object> getVariables() { return variables; }
        public Date getStartTime() { return startTime; }
        
        public void setVariable(String key, Object value) {
            variables.put(key, value);
        }
        
        public Object getVariable(String key) {
            return variables.get(key);
        }
    }

    public static class WorkflowExecutionResult {
        private final boolean success;
        private final String message;
        private final WorkflowExecution execution;

        public WorkflowExecutionResult(boolean success, String message, WorkflowExecution execution) {
            this.success = success;
            this.message = message;
            this.execution = execution;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public WorkflowExecution getExecution() { return execution; }
    }
}