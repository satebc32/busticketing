# Generic Output Success Verification Examples

## 🎯 Overview
The system automatically analyzes command output and determines success/failure using intelligent pattern matching and scoring algorithms.

## 📊 Auto-Generated Variables

### Generic Variables (Always Available)
```javascript
${generic_status}        // "success" or "failed" - Universal success detection
${command_status}        // "success" or "failed" - Alias for generic_status
${success_confidence}    // Numeric score (0-10+) - Success indicator count
${failure_confidence}    // Numeric score (0-10+) - Failure indicator count
```

### Specialized Variables (Context-Aware)
```javascript
${vlan_status}          // VLAN operations
${interface_status}     // Interface configurations
${ping_status}          // Connectivity tests
${route_status}         // Routing operations
${save_status}          // Configuration saves
${show_status}          // Show commands
${verification_status}  // Verification tasks
```

## 🔍 Success Detection Examples

### VLAN Configuration Success
```cisco
# ✅ SUCCESS - Contains "VLAN 100" + "active"
VLAN Name                             Status    Ports
---- -------------------------------- --------- -------------------------------
100  DATA_VLAN                        active    Gi0/1

# Variables Set:
# ${vlan_status} = "success"
# ${vlan_100_status} = "active"
# ${generic_status} = "success"
# ${success_confidence} = 3
```

### Interface Configuration Success
```cisco
# ✅ SUCCESS - Contains "up/up" status
GigabitEthernet0/1 is up, line protocol is up
  Hardware is Gigabit Ethernet, address is 0050.56c0.0001
  Internet address is 192.168.1.10/24

# Variables Set:
# ${interface_status} = "success"
# ${generic_status} = "success"
# ${success_confidence} = 4
```

### Ping Test Success
```cisco
# ✅ SUCCESS - Contains reply information
PING 192.168.1.1 (192.168.1.1): 56 data bytes
64 bytes from 192.168.1.1: icmp_seq=0 ttl=64 time=1.234 ms
64 bytes from 192.168.1.1: icmp_seq=1 ttl=64 time=1.456 ms

--- 192.168.1.1 ping statistics ---
2 packets transmitted, 2 packets received, 0% packet loss

# Variables Set:
# ${ping_status} = "success"
# ${generic_status} = "success"
# ${success_confidence} = 5
```

### Configuration Save Success
```cisco
# ✅ SUCCESS - Contains "Building configuration"
Building configuration...
[OK]
Saved configuration to startup-config

# Variables Set:
# ${save_status} = "success"
# ${generic_status} = "success"
# ${success_confidence} = 3
```

## ❌ Failure Detection Examples

### VLAN Configuration Failure
```cisco
# ❌ FAILURE - Contains error message
%Error: VLAN 100 not found in VLAN database
%Configuration failed

# Variables Set:
# ${vlan_status} = "failed"
# ${generic_status} = "failed"
# ${failure_confidence} = 3
```

### Authentication Failure
```cisco
# ❌ FAILURE - Authentication error
% Authentication failed
Login incorrect

# Variables Set:
# ${generic_status} = "failed"
# ${failure_confidence} = 4
```

### Command Error
```cisco
# ❌ FAILURE - Invalid command
% Invalid input detected at '^' marker.
% Unknown command or computer name, or unable to find computer address

# Variables Set:
# ${generic_status} = "failed"
# ${failure_confidence} = 3
```

## 🎯 Condition Examples

### Basic Generic Success Check
```javascript
// Works for any command type
${generic_status} == "success"
```

### Specialized Success Checks
```javascript
// VLAN operations
${vlan_status} == "success"

// Interface operations  
${interface_status} == "success"

// Connectivity tests
${ping_status} == "success"

// Configuration saves
${save_status} == "success"
```

### Confidence-Based Conditions
```javascript
// High confidence success (score >= 3)
${success_confidence} >= 3

// No failure indicators
${failure_confidence} == 0

// Success significantly outweighs failures
${success_confidence} > ${failure_confidence}
```

### Complex Logic
```javascript
// VLAN success with high confidence
${vlan_status} == "success" AND ${success_confidence} >= 2

// Generic success but no failures
${generic_status} == "success" AND ${failure_confidence} == 0
```

## 🛠️ Workflow Patterns

### Pattern 1: Universal Success Check
```javascript
Task 1: [Any Configuration Command]
Task 2: [Condition Check] → ${generic_status} == "success"
```

### Pattern 2: Specialized with Fallback
```javascript
Task 1: [VLAN Configuration]
Task 2: [Condition Check] → ${vlan_status} == "success" OR ${generic_status} == "success"
```

### Pattern 3: Confidence-Based Decision
```javascript
Task 1: [Complex Configuration]
Task 2: [Condition Check] → ${success_confidence} >= 3 AND ${failure_confidence} == 0
```

## 📋 Supported Command Types

### Automatically Detected Contexts
- **VLAN**: `vlan`, `switchport access vlan`
- **Interface**: `interface`, `line protocol`, `switchport mode`
- **Ping**: `ping`, `reply from`, `packet loss`
- **Routing**: `route`, `connected via`, `ip route`
- **Save**: `save`, `write`, `copy`, `building configuration`
- **Show**: `show`, `display` (structured data detection)
- **Verification**: `verify`, `check` (error-free validation)

### Generic Patterns Work For
- Device configurations
- Template applications
- System commands
- Custom scripts
- Any network operation
- Multi-vendor equipment

## 💡 Best Practices

1. **Use Generic Variables** for universal compatibility
2. **Use Specialized Variables** for specific operations
3. **Check Confidence Scores** for complex scenarios
4. **Combine Multiple Conditions** for robust validation
5. **Test with Real Output** to verify pattern matching

The generic success verification system provides intelligent, automatic success detection for any network operation! 🎯