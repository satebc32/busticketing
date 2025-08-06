import java.util.Scanner;

class palindrome {
    
    // Method to check if a string is a palindrome (case-sensitive)
    public static boolean isPalindrome(String str) {
        if (str == null || str.length() == 0) {
            return true; // Empty string is considered a palindrome
        }
        
        int left = 0;
        int right = str.length() - 1;
        
        while (left < right) {
            if (str.charAt(left) != str.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }
    
    // Method to check if a string is a palindrome (case-insensitive)
    public static boolean isPalindromeIgnoreCase(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        
        str = str.toLowerCase();
        return isPalindrome(str);
    }
    
    // Method to check if a string is a palindrome ignoring spaces and punctuation
    public static boolean isPalindromeAlphanumeric(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        
        // Remove non-alphanumeric characters and convert to lowercase
        StringBuilder cleanStr = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleanStr.append(Character.toLowerCase(c));
            }
        }
        
        return isPalindrome(cleanStr.toString());
    }
    
    // Method to check if a number is a palindrome
    public static boolean isPalindromeNumber(int num) {
        if (num < 0) {
            return false; // Negative numbers are not palindromes
        }
        
        int original = num;
        int reversed = 0;
        
        while (num > 0) {
            reversed = reversed * 10 + num % 10;
            num /= 10;
        }
        
        return original == reversed;
    }
    
    // Method to reverse a string
    public static String reverseString(String str) {
        if (str == null) {
            return null;
        }
        
        StringBuilder reversed = new StringBuilder();
        for (int i = str.length() - 1; i >= 0; i--) {
            reversed.append(str.charAt(i));
        }
        return reversed.toString();
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Palindrome Checker ===");
        System.out.println();
        
        // Test with predefined examples
        System.out.println("Testing predefined examples:");
        
        String[] testStrings = {
            "racecar",
            "hello",
            "A man a plan a canal Panama",
            "race a car",
            "Madam",
            "level",
            "12321",
            "12345"
        };
        
        for (String test : testStrings) {
            System.out.println("String: \"" + test + "\"");
            System.out.println("  Case-sensitive: " + isPalindrome(test));
            System.out.println("  Case-insensitive: " + isPalindromeIgnoreCase(test));
            System.out.println("  Alphanumeric only: " + isPalindromeAlphanumeric(test));
            System.out.println();
        }
        
        // Test numbers
        System.out.println("Testing numbers:");
        int[] testNumbers = {12321, 12345, 1001, -121, 7};
        for (int num : testNumbers) {
            System.out.println("Number: " + num + " -> " + isPalindromeNumber(num));
        }
        System.out.println();
        
        // Interactive mode
        System.out.println("=== Interactive Mode ===");
        System.out.println("Enter strings to check if they are palindromes (type 'exit' to quit):");
        
        while (true) {
            System.out.print("Enter a string: ");
            String input = scanner.nextLine();
            
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
            
            System.out.println("Original: \"" + input + "\"");
            System.out.println("Reversed: \"" + reverseString(input) + "\"");
            System.out.println("Is palindrome (case-sensitive): " + isPalindrome(input));
            System.out.println("Is palindrome (case-insensitive): " + isPalindromeIgnoreCase(input));
            System.out.println("Is palindrome (alphanumeric only): " + isPalindromeAlphanumeric(input));
            
            // Try to parse as number
            try {
                int num = Integer.parseInt(input.trim());
                System.out.println("As number: " + isPalindromeNumber(num));
            } catch (NumberFormatException e) {
                System.out.println("Not a valid number");
            }
            
            System.out.println("---");
        }
        
        System.out.println("Thank you for using the Palindrome Checker!");
        scanner.close();
    }
}
