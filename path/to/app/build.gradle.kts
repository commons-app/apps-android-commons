# Import necessary libraries
import org.gradle.api.tasks.TaskProvider

// Add a task to run the static check at build time
tasks.register("checkQids") {
    doLast {
        val qids = resources.getStringArray(R.array.q_ids)
        val sortedQids = qids.sorted()
        if (sortedQids.joinToString() != qids.joinToString()) {
            throw RuntimeException("QIDs are not sorted correctly")
        }
    }
}

// Add a dependency to the checkQids task
tasks.checkQids.dependsOn(tasks.compileJava)