package com.example.Simtas.data.ml

import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import java.util.Locale

/**
 * Universal Student Knowledge Base for SmartNotes AI.
 * Offline-first, high-precision educational repository covering:
 * 1. Computer Science (Java, Kotlin, Python, C, C++, Data Structures, Algorithms, DB/SQL, OS, Networks)
 * 2. Science (Physics, Chemistry, Biology)
 * 3. Educational Comparisons ("Difference between X and Y")
 * 4. Advantages, Disadvantages, Examples, and Definitions
 *
 * ZERO cloud AI APIs. 100% on-device educational knowledge.
 */
object UniversalStudentKnowledgeBase {

    data class KnowledgeTopic(
        val title: String,
        val domain: String, // "Computer Science", "Physics", "Chemistry", "Biology", "General"
        val definition: String,
        val explanation: String,
        val keyPoints: List<String> = emptyList(),
        val example: String? = null,
        val codeExample: String? = null
    )

    // --- Verified Knowledge Base ---
    private val TOPICS = mutableMapOf<String, KnowledgeTopic>()

    init {
        registerComputerScience()
        registerScience()
    }

    private fun registerTopic(key: String, topic: KnowledgeTopic) {
        TOPICS[key.lowercase(Locale.ROOT).trim()] = topic
    }

    private fun registerComputerScience() {
        // --- Programming Languages ---
        registerTopic(
            "java",
            KnowledgeTopic(
                title = "Java Programming Language",
                domain = "Computer Science",
                definition = "Java is a class-based, object-oriented, platform-independent programming language that runs on the Java Virtual Machine (JVM) using the 'Write Once, Run Anywhere' (WORA) philosophy.",
                explanation = "Java achieves platform independence by compiling source code into bytecode (.class files), which any OS with a JVM can execute. It features automatic garbage collection, strong type checking, and rich standard libraries.",
                keyPoints = listOf(
                    "Platform Independent: Bytecode executes on any JVM regardless of underlying hardware.",
                    "Object-Oriented: Enforces OOP concepts including Classes, Objects, Inheritance, and Encapsulation.",
                    "Memory Management: Automatic garbage collection frees unused heap memory.",
                    "Robust & Secure: No explicit pointers, strictly typed, and built-in bytecode verification."
                ),
                example = "Public class Student { String name; int id; }",
                codeExample = """
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
""".trimIndent()
            )
        )

        registerTopic(
            "kotlin",
            KnowledgeTopic(
                title = "Kotlin Programming Language",
                domain = "Computer Science",
                definition = "Kotlin is a modern, statically typed, cross-platform programming language designed by JetBrains that fully interoperates with Java and is Google's preferred language for Android development.",
                explanation = "Kotlin eliminates boilerplate code with features like data classes, type inference, extension functions, and first-class coroutines for asynchronous programming, while providing compile-time null safety to prevent NullPointerExceptions.",
                keyPoints = listOf(
                    "Null Safety: Type system distinguishes nullable (String?) from non-nullable (String) references.",
                    "Concise Syntax: Reduces boilerplate with data classes, smart casts, and lambdas.",
                    "Java Interoperability: 100% interoperable with existing Java code and libraries.",
                    "Coroutines: Lightweight, non-blocking threads for asynchronous tasks."
                ),
                codeExample = """
data class User(val id: Int, val name: String)

fun main() {
    val user = User(1, "Alice")
    println("User: ${'$'}{user.name}")
}
""".trimIndent()
            )
        )

        registerTopic(
            "python",
            KnowledgeTopic(
                title = "Python Programming Language",
                domain = "Computer Science",
                definition = "Python is a high-level, interpreted, dynamically typed programming language emphasizing code readability, simplicity, and rapid development.",
                explanation = "Python uses whitespace indentation to define code blocks rather than braces. It supports procedural, object-oriented, and functional programming paradigms, and possesses a vast ecosystem for data science, AI, web development, and automation.",
                keyPoints = listOf(
                    "Interpreted: Executes code line-by-line without an explicit ahead-of-time compile step.",
                    "Dynamic Typing: Variable types are checked during execution at runtime.",
                    "Batteries Included: Comprehensive standard library and extensive PyPI package ecosystem.",
                    "Clean Syntax: Indentation defines scope, producing readable, concise programs."
                ),
                codeExample = """
def greet(name: str) -> str:
    return f"Hello, {name}!"

numbers = [1, 2, 3, 4, 5]
squares = [x**2 for x in numbers]  # List comprehension
print(squares)  # [1, 4, 9, 16, 25]
""".trimIndent()
            )
        )

        registerTopic(
            "c language",
            KnowledgeTopic(
                title = "C Programming Language",
                domain = "Computer Science",
                definition = "C is a foundational, procedural, general-purpose computer programming language supporting structured programming, lexical variable scope, and low-level memory manipulation via pointers.",
                explanation = "Developed by Dennis Ritchie at Bell Labs, C serves as the foundation for operating systems (such as Linux and Windows), device drivers, and compilers. It provides direct hardware and memory access with minimal runtime overhead.",
                keyPoints = listOf(
                    "Low-Level Access: Direct memory addressing and manipulation through raw pointers.",
                    "Efficiency: Produces compact, high-speed compiled machine code with minimal runtime overhead.",
                    "Procedural: Structured around functions and subroutines rather than objects and classes.",
                    "Portability: Standardized ANSI/ISO C compiles across almost all CPU architectures."
                ),
                codeExample = """
#include <stdio.h>

int main() {
    int num = 42;
    int *ptr = &num;  // Pointer storing address of num
    printf("Value: %d, Address: %p\n", *ptr, (void*)ptr);
    return 0;
}
""".trimIndent()
            )
        )

        registerTopic(
            "c++",
            KnowledgeTopic(
                title = "C++ Programming Language",
                domain = "Computer Science",
                definition = "C++ is a high-performance, statically typed, general-purpose programming language developed by Bjarne Stroustrup as an extension of C with classes, object-oriented features, and generic programming templates.",
                explanation = "C++ combines low-level memory control with high-level abstractions like RAII (Resource Acquisition Is Initialization), Standard Template Library (STL), templates, and operator overloading. It is widely used in game engines, high-frequency trading, and embedded systems.",
                keyPoints = listOf(
                    "Multi-Paradigm: Supports procedural, object-oriented, and generic programming.",
                    "Standard Template Library (STL): Provides optimized vectors, maps, queues, and sorting algorithms.",
                    "Memory Control: Supports stack allocation, pointers, references, and smart pointers (std::unique_ptr).",
                    "Zero-Overhead Principle: Abstractions do not carry runtime or memory penalties."
                ),
                codeExample = """
#include <iostream>
#include <vector>

int main() {
    std::vector<int> nums = {10, 20, 30};
    nums.push_back(40);
    for (int n : nums) {
        std::cout << n << " ";
    }
    return 0;
}
""".trimIndent()
            )
        )

        // --- OOP Principles ---
        registerTopic(
            "oop",
            KnowledgeTopic(
                title = "Object-Oriented Programming (OOP)",
                domain = "Computer Science",
                definition = "Object-Oriented Programming (OOP) is a programming paradigm based on the concept of 'objects', which contain data in the form of fields (attributes) and code in the form of procedures (methods).",
                explanation = "OOP organizes software design around data and objects rather than functions and logic. The four foundational pillars of OOP are Encapsulation, Abstraction, Inheritance, and Polymorphism.",
                keyPoints = listOf(
                    "Encapsulation: Bundling data and methods into a single unit (class) and restricting direct access to internal state using private variables.",
                    "Abstraction: Hiding complex internal implementation details and exposing only the essential interface to the user.",
                    "Inheritance: Mechanism where a child class acquires properties and behaviors of a parent class to promote code reuse.",
                    "Polymorphism: Ability of an object or method to take on multiple forms (Method Overloading and Method Overriding)."
                ),
                example = "A 'Car' class inherits from a 'Vehicle' class, encapsulating properties like speed and fuel."
            )
        )

        registerTopic(
            "polymorphism",
            KnowledgeTopic(
                title = "Polymorphism in OOP",
                domain = "Computer Science",
                definition = "Polymorphism is an OOP principle allowing one interface or method name to be used for a general class of actions, enabling objects of different classes to respond differently to the same message.",
                explanation = "There are two primary types of polymorphism: Compile-time (Static) Polymorphism achieved through method overloading, and Runtime (Dynamic) Polymorphism achieved through method overriding and virtual functions.",
                keyPoints = listOf(
                    "Compile-time (Overloading): Multiple methods with the same name but different parameter lists within the same class.",
                    "Runtime (Overriding): Child class provides a specific implementation of a method already declared in its parent class.",
                    "Benefits: Enhances software flexibility, extensibility, and maintainability."
                ),
                codeExample = """
open class Animal {
    open fun sound() = println("Animal makes sound")
}
class Dog : Animal() {
    override fun sound() = println("Dog barks")
}
""".trimIndent()
            )
        )

        registerTopic(
            "inheritance",
            KnowledgeTopic(
                title = "Inheritance in OOP",
                domain = "Computer Science",
                definition = "Inheritance is an OOP mechanism wherein a new class (derived/subclass) inherits attributes and methods from an existing class (base/superclass), establishing an 'IS-A' relationship.",
                explanation = "Inheritance promotes code reusability, minimizes redundancy, and facilitates hierarchical classification. Subclasses can reuse, extend, or override inherited behavior.",
                keyPoints = listOf(
                    "Reusability: Subclasses reuse verified parent class code without re-implementation.",
                    "Types: Single, Multilevel, Hierarchical, and Multiple (via interfaces in Java/Kotlin).",
                    "IS-A Relationship: A Dog 'is a' Mammal; a Square 'is a' Shape."
                )
            )
        )

        registerTopic(
            "encapsulation",
            KnowledgeTopic(
                title = "Encapsulation in OOP",
                domain = "Computer Science",
                definition = "Encapsulation is the OOP principle of bundling data (fields) and the methods that operate on that data into a single unit (class), while restricting unauthorized direct access from outside the class.",
                explanation = "Encapsulation achieves data hiding through access specifiers (private, protected, public) and exposes controlled access via public getter and setter methods with internal validation.",
                keyPoints = listOf(
                    "Data Hiding: Internal state is kept private to protect against accidental corruption.",
                    "Validation: Setter methods can inspect and enforce valid value ranges before assignment.",
                    "Modularity: Internal implementation can be updated without affecting external callers."
                )
            )
        )

        registerTopic(
            "abstraction",
            KnowledgeTopic(
                title = "Abstraction in OOP",
                domain = "Computer Science",
                definition = "Abstraction is the OOP principle of hiding internal implementation complexity and displaying only the essential functionalities to the user.",
                explanation = "Abstraction reduces complexity by focusing on 'what' an object does rather than 'how' it accomplishes it. It is implemented in software using abstract classes and interfaces.",
                keyPoints = listOf(
                    "Interfaces & Abstract Classes: Define contracts that implementing classes must satisfy.",
                    "Simplification: Users interact with clear interfaces without worrying about internal machinery.",
                    "Real-World Analogy: Pressing an accelerator pedal accelerates a car without the driver needing to know the fuel injection mechanics."
                )
            )
        )

        // --- Data Structures ---
        registerTopic(
            "data structure",
            KnowledgeTopic(
                title = "Data Structures",
                domain = "Computer Science",
                definition = "A data structure is a specialized format for organizing, storing, processing, and retrieving data efficiently in computer memory.",
                explanation = "Different data structures are suited to different types of applications. Choosing the optimal data structure directly affects algorithmic time complexity (speed) and space complexity (memory usage).",
                keyPoints = listOf(
                    "Linear Data Structures: Elements arranged sequentially (Arrays, Linked Lists, Stacks, Queues).",
                    "Non-Linear Data Structures: Elements arranged hierarchically or interconnected (Trees, Graphs).",
                    "Key Operations: Insertion, Deletion, Traversal, Searching, and Sorting."
                )
            )
        )

        registerTopic(
            "array",
            KnowledgeTopic(
                title = "Array Data Structure",
                domain = "Computer Science",
                definition = "An array is a linear collection of elements of the same data type stored at contiguous memory locations, accessible via zero-based integer indices.",
                explanation = "Arrays offer constant-time O(1) random access by index because memory addresses can be calculated directly. However, traditional arrays have a fixed size and require O(n) time for element insertion or deletion due to shifting.",
                keyPoints = listOf(
                    "Contiguous Memory: Elements occupy consecutive bytes in RAM.",
                    "Fast Access: Index access arr[i] is instantaneous O(1).",
                    "Fixed Size: Static capacity allocated at creation; dynamic arrays (like ArrayList) resize automatically.",
                    "Insertion/Deletion: Requires O(n) time to shift adjacent elements."
                )
            )
        )

        registerTopic(
            "linked list",
            KnowledgeTopic(
                title = "Linked List Data Structure",
                domain = "Computer Science",
                definition = "A linked list is a linear data structure consisting of nodes, where each node contains a data value and a reference (pointer) to the next node in the sequence.",
                explanation = "Unlike arrays, linked list nodes are not stored in contiguous memory. They support dynamic sizing and fast O(1) insertions/deletions at the head, but require linear O(n) time for random element access.",
                keyPoints = listOf(
                    "Dynamic Size: Allocates memory as needed on the heap; no resizing overflow.",
                    "Fast Insertion/Deletion: O(1) time if the target node pointer is already known.",
                    "Sequential Access: Must traverse from head node step-by-step; no O(1) random indexing.",
                    "Types: Singly Linked List (next only), Doubly Linked List (next and prev), Circular Linked List."
                )
            )
        )

        registerTopic(
            "stack",
            KnowledgeTopic(
                title = "Stack Data Structure",
                domain = "Computer Science",
                definition = "A stack is a linear data structure following the Last-In, First-Out (LIFO) principle, where insertions and deletions happen exclusively at one end called the 'top'.",
                explanation = "A stack operates like a stack of plates: the last plate placed on top is the first plate removed. Standard operations are push (insert), pop (remove top), and peek (inspect top), all executing in O(1) time.",
                keyPoints = listOf(
                    "LIFO Principle: Last-In, First-Out.",
                    "Core Operations: push(x) adds to top, pop() removes top, peek() views top without removal.",
                    "Time Complexity: O(1) for push, pop, and peek.",
                    "Applications: Function call stack (call frames), undo/redo mechanisms, expression parsing, recursion backtracking."
                )
            )
        )

        registerTopic(
            "queue",
            KnowledgeTopic(
                title = "Queue Data Structure",
                domain = "Computer Science",
                definition = "A queue is a linear data structure following the First-In, First-Out (FIFO) principle, where elements are inserted at the rear (enqueue) and removed from the front (dequeue).",
                explanation = "A queue mirrors real-world waiting lines: the first person to arrive is the first person served. Both enqueue and dequeue operations run in O(1) time.",
                keyPoints = listOf(
                    "FIFO Principle: First-In, First-Out.",
                    "Core Operations: enqueue(x) adds at rear, dequeue() removes from front, front() views head element.",
                    "Time Complexity: O(1) for enqueue and dequeue.",
                    "Variants: Circular Queue, Priority Queue (heap-based), Double-Ended Queue (Deque).",
                    "Applications: CPU scheduling queues, printer spooling, breadth-first search (BFS)."
                )
            )
        )

        registerTopic(
            "hash table",
            KnowledgeTopic(
                title = "Hash Table / HashMap Data Structure",
                domain = "Computer Science",
                definition = "A hash table (hash map) is a data structure that implements an associative array of key-value pairs, using a hash function to compute an index into an array of buckets.",
                explanation = "Hash tables provide average-case O(1) time complexity for lookup, insertion, and deletion. Collisions (when multiple keys hash to the same bucket) are handled via separate chaining (linked lists) or open addressing (linear probing).",
                keyPoints = listOf(
                    "Hash Function: Converts a key into an integer array index.",
                    "Average Time Complexity: O(1) for search, insert, and delete.",
                    "Worst-case Time Complexity: O(n) if all keys collide into a single bucket.",
                    "Collision Resolution: Separate chaining (buckets contain linked lists) or open addressing.",
                    "Applications: Database indexing, symbol tables in compilers, caches (LRU cache)."
                )
            )
        )

        registerTopic(
            "binary search tree",
            KnowledgeTopic(
                title = "Binary Search Tree (BST)",
                domain = "Computer Science",
                definition = "A Binary Search Tree (BST) is a hierarchical node-based binary tree data structure where each node has at most two children, and the left subtree contains keys smaller than the node's key while the right subtree contains keys greater.",
                explanation = "BST maintains sorted order dynamically. On a balanced tree (like AVL or Red-Black trees), searching, insertion, and deletion take O(log n) time. On a degenerated tree (skewed into a line), time degrades to O(n).",
                keyPoints = listOf(
                    "Ordering Invariant: left.key < node.key < right.key for all nodes.",
                    "Search Time Complexity: Average O(log n), worst-case O(n) if unbalanced.",
                    "Inorder Traversal: Traverses keys in ascending sorted order (Left -> Node -> Right).",
                    "Self-Balancing Trees: AVL trees and Red-Black trees automatically rebalance via tree rotations."
                )
            )
        )

        // --- Algorithms ---
        registerTopic(
            "binary search",
            KnowledgeTopic(
                title = "Binary Search Algorithm",
                domain = "Computer Science",
                definition = "Binary search is an efficient search algorithm that finds the position of a target value within a sorted array by repeatedly dividing the search interval in half.",
                explanation = "Binary search compares the target value to the middle element of the array. If they match, the index is returned. If the target is smaller, the search continues in the lower half; if larger, in the upper half. It runs in O(log n) time.",
                keyPoints = listOf(
                    "Precondition: The input array MUST already be sorted.",
                    "Time Complexity: Best O(1), Average and Worst O(log n).",
                    "Space Complexity: O(1) for iterative, O(log n) for recursive call stack.",
                    "Efficiency: Can search through 1,000,000 sorted elements in at most 20 comparisons (log2(1,000,000) ≈ 20)."
                ),
                codeExample = """
fun binarySearch(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1
    while (left <= right) {
        val mid = left + (right - left) / 2
        when {
            arr[mid] == target -> return mid
            arr[mid] < target -> left = mid + 1
            else -> right = mid - 1
        }
    }
    return -1 // Target not found
}
""".trimIndent()
            )
        )

        registerTopic(
            "bubble sort",
            KnowledgeTopic(
                title = "Bubble Sort Algorithm",
                domain = "Computer Science",
                definition = "Bubble sort is a simple comparison-based sorting algorithm that repeatedly steps through the list, compares adjacent elements, and swaps them if they are in the wrong order.",
                explanation = "The pass through the list is repeated until no swaps are needed. Larger elements 'bubble up' to the end of the array with each pass. It has a quadratic time complexity of O(n²), making it impractical for large datasets.",
                keyPoints = listOf(
                    "Mechanism: Compares adjacent pairs and swaps them if arr[i] > arr[i+1].",
                    "Time Complexity: Best O(n) if already sorted, Average and Worst O(n²).",
                    "Space Complexity: O(1) auxiliary space (in-place sort).",
                    "Stability: Stable sorting algorithm (preserves relative order of equal elements)."
                )
            )
        )

        registerTopic(
            "merge sort",
            KnowledgeTopic(
                title = "Merge Sort Algorithm",
                domain = "Computer Science",
                definition = "Merge sort is an efficient, general-purpose, comparison-based divide-and-conquer sorting algorithm that divides an array into halves, sorts them recursively, and merges the sorted halves.",
                explanation = "Merge sort divides the unsorted list into n sublists until each contains 1 element, then repeatedly merges sublists to produce new sorted sublists. It guarantees O(n log n) time in all cases but requires O(n) auxiliary memory.",
                keyPoints = listOf(
                    "Divide and Conquer: Halves the problem, solves recursively, merges results.",
                    "Time Complexity: Guaranteed O(n log n) in best, average, and worst cases.",
                    "Space Complexity: O(n) auxiliary memory needed during merging.",
                    "Stability: Stable sorting algorithm."
                )
            )
        )

        registerTopic(
            "quick sort",
            KnowledgeTopic(
                title = "Quick Sort Algorithm",
                domain = "Computer Science",
                definition = "Quick sort is a highly efficient divide-and-conquer sorting algorithm that selects a 'pivot' element and partitions the array into elements smaller and larger than the pivot.",
                explanation = "The sub-arrays are then sorted recursively. With good pivot selection (e.g. median-of-three or randomized), it runs in O(n log n) average time with O(log n) stack space. Worst-case is O(n²) if an extreme pivot is consistently chosen.",
                keyPoints = listOf(
                    "Partitioning: Elements <= pivot placed on left, > pivot placed on right.",
                    "Time Complexity: Average O(n log n), Worst-case O(n²).",
                    "Space Complexity: O(log n) auxiliary stack space (in-place).",
                    "Cache Friendly: Exceptional real-world performance due to contiguous memory access."
                )
            )
        )

        registerTopic(
            "big o notation",
            KnowledgeTopic(
                title = "Big O Notation & Algorithmic Complexity",
                domain = "Computer Science",
                definition = "Big O notation is a mathematical notation that characterizes the time complexity and space complexity of an algorithm relative to input size (n).",
                explanation = "Big O measures upper-bound scalability (worst-case performance). Common complexities ranked from fastest to slowest: O(1) Constant < O(log n) Logarithmic < O(n) Linear < O(n log n) Linearithmic < O(n²) Quadratic < O(2ⁿ) Exponential < O(n!) Factorial.",
                keyPoints = listOf(
                    "O(1) Constant: Array index lookup, Hash map insertion (average).",
                    "O(log n) Logarithmic: Binary search in sorted array.",
                    "O(n) Linear: Linear search, traversing an array or linked list.",
                    "O(n log n) Linearithmic: Merge sort, Quick sort (average), Heap sort.",
                    "O(n²) Quadratic: Bubble sort, Selection sort, nested loops.",
                    "O(2ⁿ) Exponential: Recursive Fibonacci calculation without memoization."
                )
            )
        )

        // --- Databases & SQL ---
        registerTopic(
            "acid properties",
            KnowledgeTopic(
                title = "ACID Properties in Databases",
                domain = "Computer Science",
                definition = "ACID is an acronym representing the four key properties that guarantee database transactions are processed reliably in a Relational Database Management System (RDBMS).",
                explanation = "ACID ensures database integrity even during power failures, crashes, and concurrent transactions. The four components are Atomicity, Consistency, Isolation, and Durability.",
                keyPoints = listOf(
                    "Atomicity: 'All or nothing'. If one part of a transaction fails, the entire transaction rolls back.",
                    "Consistency: A transaction must transition the database from one valid state to another, satisfying all constraints.",
                    "Isolation: Concurrent execution of transactions yields the same state as if they were executed sequentially.",
                    "Durability: Once a transaction commits, its changes survive power loss or system crashes (saved to write-ahead log/disk)."
                ),
                example = "In a bank transfer of $100, subtracting $100 from Account A and adding $100 to Account B must succeed together (Atomicity)."
            )
        )

        registerTopic(
            "sql joins",
            KnowledgeTopic(
                title = "SQL Joins",
                domain = "Computer Science",
                definition = "A SQL JOIN clause is used to combine rows from two or more tables based on a related column between them (typically primary and foreign keys).",
                explanation = "Joins allow relational databases to keep tables normalized while querying connected entities. Main types are INNER JOIN, LEFT JOIN, RIGHT JOIN, and FULL OUTER JOIN.",
                keyPoints = listOf(
                    "INNER JOIN: Returns records that have matching values in both tables.",
                    "LEFT (OUTER) JOIN: Returns all records from the left table, and matching records from the right table (NULL if no match).",
                    "RIGHT (OUTER) JOIN: Returns all records from the right table, and matching records from the left table.",
                    "FULL (OUTER) JOIN: Returns all records when there is a match in either left or right table."
                ),
                codeExample = """
-- INNER JOIN example
SELECT Students.name, Courses.course_name
FROM Students
INNER JOIN Enrollments ON Students.id = Enrollments.student_id
INNER JOIN Courses ON Enrollments.course_id = Courses.id;
""".trimIndent()
            )
        )

        // --- Operating Systems ---
        registerTopic(
            "deadlock",
            KnowledgeTopic(
                title = "Deadlock in Operating Systems",
                domain = "Computer Science",
                definition = "A deadlock is a situation in an operating system where a set of processes are blocked because each process is holding a resource and waiting for another resource held by another process in the set.",
                explanation = "Deadlock prevents all affected processes from executing. It occurs only when four conditions hold simultaneously (Coffman Conditions): Mutual Exclusion, Hold and Wait, No Preemption, and Circular Wait.",
                keyPoints = listOf(
                    "Mutual Exclusion: At least one resource must be held in a non-shareable mode.",
                    "Hold and Wait: A process holds resources while waiting for additional resources.",
                    "No Preemption: Resources cannot be forcibly taken; only released voluntarily.",
                    "Circular Wait: A closed chain of processes exists where each waits for a resource held by the next.",
                    "Prevention & Avoidance: Banker's Algorithm, ordering resource acquisitions, lock timeouts."
                )
            )
        )

        registerTopic(
            "virtual memory",
            KnowledgeTopic(
                title = "Virtual Memory & Paging",
                domain = "Computer Science",
                definition = "Virtual memory is a memory management capability of an operating system that uses hardware and software to allow a computer to compensate for physical memory (RAM) shortages by temporarily transferring data to disk storage.",
                explanation = "Virtual memory maps virtual addresses used by programs to physical addresses in RAM using Page Tables. Memory is divided into fixed-size blocks called Pages (in virtual memory) and Frames (in physical RAM).",
                keyPoints = listOf(
                    "Paging: Division of virtual address space into fixed-size blocks (commonly 4KB pages).",
                    "Page Fault: Hardware interrupt triggered when a program accesses a page not currently in physical RAM.",
                    "Swap Space: Area on disk (paging file) where evicted pages are stored until needed.",
                    "Thrashing: Severe performance collapse caused by the OS spending more time swapping pages than executing instructions."
                )
            )
        )

        // --- Computer Networks ---
        registerTopic(
            "osi model",
            KnowledgeTopic(
                title = "OSI 7-Layer Reference Model",
                domain = "Computer Science",
                definition = "The Open Systems Interconnection (OSI) model is a conceptual framework that standardizes the functions of a telecommunication or computing system into seven abstract abstraction layers.",
                explanation = "Data moves from Layer 7 down to Layer 1 on the sending host, travels across physical media, and moves from Layer 1 up to Layer 7 on the receiving host. Each layer provides services to the layer above it.",
                keyPoints = listOf(
                    "Layer 7 - Application: User interface & network services (HTTP, HTTPS, FTP, DNS, SMTP).",
                    "Layer 6 - Presentation: Data formatting, encryption, decryption, compression (SSL/TLS, JPEG, ASCII).",
                    "Layer 5 - Session: Establishes, manages, and terminates connections between applications.",
                    "Layer 4 - Transport: End-to-end delivery, flow control, error recovery (TCP, UDP).",
                    "Layer 3 - Network: Logical addressing, path determination, and packet routing (IP, ICMP, Routers).",
                    "Layer 2 - Data Link: Physical addressing (MAC), framing, error detection (Ethernet, Switches).",
                    "Layer 1 - Physical: Transmission of raw unstructured bit streams over physical media (Cables, Hubs)."
                )
            )
        )

        registerTopic(
            "tcp and udp",
            KnowledgeTopic(
                title = "TCP vs UDP Protocols",
                domain = "Computer Science",
                definition = "TCP (Transmission Control Protocol) and UDP (User Datagram Protocol) are the two primary Transport Layer protocols used to transmit data across IP networks.",
                explanation = "TCP is connection-oriented, reliable, and guarantees in-order packet delivery using a three-way handshake and retransmissions. UDP is connectionless, lightweight, and fast, transmitting datagrams with no handshake or delivery guarantee.",
                keyPoints = listOf(
                    "Connection: TCP establishes connection via 3-way handshake (SYN, SYN-ACK, ACK); UDP is connectionless.",
                    "Reliability: TCP guarantees delivery via acknowledgments and retransmissions; UDP provides best-effort delivery.",
                    "Speed & Overhead: UDP has lower latency and smaller header (8 bytes vs 20+ bytes in TCP).",
                    "Use Cases: TCP is used for Web (HTTP/HTTPS), Email (SMTP), File transfer; UDP is used for Video streaming, Gaming, DNS, VoIP."
                )
            )
        )
    }

