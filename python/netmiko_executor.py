#!/usr/bin/env python3
"""
Netmiko executor script for device configuration
Executes network device commands using the Netmiko library
"""

import json
import sys
import logging
from datetime import datetime
from netmiko import ConnectHandler
from netmiko.exceptions import NetMikoTimeoutException, NetMikoAuthenticationException

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def execute_netmiko_commands(config_file, output_file=None):
    """
    Execute Netmiko commands based on configuration file
    
    Args:
        config_file (str): Path to JSON configuration file
        output_file (str): Optional path to output file for results
    
    Returns:
        dict: Execution result
    """
    try:
        # Load configuration
        with open(config_file, 'r') as f:
            config = json.load(f)
        
        device_config = config['device']
        commands = config['commands']
        test_only = config.get('test_only', False)
        
        result = {
            'success': False,
            'message': '',
            'output': '',
            'timestamp': datetime.now().isoformat(),
            'device_host': device_config.get('host', 'unknown')
        }
        
        # Connect to device
        logger.info(f"Connecting to device {device_config['host']}")
        
        try:
            net_connect = ConnectHandler(**device_config)
            logger.info("Successfully connected to device")
            
            if test_only:
                # Just test connectivity
                result['success'] = True
                result['message'] = 'Connection test successful'
                result['output'] = 'Connection established successfully'
            else:
                # Execute commands
                output_lines = []
                
                for command in commands:
                    if command.strip():
                        logger.info(f"Executing command: {command}")
                        
                        if command.strip().startswith('configure') or command.strip().startswith('config'):
                            # Configuration command
                            cmd_output = net_connect.send_config_set([command])
                        else:
                            # Show command
                            cmd_output = net_connect.send_command(command)
                        
                        output_lines.append(f"Command: {command}")
                        output_lines.append(cmd_output)
                        output_lines.append("-" * 50)
                
                # Save configuration if we made changes
                if any(cmd.strip().startswith(('configure', 'config')) for cmd in commands):
                    save_output = net_connect.save_config()
                    output_lines.append("Configuration saved:")
                    output_lines.append(save_output)
                
                result['success'] = True
                result['message'] = f'Successfully executed {len(commands)} commands'
                result['output'] = '\n'.join(output_lines)
            
            # Disconnect
            net_connect.disconnect()
            logger.info("Disconnected from device")
            
        except NetMikoAuthenticationException as e:
            result['message'] = f'Authentication failed: {str(e)}'
            logger.error(result['message'])
            
        except NetMikoTimeoutException as e:
            result['message'] = f'Connection timeout: {str(e)}'
            logger.error(result['message'])
            
        except Exception as e:
            result['message'] = f'Device connection error: {str(e)}'
            logger.error(result['message'])
        
        # Write output file if specified
        if output_file:
            with open(output_file, 'w') as f:
                json.dump(result, f, indent=2)
        
        # Print result to stdout
        print(json.dumps(result, indent=2))
        
        return result
        
    except FileNotFoundError:
        error_result = {
            'success': False,
            'message': f'Configuration file not found: {config_file}',
            'output': '',
            'timestamp': datetime.now().isoformat()
        }
        print(json.dumps(error_result, indent=2))
        return error_result
        
    except json.JSONDecodeError as e:
        error_result = {
            'success': False,
            'message': f'Invalid JSON in configuration file: {str(e)}',
            'output': '',
            'timestamp': datetime.now().isoformat()
        }
        print(json.dumps(error_result, indent=2))
        return error_result
        
    except Exception as e:
        error_result = {
            'success': False,
            'message': f'Unexpected error: {str(e)}',
            'output': '',
            'timestamp': datetime.now().isoformat()
        }
        print(json.dumps(error_result, indent=2))
        return error_result

def validate_device_config(device_config):
    """
    Validate device configuration
    
    Args:
        device_config (dict): Device configuration
        
    Returns:
        tuple: (is_valid, error_message)
    """
    required_fields = ['device_type', 'host', 'username', 'password']
    
    for field in required_fields:
        if field not in device_config:
            return False, f"Missing required field: {field}"
    
    # Validate device type
    supported_types = [
        'cisco_ios', 'cisco_xe', 'cisco_nxos', 'cisco_asa',
        'arista_eos', 'juniper_junos', 'hp_comware', 'huawei',
        'fortinet', 'paloalto_panos', 'linux'
    ]
    
    if device_config['device_type'] not in supported_types:
        return False, f"Unsupported device type: {device_config['device_type']}"
    
    return True, ""

def main():
    """Main execution function"""
    if len(sys.argv) < 2:
        print("Usage: python3 netmiko_executor.py <config_file> [output_file]")
        sys.exit(1)
    
    config_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    try:
        # Load and validate config
        with open(config_file, 'r') as f:
            config = json.load(f)
        
        device_config = config.get('device', {})
        is_valid, error_msg = validate_device_config(device_config)
        
        if not is_valid:
            error_result = {
                'success': False,
                'message': f'Invalid device configuration: {error_msg}',
                'output': '',
                'timestamp': datetime.now().isoformat()
            }
            print(json.dumps(error_result, indent=2))
            sys.exit(1)
        
        # Execute commands
        result = execute_netmiko_commands(config_file, output_file)
        
        # Exit with appropriate code
        sys.exit(0 if result['success'] else 1)
        
    except Exception as e:
        error_result = {
            'success': False,
            'message': f'Script execution error: {str(e)}',
            'output': '',
            'timestamp': datetime.now().isoformat()
        }
        print(json.dumps(error_result, indent=2))
        sys.exit(1)

if __name__ == "__main__":
    main()