// Main JavaScript for Network Workflow Builder

// Global variables
let stompClient = null;
let isConnected = false;

// Initialize the application
$(document).ready(function() {
    initializeWebSocket();
    setupGlobalEventHandlers();
    loadPredefinedTemplates();
});

// WebSocket connection for real-time updates
function initializeWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket: ' + frame);
        isConnected = true;
        
        // Subscribe to workflow updates
        stompClient.subscribe('/topic/workflow-updates', function(message) {
            handleWorkflowUpdate(JSON.parse(message.body));
        });
        
        // Subscribe to task updates
        stompClient.subscribe('/topic/task-updates', function(message) {
            handleTaskUpdate(JSON.parse(message.body));
        });
        
    }, function(error) {
        console.log('WebSocket connection error: ' + error);
        isConnected = false;
        
        // Retry connection after 5 seconds
        setTimeout(initializeWebSocket, 5000);
    });
}

// Handle real-time workflow updates
function handleWorkflowUpdate(update) {
    console.log('Workflow update received:', update);
    
    // Update UI based on the workflow update
    if (update.type === 'STATUS_CHANGE') {
        updateWorkflowStatus(update.workflowId, update.status);
    } else if (update.type === 'EXECUTION_COMPLETE') {
        showNotification('Workflow execution completed: ' + update.workflowName, 'success');
    } else if (update.type === 'EXECUTION_FAILED') {
        showNotification('Workflow execution failed: ' + update.workflowName, 'danger');
    }
}

// Handle real-time task updates
function handleTaskUpdate(update) {
    console.log('Task update received:', update);
    
    // Update task status in UI
    if (update.type === 'STATUS_CHANGE') {
        updateTaskStatus(update.taskId, update.status);
    }
}

// Setup global event handlers
function setupGlobalEventHandlers() {
    // Handle navigation
    $(document).on('click', 'a[href^="/"]', function(e) {
        const href = $(this).attr('href');
        
        // Don't intercept external links or special links
        if (href.startsWith('http') || href.includes('#') || $(this).hasClass('no-ajax')) {
            return;
        }
        
        // Add loading state
        showPageLoading();
    });
    
    // Handle form submissions with loading states
    $(document).on('submit', 'form', function() {
        const submitBtn = $(this).find('button[type="submit"]');
        if (submitBtn.length > 0) {
            const originalText = submitBtn.html();
            submitBtn.html('<i class="fas fa-spinner fa-spin me-1"></i>Processing...').prop('disabled', true);
            
            // Restore button after 5 seconds as failsafe
            setTimeout(function() {
                submitBtn.html(originalText).prop('disabled', false);
            }, 5000);
        }
    });
    
    // Handle AJAX errors globally
    $(document).ajaxError(function(event, xhr, settings, thrownError) {
        console.error('AJAX Error:', {
            url: settings.url,
            status: xhr.status,
            error: thrownError
        });
        
        if (xhr.status === 401) {
            showNotification('Session expired. Please refresh the page.', 'warning');
        } else if (xhr.status >= 500) {
            showNotification('Server error occurred. Please try again.', 'danger');
        }
    });
}

// Load predefined templates on startup
function loadPredefinedTemplates() {
    $.post('/api/templates/load-predefined')
        .done(function(response) {
            console.log('Predefined templates loaded');
        })
        .fail(function() {
            console.warn('Failed to load predefined templates');
        });
}

// Show page loading indicator
function showPageLoading() {
    if ($('#page-loading').length === 0) {
        $('body').append(`
            <div id="page-loading" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" 
                 style="background: rgba(255,255,255,0.8); z-index: 9999;">
                <div class="text-center">
                    <div class="spinner-border text-primary" role="status"></div>
                    <div class="mt-2">Loading...</div>
                </div>
            </div>
        `);
    }
}

// Hide page loading indicator
function hidePageLoading() {
    $('#page-loading').remove();
}

