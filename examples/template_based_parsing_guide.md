# Template-Based Output Parsing System Guide

## 🎯 Overview
The Template-Based Output Parsing System allows you to define custom templates that automatically extract specific variables from ANY command output using regex, grep, and other parsing methods.

## ✨ Key Features

### 🔧 **Parsing Types Supported:**
- **REGEX** - Extract using regular expressions
- **GREP** - Extract lines matching patterns (like Linux grep)
- **TABLE** - Parse structured table data
- **KEY_VALUE** - Extract "key: value" pairs
- **LINE_COUNT** - Count lines matching pattern
- **CONTAINS** - Check if output contains text
- **JSON** - Parse JSON output
- **XML** - Parse XML output

### 🎭 **Transformations Available:**
- `uppercase`, `lowercase`, `trim`
- `replace_spaces`, `remove_special`
- `extract_number`

## 🚀 **How It Works**

### 1. **Automatic Template Matching**
```javascript
// System automatically:
// 1. Matches command pattern (e.g., "show vlan" → VLAN parser)
// 2. Matches device type (cisco, generic, etc.)
// 3. Applies parsing rules in order
// 4. Sets variables in workflow execution context
```

### 2. **Variable Extraction Process**
```javascript
Command: "show vlan brief"
Output: 
"100  DATA_VLAN   active   Gi0/1, Gi0/2
 200  VOICE_VLAN  active   Gi0/3"

// Auto-extracted variables:
${vlan_100_name} = "DATA_VLAN"
${vlan_100_status} = "active" 
${total_vlans} = "2"
${active_vlans} = "2"
```

## 📋 **Predefined Templates**

### **1. VLAN Status Parser**
```javascript
// Extracts from "show vlan" commands
${vlan_100_name}     // VLAN 100 name
${vlan_100_status}   // VLAN 100 status
${vlan_200_ports}    // VLAN 200 assigned ports
${total_vlans}       // Total VLAN count
${active_vlans}      // Active VLAN count
```

### **2. Interface Status Parser**
```javascript
// Extracts from "show interface" commands
${interface_name}     // Interface name (e.g., "GigabitEthernet0/1")
${admin_status}       // Administrative status (up/down)
${operational_status} // Line protocol status (up/down)
${ip_address}         // IP address with subnet
${mtu}               // Interface MTU
${bandwidth}         // Interface bandwidth
```

### **3. System Version Parser**
```javascript
// Extracts from "show version" commands
${ios_version}       // IOS version
${device_model}      // Device model (uppercase)
${uptime_days}       // Uptime in days
${serial_number}     // Device serial number
${memory_size}       // Total memory in KB
```

### **4. Routing Table Parser**
```javascript
// Extracts from "show ip route" commands
${default_route_via}    // Default route next hop IP
${connected_networks}   // All connected networks
${static_routes_count}  // Number of static routes
${ospf_routes_count}    // Number of OSPF routes
${has_default_route}    // "true" if default route exists
```

### **5. Ping Test Parser**
```javascript
// Extracts from "ping" commands
${ping_target}         // Target IP address
${packets_sent}        // Number of packets sent
${packets_received}    // Number of packets received
${packet_loss_percent} // Packet loss percentage
${min_rtt}            // Minimum round-trip time
${avg_rtt}            // Average round-trip time
${ping_success}       // "true" if 0% packet loss
```

### **6. Custom Grep Parser**
```javascript
// Works with ANY command output
${error_lines}        // Lines containing error keywords
${success_lines}      // Lines containing success keywords
${ip_addresses}       // All IP addresses found
${interface_names}    // All interface names found
```

## 🎯 **Real-World Examples**

### **Example 1: VLAN Configuration Verification**
```cisco
# Command Output:
VLAN Name                             Status    Ports
---- -------------------------------- --------- -------------------------------
100  DATA_VLAN                        active    Gi0/1, Gi0/2, Gi0/5-10
200  VOICE_VLAN                       active    Gi0/3, Gi0/4

# Auto-Generated Variables:
${vlan_100_name} = "DATA_VLAN"
${vlan_100_status} = "active"
${vlan_200_ports} = "Gi0/3, Gi0/4"  
${total_vlans} = "2"
${active_vlans} = "2"

# Condition Examples:
${vlan_100_status} == "active"
${total_vlans} >= "2"
${active_vlans} == ${total_vlans}
```

### **Example 2: Interface Status Check**
```cisco
# Command Output:
GigabitEthernet0/1 is up, line protocol is up
  Hardware is Gigabit Ethernet, address is 0050.56c0.0001
  Internet address is 192.168.1.10/24
  MTU 1500 bytes, BW 1000000 Kbit/sec

# Auto-Generated Variables:
${interface_name} = "GigabitEthernet0/1"
${admin_status} = "up"
${operational_status} = "up"
${ip_address} = "192.168.1.10/24"
${mtu} = "1500"
${bandwidth} = "1000000"

# Condition Examples:
${admin_status} == "up" AND ${operational_status} == "up"
${mtu} >= "1500"
${ip_address} contains "192.168.1"
```