    private fun registerScience() {
        // --- Physics ---
        registerTopic(
            "newton's second law",
            KnowledgeTopic(
                title = "Newton's Second Law of Motion",
                domain = "Physics",
                definition = "Newton's Second Law of Motion states that the acceleration of an object is directly proportional to the net force acting upon it and inversely proportional to its mass, expressed mathematically as F = ma.",
                explanation = "Where F is the net force in Newtons (N), m is the mass in kilograms (kg), and a is the acceleration in meters per second squared (m/s²). If force increases, acceleration increases; if mass increases, acceleration decreases for the same force.",
                keyPoints = listOf(
                    "Formula: Force = mass × acceleration (F = ma).",
                    "Units: Force in Newtons (1 N = 1 kg·m/s²), mass in kg, acceleration in m/s².",
                    "Direction: Acceleration occurs in the exact direction of the net applied force.",
                    "Example: Pushing a 10 kg box with 50 N of force produces an acceleration of 5 m/s² (a = F/m = 50/10)."
                )
            )
        )

        registerTopic(
            "newton's laws",
            KnowledgeTopic(
                title = "Newton's Three Laws of Motion",
                domain = "Physics",
                definition = "Newton's Laws of Motion are three classical physical laws that establish the relationship between the forces acting on a body and the motion of that body.",
                explanation = "Formulated by Sir Isaac Newton in 1687, these laws form the foundation of classical mechanics.",
                keyPoints = listOf(
                    "First Law (Inertia): An object remains at rest or in uniform motion unless acted upon by a net external force.",
                    "Second Law (Force): Acceleration is proportional to force and inversely proportional to mass: F = ma.",
                    "Third Law (Action & Reaction): For every action, there is an equal and opposite reaction (forces always occur in matched pairs)."
                )
            )
        )

        registerTopic(
            "gravity",
            KnowledgeTopic(
                title = "Gravity & Newton's Law of Universal Gravitation",
                domain = "Physics",
                definition = "Gravity is a fundamental physical force of attraction that acts between all objects with mass or energy throughout the universe.",
                explanation = "Newton's Law of Universal Gravitation states that any two bodies in the universe attract each other with a force proportional to the product of their masses and inversely proportional to the square of the distance between their centers: F = G * (m1 * m2) / r².",
                keyPoints = listOf(
                    "Universal Formula: F = G · (m₁ · m₂) / r², where G is the gravitational constant (6.674 × 10⁻¹¹ N·m²/kg²).",
                    "Earth Acceleration: Near Earth's surface, gravitational acceleration g ≈ 9.8 m/s² (or ~9.81 m/s²).",
                    "Weight vs Mass: Mass is the invariant quantity of matter (kg); weight is the gravitational force acting on that mass (W = mg in Newtons)."
                )
            )
        )

        registerTopic(
            "ohm's law",
            KnowledgeTopic(
                title = "Ohm's Law in Physics & Electronics",
                domain = "Physics",
                definition = "Ohm's Law states that the electric current passing through a conductor between two points is directly proportional to the voltage across the two points and inversely proportional to the resistance: V = I * R.",
                explanation = "Where V is voltage in Volts (V), I is current in Amperes (A), and R is resistance in Ohms (Ω). It is the fundamental law governing DC electrical circuits.",
                keyPoints = listOf(
                    "Formula: Voltage = Current × Resistance (V = I · R).",
                    "Alternative Forms: I = V / R, R = V / I.",
                    "Units: Voltage in Volts (V), Current in Amperes (A), Resistance in Ohms (Ω).",
                    "Example: A 12V battery connected to a 4Ω resistor draws a current of 3A (I = 12 / 4 = 3)."
                )
            )
        )

        // --- Chemistry ---
        registerTopic(
            "atom",
            KnowledgeTopic(
                title = "Atomic Structure",
                domain = "Chemistry",
                definition = "An atom is the basic building block of chemistry and the smallest unit of ordinary matter that forms a chemical element, consisting of a dense central nucleus surrounded by a cloud of negatively charged electrons.",
                explanation = "The central nucleus contains positively charged protons and neutral neutrons (except hydrogen-1 which has no neutrons). Negatively charged electrons orbit the nucleus in discrete energy levels (shells).",
                keyPoints = listOf(
                    "Protons: Positively charged particles (+1) located in the nucleus; count determines atomic number (Z).",
                    "Neutrons: Electrically neutral particles (0) in nucleus; variations produce isotopes.",
                    "Electrons: Negatively charged particles (-1) orbiting in electron shells; determine chemical bonding.",
                    "Atomic Mass: Sum of protons and neutrons in the nucleus (electrons have negligible mass ~1/1836 amu)."
                )
            )
        )

        registerTopic(
            "chemical bonds",
            KnowledgeTopic(
                title = "Chemical Bonding (Covalent, Ionic, Hydrogen)",
                domain = "Chemistry",
                definition = "A chemical bond is an attractive force that holds atoms or ions together to form molecules, crystals, and chemical compounds.",
                explanation = "Atoms form bonds to achieve stable electron configurations (typically an octet of 8 valence electrons). The primary types are Covalent, Ionic, and Metallic bonds.",
                keyPoints = listOf(
                    "Covalent Bond: Formed when two non-metal atoms share pairs of valence electrons (e.g. H₂O, CO₂, CH₄).",
                    "Ionic Bond: Formed by the electrostatic attraction between oppositely charged ions created by electron transfer (e.g. NaCl: Na⁺ and Cl⁻).",
                    "Hydrogen Bond: Weak intermolecular attraction between a hydrogen atom covalently bonded to an electronegative atom (N, O, F) and another electronegative atom.",
                    "Metallic Bond: Attraction between a lattice of positive metal cations and a delocalized 'sea of electrons'."
                )
            )
        )

        registerTopic(
            "acids and bases",
            KnowledgeTopic(
                title = "Acids, Bases, and the pH Scale",
                domain = "Chemistry",
                definition = "Acids are chemical substances that release hydrogen ions (H⁺ or hydronium H₃O⁺) in aqueous solution, while bases release hydroxide ions (OH⁻) or accept protons.",
                explanation = "The pH scale measures the concentration of hydrogen ions on a logarithmic scale from 0 to 14. A pH of 7 represents a neutral solution (pure water at 25°C).",
                keyPoints = listOf(
                    "Acids: pH < 7, sour taste, turn blue litmus red (e.g. Hydrochloric acid HCl, Citric acid, Vinegar).",
                    "Neutral: pH = 7, equal concentrations of H⁺ and OH⁻ (pure water).",
                    "Bases (Alkalis): pH > 7, bitter taste, slippery feel, turn red litmus blue (e.g. Sodium hydroxide NaOH, Bleach).",
                    "Logarithmic Scale: Each 1-unit decrease in pH represents a 10-fold increase in hydrogen ion concentration."
                )
            )
        )

        // --- Biology ---
        registerTopic(
            "photosynthesis",
            KnowledgeTopic(
                title = "Photosynthesis in Biology",
                domain = "Biology",
                definition = "Photosynthesis is the biological process by which green plants, algae, and cyanobacteria convert light energy into chemical energy stored in glucose molecules, using water and carbon dioxide.",
                explanation = "Photosynthesis takes place within plant chloroplasts containing chlorophyll pigments. It occurs in two stages: Light-Dependent Reactions (producing ATP and NADPH, releasing O₂) and the Light-Independent Calvin Cycle (synthesizing glucose).",
                keyPoints = listOf(
                    "Chemical Equation: 6 CO₂ + 6 H₂O + Light Energy → C₆H₁₂O₆ + 6 O₂.",
                    "Reactants: Carbon dioxide (absorbed via stomata) and Water (absorbed by roots) powered by sunlight.",
                    "Products: Glucose (chemical energy for growth) and Oxygen (released into the atmosphere).",
                    "Chloroplast & Chlorophyll: Green pigment that captures photons and drives the photosynthetic electron transport chain."
                )
            )
        )

        registerTopic(
            "cellular respiration",
            KnowledgeTopic(
                title = "Cellular Respiration in Biology",
                domain = "Biology",
                definition = "Cellular respiration is the biochemical process by which cells break down glucose and other organic molecules in the presence of oxygen to produce ATP (adenosine triphosphate), releasing water and carbon dioxide as byproducts.",
                explanation = "Occurring primarily within mitochondria, aerobic cellular respiration consists of four stages: Glycolysis, the Link Reaction, the Krebs (Citric Acid) Cycle, and the Electron Transport Chain (Oxidative Phosphorylation), producing ~36-38 ATP per glucose molecule.",
                keyPoints = listOf(
                    "Chemical Equation: C₆H₁₂O₆ + 6 O₂ → 6 CO₂ + 6 H₂O + ~36-38 ATP.",
                    "Location: Glycolysis occurs in the cytoplasm; Krebs Cycle and Electron Transport Chain occur in mitochondria.",
                    "Purpose: Generates ATP, the universal energy currency for cellular work.",
                    "Aerobic vs Anaerobic: Aerobic requires oxygen and produces high ATP; anaerobic (fermentation) produces only 2 ATP with lactic acid or ethanol."
                )
            )
        )

        registerTopic(
            "dna and rna",
            KnowledgeTopic(
                title = "DNA vs RNA (Nucleic Acids)",
                domain = "Biology",
                definition = "DNA (Deoxyribonucleic Acid) and RNA (Ribonucleic Acid) are the two primary nucleic acids that carry genetic information and guide protein synthesis in all living cells.",
                explanation = "DNA stores the hereditary master blueprint inside the cell nucleus, whereas RNA translates the genetic code into functional proteins through transcription and translation.",
                keyPoints = listOf(
                    "Structure: DNA is double-stranded (double helix); RNA is typically single-stranded.",
                    "Sugar: DNA contains deoxyribose; RNA contains ribose (with an extra -OH group).",
                    "Nitrogenous Bases: DNA uses Adenine (A), Thymine (T), Cytosine (C), Guanine (G); RNA replaces Thymine with Uracil (U).",
                    "Function: DNA provides long-term genetic storage; RNA carries genetic messages (mRNA), transfers amino acids (tRNA), and builds ribosomes (rRNA)."
                )
            )
        )

        registerTopic(
            "mitosis and meiosis",
            KnowledgeTopic(
                title = "Mitosis vs Meiosis (Cell Division)",
                domain = "Biology",
                definition = "Mitosis and Meiosis are the two types of eukaryotic cell division. Mitosis produces identical somatic cells for growth and repair, while Meiosis produces genetically diverse gametes for sexual reproduction.",
                explanation = "Mitosis involves one division producing two diploid (2n) daughter cells. Meiosis involves two successive divisions (Meiosis I and II) with crossing over, producing four haploid (n) daughter cells with half the original chromosome number.",
                keyPoints = listOf(
                    "Mitosis: 1 division → 2 genetically identical daughter cells; Diploid (2n) → Diploid (2n).",
                    "Meiosis: 2 divisions → 4 genetically unique daughter cells; Diploid (2n) → Haploid (n).",
                    "Purpose: Mitosis is for tissue growth and cellular repair; Meiosis produces sperm and egg gametes.",
                    "Genetic Variation: Meiosis introduces genetic variation via crossing over (recombination) and independent assortment."
                )
            )
        )
    }