// Show notification/toast
function showNotification(message, type = 'info', duration = 5000) {
    const toastHtml = `
        <div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;
    
    // Create toast container if it doesn't exist
    if ($('#toast-container').length === 0) {
        $('body').append(`
            <div id="toast-container" class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 11000;"></div>
        `);
    }
    
    const $toast = $(toastHtml);
    $('#toast-container').append($toast);
    
    const toast = new bootstrap.Toast($toast[0], { delay: duration });
    toast.show();
    
    // Remove toast element after it's hidden
    $toast.on('hidden.bs.toast', function() {
        $(this).remove();
    });
}

// Update workflow status in UI
function updateWorkflowStatus(workflowId, status) {
    $(`.workflow-status[data-workflow-id="${workflowId}"]`).each(function() {
        const $badge = $(this);
        $badge.removeClass('bg-secondary bg-primary bg-warning bg-success bg-danger bg-info');
        $badge.addClass('bg-' + getStatusColor(status));
        $badge.text(status);
    });
}

// Update task status in UI
function updateTaskStatus(taskId, status) {
    const $taskNode = $(`#task-${taskId}`);
    if ($taskNode.length > 0) {
        // Update task node classes
        $taskNode.removeClass('task-status-PENDING task-status-RUNNING task-status-COMPLETED task-status-FAILED');
        $taskNode.addClass('task-status-' + status);
        
        // Update status badge if present
        $taskNode.find('.task-status-badge').each(function() {
            const $badge = $(this);
            $badge.removeClass('bg-secondary bg-primary bg-warning bg-success bg-danger bg-info');
            $badge.addClass('bg-' + getStatusColor(status));
            $badge.text(status);
        });
    }
}

// Get Bootstrap color class for status
function getStatusColor(status) {
    switch(status) {
        case 'DRAFT': return 'secondary';
        case 'READY': return 'primary';
        case 'RUNNING': return 'warning';
        case 'COMPLETED': return 'success';
        case 'FAILED': return 'danger';
        case 'PAUSED': return 'info';
        default: return 'secondary';
    }
}

// Format date for display
function formatDate(dateString) {
    if (!dateString) return 'Unknown';
    const date = new Date(dateString);
    return date.toLocaleString();
}

// Format relative time
function formatRelativeTime(dateString) {
    if (!dateString) return 'Unknown';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minutes ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays < 7) return `${diffDays} days ago`;
    return formatDate(dateString);
}

// Escape HTML to prevent XSS
function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Debounce function for search inputs
function debounce(func, wait, immediate) {
    let timeout;
    return function executedFunction() {
        const context = this;
        const args = arguments;
        const later = function() {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };
        const callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
}

// Copy text to clipboard
function copyToClipboard(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(function() {
            showNotification('Copied to clipboard', 'success', 2000);
        });
    } else {
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showNotification('Copied to clipboard', 'success', 2000);
    }
}

// Theme toggle functionality
function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    
    showNotification(`Switched to ${newTheme} theme`, 'info', 2000);
}

// Load saved theme
function loadSavedTheme() {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        document.documentElement.setAttribute('data-theme', 'dark');
    }
}

// Initialize theme on page load
loadSavedTheme();

// Export functions for global use
window.NetworkWorkflow = {
    showNotification,
    updateWorkflowStatus,
    updateTaskStatus,
    getStatusColor,
    formatDate,
    formatRelativeTime,
    escapeHtml,
    debounce,
    copyToClipboard,
    toggleTheme,
    showPageLoading,
    hidePageLoading
};

// Handle page visibility changes
document.addEventListener('visibilitychange', function() {
    if (document.hidden) {
        console.log('Page hidden - pausing updates');
    } else {
        console.log('Page visible - resuming updates');
        // Reconnect WebSocket if needed
        if (!isConnected) {
            initializeWebSocket();
        }
    }
});

// Handle connection status
window.addEventListener('online', function() {
    showNotification('Connection restored', 'success', 2000);
    if (!isConnected) {
        initializeWebSocket();
    }
});

window.addEventListener('offline', function() {
    showNotification('Connection lost - working offline', 'warning');
});

console.log('Network Workflow Builder initialized successfully');