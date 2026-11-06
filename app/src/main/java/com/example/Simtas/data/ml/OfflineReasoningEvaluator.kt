package com.example.Simtas.data.ml

import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import java.util.Locale

/**
 * Dedicated Offline Reasoning & Conceptual Evaluation Component.
 * Executes Step 4 of the question answering search hierarchy:
 * Evaluates conceptual reasoning, comparative differences (e.g. 1NF vs 2NF),
 * and verified foundational educational curriculum knowledge when a topic
 * is not directly answered in the lecture transcript or study notes.
 *
 * Strict rule:
 * Clearly distinguishes answers derived from general knowledge from lecture content.
 */
object OfflineReasoningEvaluator {

    // Comparative reasoning knowledge base for difference questions
    private val COMPARATIVE_REASONING_BASE = mapOf(
        "1nf and 2nf" to "1NF requires all attribute values to be atomic (indivisible) with no repeating groups. 2NF builds upon 1NF by eliminating partial functional dependencies, requiring every non-key attribute to be fully functionally dependent on the entire primary key.",
        "2nf and 3nf" to "2NF eliminates partial functional dependencies, whereas 3NF builds upon 2NF by eliminating transitive dependencies, ensuring non-key attributes depend solely on the candidate key and not on other non-key attributes.",
        "1nf and 3nf" to "1NF ensures atomicity of column values and eliminates repeating groups, while 3NF achieves a higher level of normalization by eliminating both partial and transitive dependencies.",
        "domestic and wild animals" to "Domestic animals are domesticated and cared for by humans for companionship, labor, or agriculture (e.g. dogs, cats, cows, horses), whereas wild animals survive independently in natural ecosystems without human domestication (e.g. lions, tigers, elephants, bears).",
        "supervised and unsupervised learning" to "Supervised learning trains predictive models on labeled datasets with known input-output pairs, while unsupervised learning discovers inherent patterns, groupings, and clusters from unlabeled data without pre-existing targets.",
        "stack and queue" to "A Stack operates on the LIFO (Last-In, First-Out) principle where elements are inserted and removed at the same top end. A Queue operates on the FIFO (First-In, First-Out) principle where elements are inserted at the rear (enqueue) and removed from the front (dequeue).",
        "process and thread" to "A Process is an executing instance of a program with its own independent memory address space and resources. A Thread is a lightweight unit of execution within a process that shares memory and resources with sibling threads.",
        "tcp and udp" to "TCP is a connection-oriented, reliable protocol that guarantees in-order packet delivery using acknowledgments and a 3-way handshake. UDP is a connectionless, lightweight protocol that provides low-latency, best-effort transmission without retransmissions.",
        "array and linked list" to "An Array stores elements in contiguous memory, offering O(1) index-based access but fixed capacity and O(n) insertions. A Linked List stores nodes in scattered memory linked by pointers, offering dynamic size and O(1) insertions at the head, but O(n) sequential traversal.",
        "dna and rna" to "DNA is a double-stranded molecule with deoxyribose sugar and Thymine for long-term genetic storage. RNA is single-stranded with ribose sugar and Uracil, translating genetic instructions into proteins.",
        "mitosis and meiosis" to "Mitosis is a single division producing 2 genetically identical diploid daughter cells for body growth and repair. Meiosis consists of 2 divisions producing 4 genetically diverse haploid gametes for sexual reproduction.",
        "compiler and interpreter" to "A Compiler translates entire source code into machine code before execution (e.g. C, C++). An Interpreter translates and executes code line-by-line at runtime (e.g. Python, JavaScript).",
        "primary key and foreign key" to "A Primary Key uniquely identifies every record in its own table and cannot contain NULL values. A Foreign Key references a Primary Key in another table, establishing relationships and enforcing referential integrity."
    )