### **Example 3: Ping Success Verification**
```bash
# Command Output:
PING 8.8.8.8 (8.8.8.8): 56 data bytes
64 bytes from 8.8.8.8: icmp_seq=0 ttl=54 time=12.345 ms
64 bytes from 8.8.8.8: icmp_seq=1 ttl=54 time=11.234 ms

--- 8.8.8.8 ping statistics ---
2 packets transmitted, 2 packets received, 0% packet loss
round-trip min/avg/max = 11.234/11.789/12.345 ms

# Auto-Generated Variables:
${ping_target} = "8.8.8.8"
${packets_sent} = "2"
${packets_received} = "2"
${packet_loss_percent} = "0"
${min_rtt} = "11.234"
${avg_rtt} = "11.789"
${ping_success} = "true"

# Condition Examples:
${ping_success} == "true"
${packet_loss_percent} == "0"
${avg_rtt} < "50"
```

### **Example 4: Routing Table Analysis**
```cisco
# Command Output:
Gateway of last resort is 192.168.1.1 to network 0.0.0.0

S*   0.0.0.0/0 [1/0] via 192.168.1.1
C    192.168.1.0/24 is directly connected, GigabitEthernet0/1
C    10.0.0.0/8 is directly connected, Loopback0
S    172.16.0.0/16 [1/0] via 192.168.1.1

# Auto-Generated Variables:
${default_route_via} = "192.168.1.1"
${connected_networks} = "192.168.1.0/24\n10.0.0.0/8"
${static_routes_count} = "2"
${ospf_routes_count} = "0"
${has_default_route} = "true"

# Condition Examples:
${has_default_route} == "true"
${static_routes_count} >= "1"
${default_route_via} == "192.168.1.1"
```

## 🛠️ **Creating Custom Templates**

### **Template Structure**
```json
{
  "id": "my_custom_parser",
  "name": "My Custom Parser",
  "description": "Parse my specific command output",
  "deviceType": "cisco",  // or "generic", "arista", etc.
  "commandPattern": "show\\s+my\\s+command",  // Regex to match command
  "rules": [
    {
      "variableName": "extracted_value",
      "pattern": "Value:\\s+(\\w+)",  // Regex pattern
      "type": "REGEX",
      "extractionGroup": "1",  // Which regex group to extract
      "description": "Extracted value from output",
      "transform": "uppercase",  // Optional transformation
      "defaultValue": "unknown",  // Default if not found
      "required": false  // Whether variable is required
    }
  ]
}
```

### **Example: Custom BGP Parser**
```json
{
  "id": "bgp_neighbor_parser",
  "name": "BGP Neighbor Parser", 
  "description": "Extract BGP neighbor information",
  "deviceType": "cisco",
  "commandPattern": "show\\s+ip\\s+bgp\\s+summary",
  "rules": [
    {
      "variableName": "bgp_router_id",
      "pattern": "BGP router identifier\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)",
      "type": "REGEX",
      "extractionGroup": "1",
      "description": "BGP Router ID"
    },
    {
      "variableName": "established_neighbors",
      "pattern": "\\d+\\.\\d+\\.\\d+\\.\\d+.*Established",
      "type": "LINE_COUNT",
      "description": "Number of established neighbors"
    }
  ]
}
```

## 🎯 **Workflow Integration**

### **Complete Workflow Example**
```javascript
// Task 1: "Check VLAN Configuration"
// Command: "show vlan brief"
// Auto-creates: ${vlan_100_status}, ${total_vlans}, etc.

// Task 2: "Verify VLAN 100 Active"  
// Condition: ${vlan_100_status} == "active"
// True Action: Continue
// False Action: Stop with error

// Task 3: "Check Interface Status"
// Command: "show interface GigabitEthernet0/1" 
// Auto-creates: ${interface_name}, ${admin_status}, etc.

// Task 4: "Verify Interface Up"
// Condition: ${admin_status} == "up" AND ${operational_status} == "up"
// True Action: Continue
// False Action: Reconfigure interface

// Task 5: "Test Connectivity"
// Command: "ping 8.8.8.8"
// Auto-creates: ${ping_success}, ${packet_loss_percent}, etc.

// Task 6: "Verify Connectivity"
// Condition: ${ping_success} == "true" AND ${packet_loss_percent} == "0"
// True Action: Success!
// False Action: Check routing
```

## 🔧 **API Endpoints**

### **Get All Templates**
```bash
GET /api/output-parsing/templates
```

### **Test Template**
```bash
POST /api/output-parsing/templates/{id}/test
{
  "output": "sample command output here..."
}
```

### **Parse Output**
```bash
POST /api/output-parsing/parse
{
  "command": "show vlan brief",
  "output": "100  DATA_VLAN  active  Gi0/1",
  "deviceType": "cisco"
}
```

## 💡 **Best Practices**

1. **Use Specific Patterns** - More specific regex patterns give better results
2. **Test Extensively** - Test templates with real command output
3. **Use Transformations** - Apply transformations for consistent data
4. **Set Default Values** - Provide defaults for optional variables
5. **Document Patterns** - Add clear descriptions to parsing rules
6. **Device-Specific Templates** - Create device-specific templates for better accuracy
7. **Combine with Generic Success** - Use both template variables and generic success detection

## 🎯 **The Power of Template-Based Parsing**

**Before Templates:**
```javascript
// Manual variable setting required
${output_variable} = "show vlan output"
// Manual parsing in conditions
${output_variable} contains "active"
```

**After Templates:**
```javascript  
// Automatic variable extraction!
${vlan_100_status} == "active"
${total_vlans} >= "2"
${active_vlans} == ${total_vlans}
// No manual parsing needed!
```

**Template-based parsing transforms ANY command output into structured, usable variables automatically! 🎯**