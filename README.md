# Network Device Configuration Workflow Builder

A **Spring Boot web-based** drag-and-drop workflow builder for network device configuration using Netmiko. This application provides a modern web interface for creating visual workflows with templates for automating network device configuration tasks.

## Features

- **🌐 Web-Based Interface**: Modern responsive web application accessible from any browser
- **🎯 Drag-and-Drop GUI**: Visual workflow builder with intuitive drag-and-drop interface
- **📋 Template System**: Pre-defined and custom configuration templates for various device types
- **➕ Dynamic Task Insertion**: Insert tasks anywhere in the workflow via GUI
- **🔌 Netmiko Integration**: Execute device configurations using Python Netmiko library
- **🖥️ Multiple Device Support**: Cisco IOS, NX-OS, Arista EOS, Juniper JunOS, and more
- **🔄 Variable Management**: Dynamic variable substitution in templates and workflows
- **⚡ Real-time Updates**: WebSocket-based live status updates
- **🚀 REST API**: Complete RESTful API for programmatic access
- **📱 Responsive Design**: Works on desktop, tablet, and mobile devices

## Architecture

- **🌐 Web Frontend**: Modern HTML5/CSS3/JavaScript interface with Bootstrap 5
- **☕ Spring Boot Backend**: RESTful API with WebSocket support for real-time updates
- **🐍 Python Integration**: Netmiko scripts for device configuration execution
- **📋 Template Engine**: JSON-based template system with parameter validation
- **⚙️ Workflow Engine**: Multi-threaded execution engine with dependency management
- **📡 Real-time Communication**: WebSocket (STOMP) for live status updates

## Prerequisites

- **Java 17** or higher
- **Python 3.7** or higher
- **Maven 3.6** or higher
- **Modern Web Browser** (Chrome, Firefox, Safari, Edge)

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd device-config-workflow
```

### 2. Install Python Dependencies

```bash
pip install -r python/requirements.txt
```

### 3. Build the Java Application

```bash
mvn clean compile
```

### 4. Run the Web Application

```bash
mvn spring-boot:run
```

The application will be available at: **http://localhost:8080**

Or build and run the JAR:

```bash
mvn package
java -jar target/device-config-workflow-1.0.0.jar
```

## Usage

### Creating a Workflow

1. **Start the Application**: Launch the workflow builder
2. **Create New Workflow**: File → New Workflow
3. **Add Tasks**: 
   - Drag tasks from the Task Palette to the canvas
   - Or right-click on canvas → Add Task
4. **Connect Tasks**: 
   - Right-click on a task → Connect To...
   - Click on the target task to create connection
5. **Configure Tasks**: 
   - Click on a task to view/edit properties
   - Set device connection parameters
   - Configure task-specific settings

### Task Types

- **Device Config**: Execute raw commands on network devices
- **Template Config**: Use predefined templates with parameters
- **Condition**: Conditional branching based on variables
- **Set Variable**: Define workflow variables
- **Script Execution**: Custom script execution (extensible)

### Using Templates

1. **Browse Templates**: View available templates in the left panel
2. **Apply Template**: Double-click a template to add it to workflow
3. **Customize Parameters**: Edit template parameters in task properties
4. **Create Custom Templates**: Templates → Manage Templates

### Inserting Tasks Mid-Workflow

1. **Right-click on Task**: Select "Insert Task After"
2. **Choose Task Type**: Select the type of task to insert
3. **Automatic Reconnection**: The system automatically reconnects workflow connections

### Executing Workflows

1. **Validate Workflow**: Workflow → Validate Workflow
2. **Execute**: Workflow → Execute Workflow
3. **Monitor Progress**: Watch task status updates in real-time
4. **View Results**: Check execution log and task outputs

## Configuration Templates

### Example: Cisco IOS VLAN Configuration

```
configure terminal
vlan ${vlan_id}
name ${vlan_name}
exit
interface ${interface}
switchport mode access
switchport access vlan ${vlan_id}
exit
exit
```

Parameters:
- `vlan_id` (VLAN_ID, required): VLAN number (1-4094)
- `vlan_name` (STRING, required): VLAN name
- `interface` (INTERFACE_NAME, required): Interface to configure

### Example: Interface Configuration

```
configure terminal
interface ${interface}
description ${description}
ip address ${ip_address} ${subnet_mask}
no shutdown
exit
exit
```

Parameters:
- `interface` (INTERFACE_NAME, required): Interface name
- `description` (STRING, optional): Interface description
- `ip_address` (IP_ADDRESS, required): IP address
- `subnet_mask` (IP_ADDRESS, required): Subnet mask

## Device Connection Configuration

For each device task, configure:
- **Device Type**: cisco_ios, cisco_nxos, arista_eos, juniper_junos, etc.
- **Host**: Device IP address or hostname
- **Username**: SSH username
- **Password**: SSH password
- **Secret**: Enable secret (if required)
- **Port**: SSH port (default: 22)
- **Timeout**: Connection timeout (default: 20 seconds)

## Supported Device Types

- Cisco IOS/IOS-XE
- Cisco NX-OS
- Cisco ASA
- Arista EOS
- Juniper JunOS
- HP Comware
- Huawei
- Fortinet
- Palo Alto PAN-OS
- Linux

## Variable System

### Global Variables
Set at workflow level, available to all tasks:
```java
workflow.setGlobalVariable("site_name", "DataCenter_01");
workflow.setGlobalVariable("vlan_base", "100");
```

### Task Variables
Set by individual tasks for use in subsequent tasks:
```java
task.addParameter("output_variable", "device_version");
```

### Variable Substitution
Use `${variable_name}` syntax in templates and commands:
```
configure terminal
hostname ${site_name}_SW_${switch_number}
```

## API Integration

### Programmatic Workflow Creation

```java
// Create workflow
Workflow workflow = new Workflow("Network Setup");

