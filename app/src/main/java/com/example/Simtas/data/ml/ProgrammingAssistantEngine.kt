package com.example.Simtas.data.ml

import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import java.util.Locale

/**
 * Dedicated Offline Programming Assistant Engine.
 * Supports:
 * 1. Generating idiomatic code examples across Java, Kotlin, Python, C, C++, and SQL.
 * 2. Explaining code logic and syntax.
 * 3. Identifying and explaining common programming bugs and errors (NullPointerException,
 *    IndexOutOfBoundsException, Segmentation Fault, StackOverflowError, etc.) with verified fixes.
 *
 * 100% offline, zero cloud API reliance.
 */
object ProgrammingAssistantEngine {

    data class CodeExample(
        val title: String,
        val language: String,
        val description: String,
        val code: String,
        val explanation: String
    )

    data class ErrorTroubleshooting(
        val errorName: String,
        val languages: List<String>,
        val cause: String,
        val fix: String,
        val exampleFix: String
    )

    private val CODE_EXAMPLES = listOf(
        CodeExample(
            title = "Binary Search in Python",
            language = "Python",
            description = "Searches for a target value in a sorted list in O(log n) time.",
            code = """
def binary_search(arr, target):
    left, right = 0, len(arr) - 1
    while left <= right:
        mid = (left + right) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    return -1  # Target not found

# Example usage:
numbers = [2, 5, 8, 12, 16, 23, 38, 56, 72, 91]
print(binary_search(numbers, 23))  # Output: 5
""".trimIndent(),
            explanation = "Divide-and-conquer algorithm. Requires the input list to be pre-sorted. Each step halves the remaining search range."
        ),
        CodeExample(
            title = "Reverse a String in Python",
            language = "Python",
            description = "Reverses a string using slicing or the reversed() function.",
            code = """
# Method 1: String Slicing (Most Pythonic and fastest)
text = "SmartNotes AI"
reversed_text = text[::-1]
print(reversed_text)  # Output: "IA setonrtamS"

# Method 2: Using reversed() and join()
reversed_alt = "".join(reversed(text))
print(reversed_alt)
""".trimIndent(),
            explanation = "String slicing [::-1] uses a step of -1 to iterate from the last character to the first in O(n) time."
        ),
        CodeExample(
            title = "Java Class and Object Example",
            language = "Java",
            description = "Demonstrates encapsulation, constructors, and methods in Java.",
            code = """
public class Student {
    private int id;
    private String name;

    // Constructor
    public Student(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Method
    public void display() {
        System.out.println("ID: " + id + ", Name: " + name);
    }

    public static void main(String[] args) {
        Student s1 = new Student(101, "Alice");
        s1.display();
    }
}
""".trimIndent(),
            explanation = "Demonstrates clean object creation in Java. Fields are marked private for encapsulation and accessed via public methods."
        ),
        CodeExample(
            title = "Kotlin Data Class and Coroutine",
            language = "Kotlin",
            description = "Shows Kotlin data class with copy() and asynchronous coroutine launch.",
            code = """
import kotlinx.coroutines.*

data class User(val id: Int, val username: String)

fun main() = runBlocking {
    val user1 = User(1, "alex_dev")
    val user2 = user1.copy(id = 2) // Automatic copy with modified property

    launch {
        delay(1000L)
        println("Coroutine finished! User: ${'$'}user2")
    }
    println("Main thread continues...")
}
""".trimIndent(),
            explanation = "Kotlin data classes automatically generate equals(), hashCode(), toString(), and copy(). Coroutines provide non-blocking asynchronous execution."
        ),
        CodeExample(
            title = "Pointers in C",
            language = "C",
            description = "Demonstrates pointer declaration, dereferencing, and address manipulation.",
            code = """
#include <stdio.h>

int main() {
    int num = 42;
    int *ptr = &num; // Pointer storing memory address of num
    printf("Value of num: %d\n", num);
    printf("Address of num (&num): %p\n", (void*)&num);
    printf("Pointer ptr value: %p\n", (void*)ptr);
    printf("Dereferenced *ptr: %d\n", *ptr);
    return 0;
}
""".trimIndent(),
            explanation = "In C, a pointer variable stores the memory address of another variable. The address-of operator (&) gets the address, and the dereference operator (*) accesses the value."
        ),
        CodeExample(
            title = "C++ Vector and STL Algorithm",
            language = "C++",
            description = "Demonstrates dynamic arrays using std::vector and std::sort.",
            code = """
#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> scores = {85, 92, 78, 99, 88};
    scores.push_back(95); // Dynamic append

    std::sort(scores.begin(), scores.end()); // O(n log n) Introsort

    std::cout << "Sorted scores: ";
    for (int score : scores) {
        std::cout << score << " ";
    }
    std::cout << std::endl;
    return 0;
}
""".trimIndent(),
            explanation = "std::vector handles dynamic memory automatically (RAII) and supports random access in O(1) time."
        ),
        CodeExample(
            title = "SQL Second Highest Salary",
            language = "SQL",
            description = "Classic interview SQL query to find the second highest salary from an Employee table.",
            code = """
-- Method 1: Using Subquery with MAX
SELECT MAX(salary) AS SecondHighestSalary
FROM Employee
WHERE salary < (SELECT MAX(salary) FROM Employee);

-- Method 2: Using LIMIT and OFFSET (MySQL/PostgreSQL)
SELECT DISTINCT salary
FROM Employee
ORDER BY salary DESC
LIMIT 1 OFFSET 1;
""".trimIndent(),
            explanation = "Method 1 finds the highest salary that is strictly less than the maximum salary, handling duplicates cleanly."
        )
    )

