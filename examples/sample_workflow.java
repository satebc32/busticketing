package examples;

import com.networkflow.model.*;
import com.networkflow.service.*;
import com.networkflow.template.*;

/**
 * Example demonstrating how to create and execute workflows programmatically
 */
public class SampleWorkflow {
    
    public static void main(String[] args) {
        // Initialize services
        WorkflowExecutionService executionService = new WorkflowExecutionService();
        TemplateService templateService = new TemplateService();
        
        // Create a new workflow
        Workflow workflow = new Workflow("Network VLAN Setup");
        workflow.setDescription("Automated VLAN configuration across multiple switches");
        
        // Set global variables
        workflow.setGlobalVariable("site_name", "DataCenter_01");
        workflow.setGlobalVariable("vlan_id", "100");
        workflow.setGlobalVariable("vlan_name", "Production_VLAN");
        
        // Task 1: Set variables for first switch
        WorkflowTask setVars1 = new WorkflowTask("Set Switch 1 Variables", WorkflowTask.TaskType.VARIABLE_SET);
        setVars1.addParameter("variable_name", "switch_ip");
        setVars1.addParameter("variable_value", "192.168.1.10");
        setVars1.setPositionX(100);
        setVars1.setPositionY(100);
        
        // Task 2: Configure VLAN on first switch using template
        WorkflowTask vlanConfig1 = new WorkflowTask("Configure VLAN Switch 1", WorkflowTask.TaskType.TEMPLATE_CONFIG);
        vlanConfig1.setTemplateId("cisco_ios_vlan"); // Assuming this template exists
        vlanConfig1.addParameter("device_type", "cisco_ios");
        vlanConfig1.addParameter("host", "${switch_ip}");
        vlanConfig1.addParameter("username", "admin");
        vlanConfig1.addParameter("password", "password");
        vlanConfig1.addParameter("vlan_id", "${vlan_id}");
        vlanConfig1.addParameter("vlan_name", "${vlan_name}");
        vlanConfig1.addParameter("interface", "GigabitEthernet0/1");
        vlanConfig1.setPositionX(300);
        vlanConfig1.setPositionY(100);
        
        // Task 3: Set variables for second switch
        WorkflowTask setVars2 = new WorkflowTask("Set Switch 2 Variables", WorkflowTask.TaskType.VARIABLE_SET);
        setVars2.addParameter("variable_name", "switch_ip");
        setVars2.addParameter("variable_value", "192.168.1.11");
        setVars2.setPositionX(100);
        setVars2.setPositionY(200);
        
        // Task 4: Configure VLAN on second switch
        WorkflowTask vlanConfig2 = new WorkflowTask("Configure VLAN Switch 2", WorkflowTask.TaskType.TEMPLATE_CONFIG);
        vlanConfig2.setTemplateId("cisco_ios_vlan");
        vlanConfig2.addParameter("device_type", "cisco_ios");
        vlanConfig2.addParameter("host", "${switch_ip}");
        vlanConfig2.addParameter("username", "admin");
        vlanConfig2.addParameter("password", "password");
        vlanConfig2.addParameter("vlan_id", "${vlan_id}");
        vlanConfig2.addParameter("vlan_name", "${vlan_name}");
        vlanConfig2.addParameter("interface", "GigabitEthernet0/2");
        vlanConfig2.setPositionX(300);
        vlanConfig2.setPositionY(200);
        
        // Task 5: Verification task
        WorkflowTask verification = new WorkflowTask("Verify Configuration", WorkflowTask.TaskType.DEVICE_CONFIG);
        verification.addParameter("device_type", "cisco_ios");
        verification.addParameter("host", "192.168.1.10"); // Check first switch
        verification.addParameter("username", "admin");
        verification.addParameter("password", "password");
        verification.addParameter("config_commands", "show vlan id ${vlan_id}\nshow interfaces status");
        verification.addParameter("output_variable", "verification_result");
        verification.setPositionX(500);
        verification.setPositionY(150);
        
        // Add tasks to workflow
        workflow.addTask(setVars1);
        workflow.addTask(vlanConfig1);
        workflow.addTask(setVars2);
        workflow.addTask(vlanConfig2);
        workflow.addTask(verification);
        
        // Create connections
        workflow.addConnection(new TaskConnection(setVars1.getId(), vlanConfig1.getId()));
        workflow.addConnection(new TaskConnection(setVars2.getId(), vlanConfig2.getId()));
        workflow.addConnection(new TaskConnection(vlanConfig1.getId(), verification.getId()));
        workflow.addConnection(new TaskConnection(vlanConfig2.getId(), verification.getId()));
        
        // Set workflow status to ready
        workflow.setStatus(Workflow.WorkflowStatus.READY);
        
        System.out.println("Created workflow: " + workflow.getName());
        System.out.println("Tasks: " + workflow.getTasks().size());
        System.out.println("Connections: " + workflow.getConnections().size());
        
        // Execute workflow (uncomment to run - requires actual devices)
        /*
        executionService.executeWorkflowAsync(workflow)
            .thenAccept(result -> {
                if (result.isSuccess()) {
                    System.out.println("Workflow executed successfully!");
                    // Print task outputs
                    for (WorkflowTask task : workflow.getTasks()) {
                        if (task.getOutput() != null) {
                            System.out.println("Task: " + task.getName());
                            System.out.println("Output: " + task.getOutput());
                            System.out.println("---");
                        }
                    }
                } else {
                    System.err.println("Workflow execution failed: " + result.getMessage());
                }
            })
            .join(); // Wait for completion
        */
    }
    
