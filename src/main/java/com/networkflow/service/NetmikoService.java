package com.networkflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for executing device configurations using Netmiko Python library
 */
@Service
public class NetmikoService {
    private static final Logger logger = LoggerFactory.getLogger(NetmikoService.class);
    private final ObjectMapper objectMapper;
    private final String pythonExecutable;
    private final String netmikoScriptPath;

    public NetmikoService() {
        this.objectMapper = new ObjectMapper();
        this.pythonExecutable = "python3"; // Can be configured
        this.netmikoScriptPath = "python/netmiko_executor.py";
    }

    /**
     * Execute device configuration using Netmiko
     */
    public NetmikoResult executeDeviceConfig(DeviceConnection deviceConnection, 
                                           String configCommands, 
                                           int timeoutSeconds) {
        try {
            // Create temporary files for input and output
            Path inputFile = createTempConfigFile(deviceConnection, configCommands);
            Path outputFile = Paths.get(inputFile.toString().replace(".json", "_output.json"));

            // Execute Python script
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable, 
                netmikoScriptPath, 
                inputFile.toString(),
                outputFile.toString()
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Wait for completion with timeout
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return new NetmikoResult(false, "Execution timeout after " + timeoutSeconds + " seconds", null);
            }

            // Read output
            String output = readProcessOutput(process);
            NetmikoResult result = parseNetmikoOutput(outputFile, output);

            // Cleanup temp files
            Files.deleteIfExists(inputFile);
            Files.deleteIfExists(outputFile);

            return result;

        } catch (Exception e) {
            logger.error("Error executing device configuration", e);
            return new NetmikoResult(false, "Error: " + e.getMessage(), null);
        }
    }

    /**
     * Test device connectivity
     */
    public NetmikoResult testConnection(DeviceConnection deviceConnection) {
        Map<String, Object> testConfig = new HashMap<>();
        testConfig.put("device", deviceConnection.toMap());
        testConfig.put("commands", new String[]{"show version"});
        testConfig.put("test_only", true);

        try {
            Path inputFile = Files.createTempFile("netmiko_test_", ".json");
            objectMapper.writeValue(inputFile.toFile(), testConfig);

            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable, 
                netmikoScriptPath, 
                inputFile.toString()
            );
            
            Process process = processBuilder.start();
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return new NetmikoResult(false, "Connection test timeout", null);
            }

            String output = readProcessOutput(process);
            Files.deleteIfExists(inputFile);

            return new NetmikoResult(process.exitValue() == 0, output, null);

        } catch (Exception e) {
            logger.error("Error testing device connection", e);
            return new NetmikoResult(false, "Connection test error: " + e.getMessage(), null);
        }
    }

    private Path createTempConfigFile(DeviceConnection deviceConnection, String configCommands) throws IOException {
        Map<String, Object> config = new HashMap<>();
        config.put("device", deviceConnection.toMap());
        config.put("commands", configCommands.split("\\r?\\n"));
        config.put("test_only", false);

        Path tempFile = Files.createTempFile("netmiko_config_", ".json");
        objectMapper.writeValue(tempFile.toFile(), config);
        return tempFile;
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    private NetmikoResult parseNetmikoOutput(Path outputFile, String processOutput) {
        try {
            if (Files.exists(outputFile)) {
                Map<String, Object> result = objectMapper.readValue(outputFile.toFile(), Map.class);
                boolean success = (Boolean) result.getOrDefault("success", false);
                String message = (String) result.getOrDefault("message", "");
                String output = (String) result.getOrDefault("output", "");
                
                return new NetmikoResult(success, message, output);
            } else {
                return new NetmikoResult(false, processOutput, null);
            }
        } catch (Exception e) {
            logger.error("Error parsing Netmiko output", e);
            return new NetmikoResult(false, "Error parsing output: " + e.getMessage(), processOutput);
        }
    }

    /**
     * Device connection configuration
     */
    public static class DeviceConnection {
        private String deviceType;
        private String host;
        private String username;
        private String password;
        private String secret;
        private int port;
        private int timeout;

        public DeviceConnection(String deviceType, String host, String username, String password) {
            this.deviceType = deviceType;
            this.host = host;
            this.username = username;
            this.password = password;
            this.port = 22; // Default SSH port
            this.timeout = 20; // Default timeout
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("device_type", deviceType);
            map.put("host", host);
            map.put("username", username);
            map.put("password", password);
            if (secret != null) map.put("secret", secret);
            map.put("port", port);
            map.put("timeout", timeout);
            return map;
        }

        // Getters and Setters
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }

    /**
     * Result of Netmiko execution
     */
    public static class NetmikoResult {
        private final boolean success;
        private final String message;
        private final String output;

        public NetmikoResult(boolean success, String message, String output) {
            this.success = success;
            this.message = message;
            this.output = output;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getOutput() { return output; }

        @Override
        public String toString() {
            return "NetmikoResult{success=" + success + ", message='" + message + "'}";
        }
    }
}