    private val ERROR_GUIDES = listOf(
        ErrorTroubleshooting(
            errorName = "NullPointerException",
            languages = listOf("Java", "Kotlin", "Android"),
            cause = "Occurs when an application attempts to access a method or field on an object reference that points to null (uninitialized or de-allocated).",
            fix = "1. Always check for null before accessing members (if (obj != null)).\n2. Use Java Optional<T>.\n3. In Kotlin, use safe-call operators (obj?.doSomething()) or the Elvis operator (val name = obj?.name ?: \"Default\").",
            exampleFix = """
// Java Fix:
if (student != null) {
    System.out.println(student.getName());
}

// Kotlin Fix:
val length = text?.length ?: 0
""".trimIndent()
        ),
        ErrorTroubleshooting(
            errorName = "IndexOutOfBoundsException / IndexError",
            languages = listOf("Java", "Python", "Kotlin", "C++"),
            cause = "Attempting to access an array, list, or string index that is either negative or greater than or equal to the size of the collection.",
            fix = "Always verify the index is within bounds: 0 <= index < collection.size.",
            exampleFix = """
// Python Fix:
if 0 <= index < len(my_list):
    print(my_list[index])

// Java / Kotlin Fix:
if (index >= 0 && index < list.size()) {
    return list.get(index);
}
""".trimIndent()
        ),
        ErrorTroubleshooting(
            errorName = "Segmentation Fault",
            languages = listOf("C", "C++"),
            cause = "Occurs when a program attempts to access a restricted memory address (e.g. dereferencing a NULL pointer, accessing freed memory, or writing past buffer boundaries).",
            fix = "1. Initialize all pointers to NULL.\n2. Verify pointers are not NULL before dereferencing.\n3. Ensure array bounds are strictly respected.\n4. Avoid returning pointers to local stack variables.",
            exampleFix = """
// C Fix:
int *ptr = malloc(sizeof(int) * 10);
if (ptr == NULL) {
    perror("Memory allocation failed");
    return 1;
}
// Always free and nullify after use:
free(ptr);
ptr = NULL;
""".trimIndent()
        ),
        ErrorTroubleshooting(
            errorName = "StackOverflowError",
            languages = listOf("Java", "Kotlin", "Python", "C++"),
            cause = "Occurs when the call stack runs out of memory, almost always due to infinite recursion or an omitted/incorrect recursive base case.",
            fix = "1. Ensure every recursive function has a guaranteed terminating base case.\n2. Convert deep recursion into an iterative loop with an explicit data structure.",
            exampleFix = """
// Fix recursive factorial with proper base case:
fun factorial(n: Int): Long {
    if (n <= 1) return 1L // Guaranteed base case
    return n * factorial(n - 1)
}
""".trimIndent()
        ),
        ErrorTroubleshooting(
            errorName = "IndentationError / SyntaxError",
            languages = listOf("Python"),
            cause = "Occurs in Python when lines of code in a block do not share consistent whitespace or indentation levels, or when tabs and spaces are mixed.",
            fix = "Use a standard 4 spaces per indentation level and configure your code editor to convert tabs to spaces.",
            exampleFix = """
# Correct Python Indentation:
def calculate_grade(score):
    if score >= 90:
        return "A"
    else:
        return "B"
""".trimIndent()
        )
    )