    /**
     * Example of creating a custom template
     */
    public static void createCustomTemplate(TemplateService templateService) {
        // Create a custom interface configuration template
        DeviceConfigTemplate interfaceTemplate = new DeviceConfigTemplate(
            "Interface IP Configuration",
            "cisco_ios",
            "configure terminal\n" +
            "interface ${interface}\n" +
            "description ${description}\n" +
            "ip address ${ip_address} ${subnet_mask}\n" +
            "no shutdown\n" +
            "exit\n" +
            "exit"
        );
        
        // Add parameters
        interfaceTemplate.addParameter(new TemplateParameter("interface", TemplateParameter.ParameterType.INTERFACE_NAME, true));
        interfaceTemplate.addParameter(new TemplateParameter("description", TemplateParameter.ParameterType.STRING, false));
        interfaceTemplate.addParameter(new TemplateParameter("ip_address", TemplateParameter.ParameterType.IP_ADDRESS, true));
        interfaceTemplate.addParameter(new TemplateParameter("subnet_mask", TemplateParameter.ParameterType.IP_ADDRESS, true));
        
        interfaceTemplate.setDescription("Configure interface with IP address and description");
        
        // Save template
        templateService.updateTemplate(interfaceTemplate);
        
        System.out.println("Created custom template: " + interfaceTemplate.getName());
    }
    
    /**
     * Example of workflow with conditional execution
     */
    public static Workflow createConditionalWorkflow() {
        Workflow workflow = new Workflow("Conditional Device Setup");
        
        // Task 1: Check device type
        WorkflowTask checkDevice = new WorkflowTask("Check Device Type", WorkflowTask.TaskType.DEVICE_CONFIG);
        checkDevice.addParameter("device_type", "cisco_ios");
        checkDevice.addParameter("host", "192.168.1.1");
        checkDevice.addParameter("username", "admin");
        checkDevice.addParameter("password", "password");
        checkDevice.addParameter("config_commands", "show version");
        checkDevice.addParameter("output_variable", "device_info");
        
        // Task 2: Condition to check if it's a switch
        WorkflowTask isSwitch = new WorkflowTask("Is Switch?", WorkflowTask.TaskType.CONDITION);
        isSwitch.addParameter("condition", "${device_info} contains 'Switch'");
        
        // Task 3: Switch-specific configuration
        WorkflowTask switchConfig = new WorkflowTask("Switch Configuration", WorkflowTask.TaskType.DEVICE_CONFIG);
        switchConfig.addParameter("device_type", "cisco_ios");
        switchConfig.addParameter("host", "192.168.1.1");
        switchConfig.addParameter("username", "admin");
        switchConfig.addParameter("password", "password");
        switchConfig.addParameter("config_commands", 
            "configure terminal\n" +
            "spanning-tree mode rapid-pvst\n" +
            "exit"
        );
        
        // Task 4: Router-specific configuration
        WorkflowTask routerConfig = new WorkflowTask("Router Configuration", WorkflowTask.TaskType.DEVICE_CONFIG);
        routerConfig.addParameter("device_type", "cisco_ios");
        routerConfig.addParameter("host", "192.168.1.1");
        routerConfig.addParameter("username", "admin");
        routerConfig.addParameter("password", "password");
        routerConfig.addParameter("config_commands", 
            "configure terminal\n" +
            "ip routing\n" +
            "exit"
        );
        
        // Add tasks
        workflow.addTask(checkDevice);
        workflow.addTask(isSwitch);
        workflow.addTask(switchConfig);
        workflow.addTask(routerConfig);
        
        // Create conditional connections
        workflow.addConnection(new TaskConnection(checkDevice.getId(), isSwitch.getId()));
        
        TaskConnection switchConnection = new TaskConnection(isSwitch.getId(), switchConfig.getId());
        switchConnection.setType(TaskConnection.ConnectionType.SUCCESS);
        workflow.addConnection(switchConnection);
        
        TaskConnection routerConnection = new TaskConnection(isSwitch.getId(), routerConfig.getId());
        routerConnection.setType(TaskConnection.ConnectionType.FAILURE);
        workflow.addConnection(routerConnection);
        
        return workflow;
    }
}