// Create tasks
WorkflowTask vlanTask = new WorkflowTask("Create VLAN", TaskType.TEMPLATE_CONFIG);
vlanTask.setTemplateId("cisco_ios_vlan");
vlanTask.addParameter("vlan_id", "100");
vlanTask.addParameter("vlan_name", "Production");

// Add to workflow
workflow.addTask(vlanTask);

// Execute
WorkflowExecutionService service = new WorkflowExecutionService();
CompletableFuture<WorkflowExecutionResult> result = service.executeWorkflowAsync(workflow);
```

### Custom Template Creation

```java
DeviceConfigTemplate template = new DeviceConfigTemplate(
    "Custom OSPF Config",
    "cisco_ios",
    "router ospf ${process_id}\nnetwork ${network} ${wildcard} area ${area}"
);

template.addParameter(new TemplateParameter("process_id", ParameterType.INTEGER, true));
template.addParameter(new TemplateParameter("network", ParameterType.IP_ADDRESS, true));
template.addParameter(new TemplateParameter("wildcard", ParameterType.IP_ADDRESS, true));
template.addParameter(new TemplateParameter("area", ParameterType.INTEGER, true));

templateService.updateTemplate(template);
```

## Error Handling

- **Connection Errors**: Automatic retry and detailed error messages
- **Configuration Errors**: Command-level error reporting
- **Template Validation**: Parameter validation before execution
- **Workflow Validation**: Dependency checking and circular reference detection

## Logging

Application uses SLF4J with Logback for comprehensive logging:
- Workflow execution steps
- Device connection attempts
- Configuration command execution
- Error details and stack traces

Log files are written to `logs/` directory.

## Security Considerations

- **Credentials**: Store sensitive credentials securely
- **Network Access**: Ensure proper network access controls
- **Template Validation**: Validate all template inputs
- **Execution Permissions**: Run with minimal required privileges

## Extending the System

### Adding New Task Types

1. Extend `WorkflowTask.TaskType` enum
2. Implement execution logic in `WorkflowExecutionService`
3. Add GUI elements in `WorkflowCanvas`

### Custom Device Types

1. Add device type to Netmiko script
2. Update `DeviceConnection` validation
3. Create device-specific templates

### Additional Template Parameters

1. Extend `TemplateParameter.ParameterType` enum
2. Implement validation logic
3. Update GUI parameter editors

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue in the repository
- Check the documentation
- Review example workflows

## Roadmap

- [ ] Workflow save/load functionality
- [ ] Task scheduling and timing
- [ ] Integration with configuration management systems
- [ ] REST API for external integration
- [ ] Multi-device parallel execution
- [ ] Rollback and recovery mechanisms
- [ ] Advanced template editor with syntax highlighting