    /**
     * Searches the universal knowledge base for a student query.
     */
    fun findKnowledge(query: String): DetectedQuestionAnswerItem? {
        val clean = query.trim()
        val lower = clean.lowercase(Locale.ROOT)
            .replace("?", "")
            .replace("!", "")
            .replace(".", "")
            .replace(",", " ")
            .replace(Regex("""^(what is|what are|why is|why are|how does|how do|explain|define|describe|clarify|tell me about|name\s+some|name|list\s+some|list|give\s+some|give|mention\s+some|mention|identify|state)\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (lower.isBlank()) return null

        val strippedArticle = lower.replace(Regex("""^(?:a|an|the)\s+"""), "").trim()

        // 1. Exact topic key match
        TOPICS[strippedArticle]?.let { topic ->
            return formatAnswerItem(clean, topic)
        }
        TOPICS[lower]?.let { topic ->
            return formatAnswerItem(clean, topic)
        }

        // 2. Substring or keyword match
        for ((key, topic) in TOPICS) {
            if (strippedArticle == key || strippedArticle.contains(key) || key.contains(strippedArticle)) {
                return formatAnswerItem(clean, topic)
            }
        }

        // 3. Multi-word overlap match
        val words = lower.split(" ").filter { it.length > 2 && it !in setOf("the", "and", "for", "with") }
        if (words.size >= 2) {
            for ((key, topic) in TOPICS) {
                val keyWords = key.split(" ").filter { it.length > 2 }
                if (keyWords.isNotEmpty() && keyWords.all { lower.contains(it) }) {
                    return formatAnswerItem(clean, topic)
                }
            }
        }

        return null
    }

    private fun formatAnswerItem(query: String, topic: KnowledgeTopic): DetectedQuestionAnswerItem {
        val answerBuilder = StringBuilder()
        answerBuilder.append(topic.definition).append("\n\n").append(topic.explanation)

        if (topic.keyPoints.isNotEmpty()) {
            answerBuilder.append("\n\nKey Concepts:")
            for (point in topic.keyPoints) {
                answerBuilder.append("\n• ").append(point)
            }
        }

        if (!topic.codeExample.isNullOrBlank()) {
            answerBuilder.append("\n\nExample Code:\n").append(topic.codeExample)
        } else if (!topic.example.isNullOrBlank()) {
            answerBuilder.append("\n\nExample: ").append(topic.example)
        }

        val statusText = when (topic.domain) {
            "Computer Science" -> "Answered from Computer Science Knowledge"
            "Physics", "Chemistry", "Biology" -> "Answered from Science Knowledge"
            else -> "Answered from Educational Knowledge"
        }

        return DetectedQuestionAnswerItem(
            question = query,
            answer = answerBuilder.toString().trim(),
            explanation = "Verified educational knowledge for ${topic.domain}: ${topic.title}.",
            status = statusText,
            isAnswered = true
        )
    }
}
