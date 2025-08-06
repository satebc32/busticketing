package com.networkflow.gui;

import com.networkflow.model.Workflow;
import com.networkflow.model.WorkflowTask;
import com.networkflow.model.TaskConnection;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Canvas for drag-and-drop workflow building
 */
public class WorkflowCanvas extends Canvas {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowCanvas.class);
    
    private static final double TASK_WIDTH = 120;
    private static final double TASK_HEIGHT = 60;
    private static final Color TASK_COLOR = Color.LIGHTBLUE;
    private static final Color SELECTED_COLOR = Color.ORANGE;
    private static final Color CONNECTION_COLOR = Color.DARKGRAY;
    
    private Workflow workflow;
    private WorkflowTask selectedTask;
    private WorkflowTask draggedTask;
    private boolean isConnecting;
    private WorkflowTask connectionSource;
    private double mouseX, mouseY;
    private double dragOffsetX, dragOffsetY;
    
    private WorkflowCanvasListener listener;

    public WorkflowCanvas(double width, double height) {
        super(width, height);
        this.workflow = new Workflow("New Workflow");
        setupEventHandlers();
        redraw();
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
        this.selectedTask = null;
        redraw();
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setListener(WorkflowCanvasListener listener) {
        this.listener = listener;
    }

    private void setupEventHandlers() {
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnMouseMoved(this::handleMouseMoved);
        
        setFocusTraversable(true);
        requestFocus();
    }

    private void handleMousePressed(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        
        WorkflowTask clickedTask = getTaskAtPosition(mouseX, mouseY);
        
        if (event.isSecondaryButtonDown()) {
            // Right click - show context menu
            showContextMenu(event, clickedTask);
        } else if (event.isPrimaryButtonDown()) {
            if (isConnecting) {
                // Connecting mode - create connection
                if (clickedTask != null && connectionSource != null && 
                    !clickedTask.equals(connectionSource)) {
                    createConnection(connectionSource, clickedTask);
                }
                isConnecting = false;
                connectionSource = null;
            } else {
                // Selection/dragging mode
                selectedTask = clickedTask;
                if (selectedTask != null) {
                    draggedTask = selectedTask;
                    dragOffsetX = mouseX - selectedTask.getPositionX();
                    dragOffsetY = mouseY - selectedTask.getPositionY();
                }
            }
        }
        
        redraw();
        if (listener != null) {
            listener.onTaskSelected(selectedTask);
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (draggedTask != null && !isConnecting) {
            double newX = event.getX() - dragOffsetX;
            double newY = event.getY() - dragOffsetY;
            
            // Keep within canvas bounds
            newX = Math.max(0, Math.min(newX, getWidth() - TASK_WIDTH));
            newY = Math.max(0, Math.min(newY, getHeight() - TASK_HEIGHT));
            
            draggedTask.setPositionX((int) newX);
            draggedTask.setPositionY((int) newY);
            
            redraw();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        draggedTask = null;
    }

    private void handleMouseMoved(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        
        if (isConnecting) {
            redraw();
        }
    }

    private void showContextMenu(MouseEvent event, WorkflowTask clickedTask) {
        ContextMenu contextMenu = new ContextMenu();
        
        if (clickedTask != null) {
            // Task context menu
            MenuItem editItem = new MenuItem("Edit Task");
            editItem.setOnAction(e -> {
                if (listener != null) {
                    listener.onEditTask(clickedTask);
                }
            });
            
            MenuItem deleteItem = new MenuItem("Delete Task");
            deleteItem.setOnAction(e -> deleteTask(clickedTask));
            
            MenuItem connectItem = new MenuItem("Connect To...");
            connectItem.setOnAction(e -> startConnection(clickedTask));
            
            MenuItem insertItem = new MenuItem("Insert Task After");
            insertItem.setOnAction(e -> {
                if (listener != null) {
                    listener.onInsertTaskAfter(clickedTask);
                }
            });
            
            contextMenu.getItems().addAll(editItem, deleteItem, connectItem, insertItem);
        } else {
            // Canvas context menu
            MenuItem addTaskItem = new MenuItem("Add Task");
            addTaskItem.setOnAction(e -> addNewTask(event.getX(), event.getY()));
            
            MenuItem pasteItem = new MenuItem("Paste Task");
            pasteItem.setOnAction(e -> {
                if (listener != null) {
                    listener.onPasteTask(event.getX(), event.getY());
                }
            });
            
            contextMenu.getItems().addAll(addTaskItem, pasteItem);
        }
        
        contextMenu.show(this, event.getScreenX(), event.getScreenY());
    }

    private void addNewTask(double x, double y) {
        if (listener != null) {
            listener.onAddNewTask(x, y);
        }
    }

    private void deleteTask(WorkflowTask task) {
        workflow.removeTask(task.getId());
        if (selectedTask != null && selectedTask.equals(task)) {
            selectedTask = null;
        }
        redraw();
        
        if (listener != null) {
            listener.onTaskDeleted(task);
        }
    }

    private void startConnection(WorkflowTask sourceTask) {
        isConnecting = true;
        connectionSource = sourceTask;
    }

    private void createConnection(WorkflowTask source, WorkflowTask target) {
        TaskConnection connection = new TaskConnection(source.getId(), target.getId());
        workflow.addConnection(connection);
        redraw();
        
        if (listener != null) {
            listener.onConnectionCreated(connection);
        }
    }

    public void addTask(WorkflowTask task, double x, double y) {
        task.setPositionX((int) x);
        task.setPositionY((int) y);
        workflow.addTask(task);
        selectedTask = task;
        redraw();
    }

    public void insertTaskAfter(WorkflowTask newTask, WorkflowTask afterTask) {
        // Position the new task to the right of the after task
        double newX = afterTask.getPositionX() + TASK_WIDTH + 50;
        double newY = afterTask.getPositionY();
        
        newTask.setPositionX((int) newX);
        newTask.setPositionY((int) newY);
        
        // Find connections from afterTask and redirect them through newTask
        List<TaskConnection> connectionsToRedirect = new ArrayList<>();
        for (TaskConnection connection : workflow.getConnections()) {
            if (connection.getSourceTaskId().equals(afterTask.getId())) {
                connectionsToRedirect.add(connection);
            }
        }
        
        // Remove old connections and create new ones
        for (TaskConnection oldConnection : connectionsToRedirect) {
            workflow.removeConnection(oldConnection.getSourceTaskId(), oldConnection.getTargetTaskId());
            
            // Connect afterTask -> newTask
            workflow.addConnection(new TaskConnection(afterTask.getId(), newTask.getId()));
            
            // Connect newTask -> original target
            workflow.addConnection(new TaskConnection(newTask.getId(), oldConnection.getTargetTaskId()));
        }
        
        // If no outgoing connections, just connect afterTask -> newTask
        if (connectionsToRedirect.isEmpty()) {
            workflow.addConnection(new TaskConnection(afterTask.getId(), newTask.getId()));
        }
        
        workflow.addTask(newTask);
        selectedTask = newTask;
        redraw();
    }

    private WorkflowTask getTaskAtPosition(double x, double y) {
        for (WorkflowTask task : workflow.getTasks()) {
            double taskX = task.getPositionX();
            double taskY = task.getPositionY();
            
            if (x >= taskX && x <= taskX + TASK_WIDTH &&
                y >= taskY && y <= taskY + TASK_HEIGHT) {
                return task;
            }
        }
        return null;
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        
        // Clear canvas
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw grid
        drawGrid(gc);
        
        // Draw connections
        drawConnections(gc);
        
        // Draw tasks
        drawTasks(gc);
        
        // Draw connection line if connecting
        if (isConnecting && connectionSource != null) {
            drawConnectionLine(gc);
        }
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);
        
        double gridSize = 20;
        for (double x = 0; x < getWidth(); x += gridSize) {
            gc.strokeLine(x, 0, x, getHeight());
        }
        for (double y = 0; y < getHeight(); y += gridSize) {
            gc.strokeLine(0, y, getWidth(), y);
        }
    }

    private void drawConnections(GraphicsContext gc) {
        gc.setStroke(CONNECTION_COLOR);
        gc.setLineWidth(2);
        
        for (TaskConnection connection : workflow.getConnections()) {
            WorkflowTask source = workflow.getTaskById(connection.getSourceTaskId());
            WorkflowTask target = workflow.getTaskById(connection.getTargetTaskId());
            
            if (source != null && target != null) {
                double sourceX = source.getPositionX() + TASK_WIDTH;
                double sourceY = source.getPositionY() + TASK_HEIGHT / 2;
                double targetX = target.getPositionX();
                double targetY = target.getPositionY() + TASK_HEIGHT / 2;
                
                // Draw curved line
                drawCurvedConnection(gc, sourceX, sourceY, targetX, targetY);
                
                // Draw arrow
                drawArrow(gc, sourceX, sourceY, targetX, targetY);
            }
        }
    }

    private void drawCurvedConnection(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        double controlOffset = Math.abs(x2 - x1) * 0.5;
        double cx1 = x1 + controlOffset;
        double cy1 = y1;
        double cx2 = x2 - controlOffset;
        double cy2 = y2;
        
        gc.beginPath();
        gc.moveTo(x1, y1);
        gc.bezierCurveTo(cx1, cy1, cx2, cy2, x2, y2);
        gc.stroke();
    }

    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        double arrowLength = 10;
        double arrowAngle = Math.PI / 6;
        
        double angle = Math.atan2(y2 - y1, x2 - x1);
        
        double x3 = x2 - arrowLength * Math.cos(angle - arrowAngle);
        double y3 = y2 - arrowLength * Math.sin(angle - arrowAngle);
        double x4 = x2 - arrowLength * Math.cos(angle + arrowAngle);
        double y4 = y2 - arrowLength * Math.sin(angle + arrowAngle);
        
        gc.strokeLine(x2, y2, x3, y3);
        gc.strokeLine(x2, y2, x4, y4);
    }

    private void drawTasks(GraphicsContext gc) {
        for (WorkflowTask task : workflow.getTasks()) {
            drawTask(gc, task);
        }
    }

    private void drawTask(GraphicsContext gc, WorkflowTask task) {
        double x = task.getPositionX();
        double y = task.getPositionY();
        
        // Determine colors based on status and selection
        Color fillColor = TASK_COLOR;
        Color strokeColor = Color.DARKBLUE;
        
        if (task.equals(selectedTask)) {
            fillColor = SELECTED_COLOR;
        }
        
        switch (task.getStatus()) {
            case RUNNING:
                fillColor = Color.YELLOW;
                break;
            case COMPLETED:
                fillColor = Color.LIGHTGREEN;
                break;
            case FAILED:
                fillColor = Color.LIGHTCORAL;
                break;
        }
        
        // Draw task rectangle
        gc.setFill(fillColor);
        gc.setStroke(strokeColor);
        gc.setLineWidth(2);
        gc.fillRoundRect(x, y, TASK_WIDTH, TASK_HEIGHT, 10, 10);
        gc.strokeRoundRect(x, y, TASK_WIDTH, TASK_HEIGHT, 10, 10);
        
        // Draw task type icon
        drawTaskTypeIcon(gc, task, x + 5, y + 5);
        
        // Draw task name
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.CENTER);
        
        String taskName = task.getName();
        if (taskName.length() > 15) {
            taskName = taskName.substring(0, 12) + "...";
        }
        
        gc.fillText(taskName, x + TASK_WIDTH / 2, y + TASK_HEIGHT - 10);
        
        // Draw task type
        gc.setFont(Font.font("Arial", 8));
        gc.fillText(task.getType().toString(), x + TASK_WIDTH / 2, y + TASK_HEIGHT - 20);
    }

    private void drawTaskTypeIcon(GraphicsContext gc, WorkflowTask task, double x, double y) {
        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.LEFT);
        
        String icon = "";
        switch (task.getType()) {
            case DEVICE_CONFIG:
                icon = "🖥";
                break;
            case TEMPLATE_CONFIG:
                icon = "📋";
                break;
            case CONDITION:
                icon = "❓";
                break;
            case VARIABLE_SET:
                icon = "💾";
                break;
            case SCRIPT_EXECUTION:
                icon = "⚙";
                break;
            default:
                icon = "⚪";
        }
        
        gc.fillText(icon, x, y + 15);
    }

    private void drawConnectionLine(GraphicsContext gc) {
        if (connectionSource != null) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeLine(
                connectionSource.getPositionX() + TASK_WIDTH,
                connectionSource.getPositionY() + TASK_HEIGHT / 2,
                mouseX,
                mouseY
            );
        }
    }

    public interface WorkflowCanvasListener {
        void onTaskSelected(WorkflowTask task);
        void onTaskDeleted(WorkflowTask task);
        void onConnectionCreated(TaskConnection connection);
        void onEditTask(WorkflowTask task);
        void onAddNewTask(double x, double y);
        void onInsertTaskAfter(WorkflowTask afterTask);
        void onPasteTask(double x, double y);
    }
}