    /**
     * Checks if the query asks for programming help, code generation, or error troubleshooting.
     */
    fun canHandle(query: String): Boolean {
        val lower = query.lowercase(Locale.ROOT)
        val isError = listOf("nullpointerexception", "segmentation fault", "indexoutofbounds", "indexerror", "stackoverflow", "indentationerror", "syntaxerror").any { lower.contains(it) }
        if (isError) return true

        val requestMarkers = listOf("code", "example", "how to", "show", "implement", "write", "syntax", "pointer")
        val hasMarker = requestMarkers.any { lower.contains(it) }

        val topicKeywords = listOf(
            "reverse a string", "binary search", "bubble sort", "pointer", "vector",
            "class and object", "data class", "coroutine", "sql query", "second highest salary",
            "python", "java", "kotlin", "c++", "c language"
        )
        val hasTopic = topicKeywords.any { lower.contains(it) }

        return hasMarker && hasTopic
    }

    /**
     * Handles code generation and programming error troubleshooting queries.
     */
    fun answer(query: String): DetectedQuestionAnswerItem? {
        val cleanQ = query.trim()
        val lower = cleanQ.lowercase(Locale.ROOT)

        if (!canHandle(cleanQ)) return null

        // 1. Check Error Troubleshooting
        for (err in ERROR_GUIDES) {
            val errLower = err.errorName.lowercase(Locale.ROOT)
            val keywords = errLower.split(Regex("""[^a-z0-9]""")).filter { it.length > 3 }
            if (lower.contains(errLower) || (keywords.isNotEmpty() && keywords.all { lower.contains(it) })) {
                val body = StringBuilder()
                body.append("Error: ").append(err.errorName).append(" (").append(err.languages.joinToString(", ")).append(")\n\n")
                body.append("Cause:\n").append(err.cause).append("\n\n")
                body.append("How to Fix:\n").append(err.fix).append("\n\n")
                body.append("Code Example:\n").append(err.exampleFix)

                return DetectedQuestionAnswerItem(
                    question = cleanQ,
                    answer = body.toString().trim(),
                    explanation = "Programming Assistant debugging guide for ${err.errorName}.",
                    status = "Answered from Programming Assistant",
                    isAnswered = true
                )
            }
        }

        // 2. Check Code Examples
        for (example in CODE_EXAMPLES) {
            val titleLower = example.title.lowercase(Locale.ROOT)
            val langLower = example.language.lowercase(Locale.ROOT)
            val nonLangTitleWords = titleLower.replace(langLower, "").split(" ")
                .filter { it.length > 3 && it !in setOf("example", "using", "with") }

            val isMatch = lower.contains(titleLower) ||
                    (lower.contains(langLower) && nonLangTitleWords.isNotEmpty() && nonLangTitleWords.any { lower.contains(it) }) ||
                    (titleLower.contains("pointers") && lower.contains("pointer")) ||
                    (titleLower.contains("reverse a string") && lower.contains("reverse a string")) ||
                    (titleLower.contains("binary search") && lower.contains("binary search"))

            if (isMatch) {
                val body = StringBuilder()
                body.append(example.title).append(" (").append(example.language).append(")\n\n")
                body.append(example.description).append("\n\n")
                body.append("Code:\n").append(example.code).append("\n\n")
                body.append("Explanation:\n").append(example.explanation)

                return DetectedQuestionAnswerItem(
                    question = cleanQ,
                    answer = body.toString().trim(),
                    explanation = "Programming Assistant verified code example in ${example.language}.",
                    status = "Answered from Programming Assistant",
                    isAnswered = true
                )
            }
        }

        return null
    }
}