    // Verified foundational curriculum definitions
    private val CURRICULUM_KNOWLEDGE_BASE = mapOf(
        "1nf" to "First Normal Form (1NF) is a relation property requiring all column values to be atomic (indivisible) with each row containing a unique set of attributes and no repeating groups.",
        "first normal form" to "First Normal Form (1NF) mandates that table attributes contain only atomic, single-valued entries, eliminating multi-valued attributes and repeating columns.",
        "2nf" to "Second Normal Form (2NF) requires a relation to be in 1NF and have no partial functional dependencies, meaning every non-prime attribute must depend on the whole candidate key.",
        "second normal form" to "Second Normal Form (2NF) enforces full functional dependency on the primary key, eliminating partial dependencies in composite-key relations.",
        "3nf" to "Third Normal Form (3NF) requires a relation to be in 2NF and have no transitive functional dependencies between non-key attributes (X → Y where neither is a candidate key).",
        "third normal form" to "Third Normal Form (3NF) ensures that every non-key attribute is non-transitively dependent on the primary key, preventing update and deletion anomalies.",
        "bcnf" to "Boyce-Codd Normal Form (BCNF) is a stricter version of 3NF where for every non-trivial functional dependency X → Y, X must be a superkey.",
        "database normalization" to "Database normalization is the process of organizing data in a relational database to reduce redundancy and improve data integrity.",
        "normalization" to "Database normalization is the process of structuring relational tables to eliminate insertion, update, and deletion anomalies while ensuring dependency preservation.",
        "data redundancy" to "Data redundancy occurs when the same piece of data is stored in multiple places within a database, leading to potential inconsistency and wasted storage.",
        "data integrity" to "Data integrity refers to the overall accuracy, completeness, and consistency of data throughout its lifecycle in a relational database.",
        "functional dependency" to "Functional dependency is a relationship between two attributes in a relational database where the value of one attribute uniquely determines the value of another.",
        "relational database" to "A relational database is a digital database based on the relational model, organizing data into structured tables consisting of rows and columns.",
        "domestic and wild animals" to "Domestic animals are animals kept, raised, or tamed by humans for companionship or agriculture (e.g. dogs, cats, cows, sheep, horses). Wild animals live independently in natural habitats without human care or domestication (e.g. lions, tigers, elephants, bears, deer).",
        "domestic animals" to "Domestic animals are species that have been selectively bred and adapted over generations to live in close association with humans for companionship, labor, or agriculture.",
        "wild animals" to "Wild animals are non-domesticated species that live and reproduce independently in natural ecosystems such as forests, plains, and oceans without human management.",
        "artificial intelligence" to "Artificial intelligence is the simulation of human intelligence processes by computer systems, encompassing learning, reasoning, and self-correction.",
        "ai" to "Artificial intelligence (AI) is the simulation of human intelligence processes by computer systems, encompassing learning, reasoning, and self-correction.",
        "machine learning" to "Machine learning is a branch of artificial intelligence focused on building applications that learn from data and improve accuracy over time without being explicitly programmed.",
        "deep learning" to "Deep learning is a subset of machine learning based on artificial neural networks with multiple layers to model complex patterns in data.",
        "photosynthesis" to "Photosynthesis is the biological process used by green plants and algae to convert light energy into chemical energy stored in glucose.",
        "cell" to "A cell is the fundamental structural, functional, and biological unit of all living organisms.",
        "dna" to "DNA (deoxyribonucleic acid) is the hereditary molecule that carries genetic instructions for development, functioning, growth, and reproduction in all organisms.",
        "ecosystem" to "An ecosystem is a biological community of interacting organisms and their physical environment functioning together as an ecological unit.",
        "gravity" to "Gravity is the fundamental physical force of attraction between all objects with mass or energy throughout the universe.",
        "newton's laws" to "Newton's laws of motion are three physical laws that describe the relationship between the motion of an object and the forces acting upon it.",
        "atom" to "An atom is the basic building block of matter, composed of protons, neutrons, and electrons.",
        "algorithm" to "An algorithm is a finite sequence of unambiguous instructions used to solve a specific problem or perform a computation.",
        "data structure" to "A data structure is a specialized format for organizing, processing, retrieving, and storing data efficiently in memory.",
        "operating system" to "An operating system is system software that manages computer hardware, software resources, and provides common services for computer programs.",
        "os" to "An operating system (OS) is system software that manages computer hardware, software resources, and provides common services for computer programs.",
        "cache" to "A cache is a high-speed hardware or software storage layer that stores a subset of data so that future requests for that data are served faster."
    )

