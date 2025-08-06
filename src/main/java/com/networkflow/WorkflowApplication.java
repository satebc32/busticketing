package com.networkflow;

import com.networkflow.gui.WorkflowCanvas;
import com.networkflow.model.Workflow;
import com.networkflow.model.WorkflowTask;
import com.networkflow.model.TaskConnection;
import com.networkflow.service.WorkflowExecutionService;
import com.networkflow.service.TemplateService;
import com.networkflow.template.DeviceConfigTemplate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * Main JavaFX application for the workflow builder
 */
public class WorkflowApplication extends Application implements WorkflowCanvas.WorkflowCanvasListener {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowApplication.class);

    private WorkflowCanvas canvas;
    private WorkflowExecutionService executionService;
    private TemplateService templateService;
    private TextArea propertiesArea;
    private ListView<DeviceConfigTemplate> templatesList;
    private Label statusLabel;
    private WorkflowTask copiedTask;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize services
            executionService = new WorkflowExecutionService();
            templateService = new TemplateService();
            templateService.loadPredefinedTemplates();

            // Create UI
            BorderPane root = createMainLayout();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            primaryStage.setTitle("Network Device Configuration Workflow Builder");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(0);
            });
            
            primaryStage.show();

            logger.info("Workflow application started successfully");

        } catch (Exception e) {
            logger.error("Error starting application", e);
            showError("Application Error", "Failed to start application: " + e.getMessage());
        }
    }

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // Menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // Main content
        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(Orientation.HORIZONTAL);

        // Left panel - Tool palette
        VBox leftPanel = createLeftPanel();
        leftPanel.setPrefWidth(250);

        // Center - Canvas
        ScrollPane canvasScroll = createCanvasPanel();
        
        // Right panel - Properties
        VBox rightPanel = createRightPanel();
        rightPanel.setPrefWidth(300);

        mainSplit.getItems().addAll(leftPanel, canvasScroll, rightPanel);
        mainSplit.setDividerPositions(0.2, 0.75);

        root.setCenter(mainSplit);

        // Status bar
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-padding: 5px;");
        root.setBottom(statusLabel);

        return root;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newWorkflow = new MenuItem("New Workflow");
        newWorkflow.setOnAction(e -> createNewWorkflow());
        
        MenuItem openWorkflow = new MenuItem("Open Workflow");
        openWorkflow.setOnAction(e -> openWorkflow());
        
        MenuItem saveWorkflow = new MenuItem("Save Workflow");
        saveWorkflow.setOnAction(e -> saveWorkflow());
        
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> Platform.exit());
        
        fileMenu.getItems().addAll(newWorkflow, openWorkflow, saveWorkflow, 
                                   new SeparatorMenuItem(), exit);

        // Workflow menu
        Menu workflowMenu = new Menu("Workflow");
        MenuItem executeWorkflow = new MenuItem("Execute Workflow");
        executeWorkflow.setOnAction(e -> executeWorkflow());
        
        MenuItem validateWorkflow = new MenuItem("Validate Workflow");
        validateWorkflow.setOnAction(e -> validateWorkflow());
        
        workflowMenu.getItems().addAll(executeWorkflow, validateWorkflow);

        // Templates menu
        Menu templatesMenu = new Menu("Templates");
        MenuItem manageTemplates = new MenuItem("Manage Templates");
        manageTemplates.setOnAction(e -> showTemplateManager());
        
        MenuItem loadPredefined = new MenuItem("Load Predefined Templates");
        loadPredefined.setOnAction(e -> {
            templateService.loadPredefinedTemplates();
            refreshTemplatesList();
            statusLabel.setText("Predefined templates loaded");
        });
        
        templatesMenu.getItems().addAll(manageTemplates, loadPredefined);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAbout());
        
        helpMenu.getItems().add(about);

        menuBar.getMenus().addAll(fileMenu, workflowMenu, templatesMenu, helpMenu);
        return menuBar;
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: #f0f0f0;");

        // Task palette
        Label taskPaletteLabel = new Label("Task Palette");
        taskPaletteLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox taskButtons = new VBox(5);
        
        Button deviceConfigBtn = new Button("Device Config");
        deviceConfigBtn.setPrefWidth(200);
        deviceConfigBtn.setOnAction(e -> createTask(WorkflowTask.TaskType.DEVICE_CONFIG));
        
        Button templateConfigBtn = new Button("Template Config");
        templateConfigBtn.setPrefWidth(200);
        templateConfigBtn.setOnAction(e -> createTask(WorkflowTask.TaskType.TEMPLATE_CONFIG));
        
        Button conditionBtn = new Button("Condition");
        conditionBtn.setPrefWidth(200);
        conditionBtn.setOnAction(e -> createTask(WorkflowTask.TaskType.CONDITION));
        
        Button variableBtn = new Button("Set Variable");
        variableBtn.setPrefWidth(200);
        variableBtn.setOnAction(e -> createTask(WorkflowTask.TaskType.VARIABLE_SET));
        
        Button scriptBtn = new Button("Script Execution");
        scriptBtn.setPrefWidth(200);
        scriptBtn.setOnAction(e -> createTask(WorkflowTask.TaskType.SCRIPT_EXECUTION));

        taskButtons.getChildren().addAll(deviceConfigBtn, templateConfigBtn, 
                                        conditionBtn, variableBtn, scriptBtn);

        // Templates list
        Label templatesLabel = new Label("Templates");
        templatesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        templatesList = new ListView<>();
        templatesList.setPrefHeight(200);
        templatesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                DeviceConfigTemplate template = templatesList.getSelectionModel().getSelectedItem();
                if (template != null) {
                    createTemplateTask(template);
                }
            }
        });

        refreshTemplatesList();

        leftPanel.getChildren().addAll(taskPaletteLabel, taskButtons, 
                                      new Separator(), templatesLabel, templatesList);
        return leftPanel;
    }

    private ScrollPane createCanvasPanel() {
        canvas = new WorkflowCanvas(1000, 800);
        canvas.setListener(this);
        
        ScrollPane scrollPane = new ScrollPane(canvas);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: white;");
        
        return scrollPane;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setStyle("-fx-background-color: #f0f0f0;");

        // Properties panel
        Label propertiesLabel = new Label("Properties");
        propertiesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        propertiesArea = new TextArea();
        propertiesArea.setPrefHeight(300);
        propertiesArea.setEditable(false);
        propertiesArea.setText("Select a task to view its properties");

        // Execution log
        Label logLabel = new Label("Execution Log");
        logLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea logArea = new TextArea();
        logArea.setPrefHeight(200);
        logArea.setEditable(false);

        rightPanel.getChildren().addAll(propertiesLabel, propertiesArea, 
                                       new Separator(), logLabel, logArea);
        return rightPanel;
    }

    private void refreshTemplatesList() {
        templatesList.getItems().clear();
        templatesList.getItems().addAll(templateService.getAllTemplates());
    }

    private void createTask(WorkflowTask.TaskType taskType) {
        WorkflowTask task = new WorkflowTask("New " + taskType.toString(), taskType);
        
        // Set default parameters based on task type
        switch (taskType) {
            case DEVICE_CONFIG:
                task.addParameter("device_type", "cisco_ios");
                task.addParameter("host", "192.168.1.1");
                task.addParameter("username", "admin");
                task.addParameter("password", "password");
                task.addParameter("config_commands", "show version");
                break;
            case TEMPLATE_CONFIG:
                task.addParameter("device_type", "cisco_ios");
                task.addParameter("host", "192.168.1.1");
                task.addParameter("username", "admin");
                task.addParameter("password", "password");
                break;
            case VARIABLE_SET:
                task.addParameter("variable_name", "my_variable");
                task.addParameter("variable_value", "default_value");
                break;
            case CONDITION:
                task.addParameter("condition", "${variable} == \"value\"");
                break;
        }

        canvas.addTask(task, 100, 100);
        statusLabel.setText("Created new " + taskType + " task");
    }

    private void createTemplateTask(DeviceConfigTemplate template) {
        WorkflowTask task = new WorkflowTask(template.getName(), WorkflowTask.TaskType.TEMPLATE_CONFIG);
        task.setTemplateId(template.getId());
        
        // Add template parameters as task parameters
        task.addParameter("device_type", template.getDeviceType());
        task.addParameter("host", "192.168.1.1");
        task.addParameter("username", "admin");
        task.addParameter("password", "password");
        
        // Add template-specific parameters with default values
        template.getParameters().forEach(param -> {
            if (param.getDefaultValue() != null) {
                task.addParameter(param.getName(), param.getDefaultValue());
            }
        });

        canvas.addTask(task, 100, 100);
        statusLabel.setText("Created task from template: " + template.getName());
    }

    // WorkflowCanvas.WorkflowCanvasListener implementation
    @Override
    public void onTaskSelected(WorkflowTask task) {
        if (task != null) {
            StringBuilder props = new StringBuilder();
            props.append("Task: ").append(task.getName()).append("\n");
            props.append("Type: ").append(task.getType()).append("\n");
            props.append("Status: ").append(task.getStatus()).append("\n");
            props.append("ID: ").append(task.getId()).append("\n");
            
            if (task.getTemplateId() != null) {
                props.append("Template: ").append(task.getTemplateId()).append("\n");
            }
            
            props.append("\nParameters:\n");
            task.getParameters().forEach((key, value) -> 
                props.append("  ").append(key).append(": ").append(value).append("\n"));
            
            if (task.getOutput() != null) {
                props.append("\nOutput:\n").append(task.getOutput());
            }
            
            if (task.getErrorMessage() != null) {
                props.append("\nError:\n").append(task.getErrorMessage());
            }
            
            propertiesArea.setText(props.toString());
        } else {
            propertiesArea.setText("No task selected");
        }
    }

    @Override
    public void onTaskDeleted(WorkflowTask task) {
        statusLabel.setText("Deleted task: " + task.getName());
        propertiesArea.setText("No task selected");
    }

    @Override
    public void onConnectionCreated(TaskConnection connection) {
        statusLabel.setText("Created connection between tasks");
    }

    @Override
    public void onEditTask(WorkflowTask task) {
        showTaskEditDialog(task);
    }

    @Override
    public void onAddNewTask(double x, double y) {
        // Show task type selection dialog
        ChoiceDialog<WorkflowTask.TaskType> dialog = new ChoiceDialog<>(
            WorkflowTask.TaskType.DEVICE_CONFIG, WorkflowTask.TaskType.values());
        dialog.setTitle("Add New Task");
        dialog.setHeaderText("Select Task Type");
        dialog.setContentText("Choose the type of task to add:");

        Optional<WorkflowTask.TaskType> result = dialog.showAndWait();
        result.ifPresent(taskType -> {
            WorkflowTask task = new WorkflowTask("New " + taskType.toString(), taskType);
            canvas.addTask(task, x, y);
        });
    }

    @Override
    public void onInsertTaskAfter(WorkflowTask afterTask) {
        ChoiceDialog<WorkflowTask.TaskType> dialog = new ChoiceDialog<>(
            WorkflowTask.TaskType.DEVICE_CONFIG, WorkflowTask.TaskType.values());
        dialog.setTitle("Insert Task");
        dialog.setHeaderText("Insert Task After: " + afterTask.getName());
        dialog.setContentText("Choose the type of task to insert:");

        Optional<WorkflowTask.TaskType> result = dialog.showAndWait();
        result.ifPresent(taskType -> {
            WorkflowTask newTask = new WorkflowTask("New " + taskType.toString(), taskType);
            canvas.insertTaskAfter(newTask, afterTask);
            statusLabel.setText("Inserted task after: " + afterTask.getName());
        });
    }

    @Override
    public void onPasteTask(double x, double y) {
        if (copiedTask != null) {
            WorkflowTask newTask = new WorkflowTask(copiedTask.getName() + " (Copy)", copiedTask.getType());
            newTask.setParameters(new java.util.HashMap<>(copiedTask.getParameters()));
            newTask.setTemplateId(copiedTask.getTemplateId());
            canvas.addTask(newTask, x, y);
            statusLabel.setText("Pasted task: " + newTask.getName());
        }
    }

    // Additional methods
    private void createNewWorkflow() {
        TextInputDialog dialog = new TextInputDialog("New Workflow");
        dialog.setTitle("New Workflow");
        dialog.setHeaderText("Create New Workflow");
        dialog.setContentText("Enter workflow name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Workflow workflow = new Workflow(name);
            canvas.setWorkflow(workflow);
            statusLabel.setText("Created new workflow: " + name);
        });
    }

    private void openWorkflow() {
        // Placeholder for workflow loading
        statusLabel.setText("Open workflow not implemented yet");
    }

    private void saveWorkflow() {
        // Placeholder for workflow saving
        statusLabel.setText("Save workflow not implemented yet");
    }

    private void executeWorkflow() {
        Workflow workflow = canvas.getWorkflow();
        if (workflow.getTasks().isEmpty()) {
            showError("Execution Error", "Cannot execute empty workflow");
            return;
        }

        statusLabel.setText("Executing workflow...");
        
        // Execute asynchronously
        executionService.executeWorkflowAsync(workflow)
            .thenAccept(result -> Platform.runLater(() -> {
                if (result.isSuccess()) {
                    statusLabel.setText("Workflow executed successfully");
                    showInfo("Execution Complete", "Workflow executed successfully");
                } else {
                    statusLabel.setText("Workflow execution failed");
                    showError("Execution Failed", result.getMessage());
                }
                canvas.redraw(); // Refresh to show updated task statuses
            }));
    }

    private void validateWorkflow() {
        // Placeholder for workflow validation
        statusLabel.setText("Workflow validation not implemented yet");
    }

    private void showTemplateManager() {
        // Placeholder for template manager
        statusLabel.setText("Template manager not implemented yet");
    }

    private void showTaskEditDialog(WorkflowTask task) {
        // Placeholder for task editing dialog
        statusLabel.setText("Task editing dialog not implemented yet");
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Network Device Configuration Workflow Builder");
        alert.setContentText("Version 1.0\n\nA drag-and-drop workflow builder for network device configuration using Netmiko.");
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", "com.sun.javafx.application.LauncherImpl");
        launch(args);
    }
}