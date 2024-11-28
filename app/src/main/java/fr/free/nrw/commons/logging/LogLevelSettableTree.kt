package fr.free.nrw.commons.logging

/**
 * Can be implemented to set the log level for file tree
 */
interface LogLevelSettableTree {
    fun setLogLevel(logLevel: Int)
}