    /**
     * Evaluates a question using offline reasoning and curriculum knowledge.
     */
    fun evaluate(question: String): DetectedQuestionAnswerItem? {
        val cleanQ = question.trim()
        val lower = cleanQ.lowercase(Locale.ROOT)
            .replace("?", "")
            .replace(",", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // 1. Check Difference / Comparison reasoning
        if (lower.contains("difference between") || lower.contains("compare") || lower.contains("versus") || lower.contains(" vs ")) {
            val diffAnswer = evaluateDifference(lower)
            if (diffAnswer != null) {
                return DetectedQuestionAnswerItem(
                    question = cleanQ,
                    answer = diffAnswer,
                    explanation = "Comparative reasoning based on verified curriculum principles.",
                    status = "Answered from General Knowledge",
                    isAnswered = true
                )
            }
        }

        // 2. Strip standard question prefixes
        val stripped = lower
            .replace(
                Regex("""^(what is|what are|why is|why are|how does|how do|explain|define|describe|clarify|tell me about|name\s+some|name|list\s+some|list|give\s+some|give|mention\s+some|mention|identify|state)\s+"""),
                ""
            )
            .trim()

        // 3. Direct key match in curriculum knowledge base
        CURRICULUM_KNOWLEDGE_BASE[stripped]?.let { definition ->
            return DetectedQuestionAnswerItem(
                question = cleanQ,
                answer = definition,
                explanation = "Verified general educational knowledge (not found in current lecture recording).",
                status = "Answered from General Knowledge",
                isAnswered = true
            )
        }

        // 4. Substring and keyword match
        for ((key, definition) in CURRICULUM_KNOWLEDGE_BASE) {
            if (stripped.contains(key) || key.contains(stripped)) {
                return DetectedQuestionAnswerItem(
                    question = cleanQ,
                    answer = definition,
                    explanation = "Verified general educational knowledge (not found in current lecture recording).",
                    status = "Answered from General Knowledge",
                    isAnswered = true
                )
            }
        }

        // 5. Multi-term match for composite queries
        val words = stripped.split(" ").filter { it.length > 2 && it !in setOf("some", "the", "and") }
        if (words.size >= 2) {
            for ((key, definition) in CURRICULUM_KNOWLEDGE_BASE) {
                val keyWords = key.split(" ").filter { it.length > 2 }
                if (keyWords.isNotEmpty() && keyWords.all { stripped.contains(it) }) {
                    return DetectedQuestionAnswerItem(
                        question = cleanQ,
                        answer = definition,
                        explanation = "Verified general educational knowledge (not found in current lecture recording).",
                        status = "Answered from General Knowledge",
                        isAnswered = true
                    )
                }
            }
        }

        // 6. Universal Student Knowledge Base (Computer Science, Physics, Chemistry, Biology)
        val universalAnswer = UniversalStudentKnowledgeBase.findKnowledge(cleanQ)
        if (universalAnswer != null) {
            return universalAnswer
        }

        return null
    }

    private fun evaluateDifference(lowerQuery: String): String? {
        for ((key, diffText) in COMPARATIVE_REASONING_BASE) {
            val terms = key.split(" and ")
            if (terms.size == 2) {
                if (lowerQuery.contains(terms[0]) && lowerQuery.contains(terms[1])) {
                    return diffText
                }
            }
        }
        return null
    }
}
