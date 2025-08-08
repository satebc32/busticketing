# Python Configuration for Network Workflow Builder

This document explains how the Network Workflow Builder automatically detects and configures Python executables for Netmiko integration.

## 🔍 **Automatic Python Detection**

The application automatically detects the available Python executable on your system by trying:

1. **`python3`** (preferred for most Linux/Unix systems)
2. **`python`** (fallback for Windows or systems where python3 is not available)

### Detection Process:
- Tests each candidate by running `{executable} --version`
- Validates the output contains "python" to ensure it's a valid Python interpreter
- Uses the first working executable found
- Logs the detected version for verification

## ⚙️ **Configuration Options**

### Auto-Detection (Default)
```yaml
python:
  executable: auto  # Automatically detect python3 or python
  script-path: python/netmiko_executor.py
  timeout: 60
```

### Manual Configuration
```yaml
python:
  executable: python3                    # Specific executable
  script-path: python/netmiko_executor.py
  timeout: 60
```

### Platform-Specific Examples

#### Linux/Unix Systems:
```yaml
python:
  executable: /usr/bin/python3
  # or
  executable: /usr/local/bin/python3.9
```

#### Windows Systems:
```yaml
python:
  executable: python
  # or full path
  executable: C:\Python39\python.exe
```

#### macOS (Homebrew):
```yaml
python:
  executable: /opt/homebrew/bin/python3
```

## 🚀 **Benefits of Auto-Detection**

### ✅ **Cross-Platform Compatibility**
- Works on Linux, Windows, macOS without configuration
- Adapts to different Python installation methods
- No manual path configuration required

### ✅ **Fallback Mechanism**
- If configured executable fails, falls back to auto-detection
- Prevents application startup failures due to Python path issues
- Provides clear logging for troubleshooting

### ✅ **Development & Deployment**
- Works in development environments (various Python versions)
- Compatible with Docker containers
- Supports CI/CD pipelines with different Python setups

## 🔧 **Manual Override**

If you need to use a specific Python version or path:

```yaml
python:
  executable: /path/to/specific/python
```

The application will:
1. Validate the specified executable
2. Fall back to auto-detection if validation fails
3. Log the detection/validation process

## 📋 **Verification**

Check the application logs during startup to see which Python executable was detected:

```
INFO  c.n.service.NetmikoService - Detected Python executable: python3 (Python 3.9.7)
INFO  c.n.service.NetmikoService - Using Python executable: python3 (Script: python/netmiko_executor.py)
```

## 🛠️ **Troubleshooting**

### Issue: No Python detected
**Solution**: Ensure Python is installed and available in your system PATH
```bash
# Test manually
python3 --version
python --version
```

### Issue: Wrong Python version
**Solution**: Specify the correct executable in `application.yml`
```yaml
python:
  executable: /usr/bin/python3.9
```

### Issue: Permission denied
**Solution**: Ensure the Python executable has proper permissions
```bash
chmod +x /path/to/python
```

## 🌟 **Best Practices**

1. **Use Auto-Detection**: Let the application find Python automatically unless you have specific requirements
2. **Verify Installation**: Check startup logs to confirm the correct Python version is detected
3. **Environment Consistency**: Use the same Python version across development, testing, and production
4. **Virtual Environments**: The application works with Python installed in virtual environments if activated
5. **Docker Support**: Auto-detection works well in Docker containers with Python pre-installed

## 📚 **Related Documentation**

- [Netmiko Integration Guide](netmiko_integration.md)
- [Workflow Execution](workflow_execution.md)
- [Template Configuration](template_configuration.md)