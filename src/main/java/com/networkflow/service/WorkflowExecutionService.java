package com.networkflow.service;

import com.networkflow.model.Workflow;
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
public class WorkflowExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionService.class);
    
    private final NetmikoService netmikoService;
    private final TemplateService templateService;
    private final ExecutorService executorService;
    private final Map<String, WorkflowExecution> activeExecutions;

    public WorkflowExecutionService() {
        this.netmikoService = new NetmikoService();
        this.templateService = new TemplateService();
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
        try {
            // Get device connection parameters
            DeviceConnection connection = createDeviceConnection(task);
            
            // Get configuration commands
            String configCommands = (String) task.getParameter("config_commands");
            if (configCommands == null || configCommands.trim().isEmpty()) {
                task.setErrorMessage("No configuration commands specified");
                return false;
            }

            // Process variables in commands
            configCommands = processVariables(execution, configCommands);

            // Execute via Netmiko
            Object timeoutParam = task.getParameter("timeout");
            int timeout = timeoutParam != null ? (Integer) timeoutParam : 60;
            NetmikoResult result = netmikoService.executeDeviceConfig(connection, configCommands, timeout);
            
            task.setOutput(result.getOutput());
            
            if (result.isSuccess()) {
                // Store output variables if specified
                String outputVar = (String) task.getParameter("output_variable");
                if (outputVar != null) {
                    execution.setVariable(outputVar, result.getOutput());
                }
                return true;
            } else {
                task.setErrorMessage(result.getMessage());
                return false;
            }

        } catch (Exception e) {
            task.setErrorMessage("Device config execution error: " + e.getMessage());
            return false;
        }
    }

    private boolean executeTemplateConfigTask(WorkflowExecution execution, WorkflowTask task) {
        try {
            String templateId = task.getTemplateId();
            if (templateId == null) {
                task.setErrorMessage("No template specified");
                return false;
            }

            DeviceConfigTemplate template = templateService.getTemplate(templateId);
            if (template == null) {
                task.setErrorMessage("Template not found: " + templateId);
                return false;
            }

            // Prepare template parameters
            Map<String, Object> templateParams = new HashMap<>(task.getParameters());
            
            // Add workflow variables
            templateParams.putAll(execution.getVariables());

            // Validate required parameters
            List<String> missingParams = template.validateParameters(templateParams);
            if (!missingParams.isEmpty()) {
                task.setErrorMessage("Missing required parameters: " + missingParams);
                return false;
            }

            // Process template
            String processedConfig = template.processTemplate(templateParams);
            
            // Execute the processed configuration
            DeviceConnection connection = createDeviceConnection(task);
            Object timeoutParam = task.getParameter("timeout");
            int timeout = timeoutParam != null ? (Integer) timeoutParam : 60;
            NetmikoResult result = netmikoService.executeDeviceConfig(connection, processedConfig, timeout);
            
            task.setOutput(result.getOutput());
            
            if (result.isSuccess()) {
                String outputVar = (String) task.getParameter("output_variable");
                if (outputVar != null) {
                    execution.setVariable(outputVar, result.getOutput());
                }
                return true;
            } else {
                task.setErrorMessage(result.getMessage());
                return false;
            }

        } catch (Exception e) {
            task.setErrorMessage("Template execution error: " + e.getMessage());
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
        // Simple condition evaluation - can be extended with expression parser
        // For now, just check if a variable exists and equals a value
        if (condition.contains("==")) {
            String[] parts = condition.split("==");
            if (parts.length == 2) {
                String varName = parts[0].trim().replace("${", "").replace("}", "");
                String expectedValue = parts[1].trim().replace("\"", "");
                Object actualValue = execution.getVariable(varName);
                return expectedValue.equals(String.valueOf(actualValue));
            }
        }
        return false;
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