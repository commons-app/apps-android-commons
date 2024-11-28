package fr.free.nrw.commons.logging

import android.util.Log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Locale
import java.util.concurrent.Executor

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import timber.log.Timber


/**
 * Extends Timber's debug tree to write logs to a file.
 */
class FileLoggingTree(
    private var logLevel: Int,
    private val logFileName: String,
    logDirectory: String,
    private val fileSizeInKb: Int,
    private val executor: Executor
) : Timber.DebugTree(), LogLevelSettableTree {

    private val logger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    private lateinit var rollingPolicy: FixedWindowRollingPolicy

    init {
        configureLogger(logDirectory)
    }

    /**
     * Can be overridden to change the file's log level.
     * @param logLevel The new log level.
     */
    override fun setLogLevel(logLevel: Int) {
        this.logLevel = logLevel
    }

    /**
     * Checks and logs any message.
     * @param priority The priority of the log message.
     * @param tag The tag associated with the log message.
     * @param message The log message.
     * @param t An optional throwable.
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        executor.execute {
            logMessage(priority, tag.orEmpty(), message)
        }
    }

    /**
     * Logs a message based on the priority.
     * @param priority The priority of the log message.
     * @param tag The tag associated with the log message.
     * @param message The log message.
     */
    private fun logMessage(priority: Int, tag: String, message: String) {
        val messageWithTag = "[$tag] : $message"
        when (priority) {
            Log.VERBOSE -> logger.trace(messageWithTag)
            Log.DEBUG -> logger.debug(messageWithTag)
            Log.INFO -> logger.info(messageWithTag)
            Log.WARN -> logger.warn(messageWithTag)
            Log.ERROR, Log.ASSERT -> logger.error(messageWithTag)
        }
    }

    /**
     * Checks if a particular log line should be logged in the file or not.
     * @param priority The priority of the log message.
     * @return True if the log message should be logged, false otherwise.
     */
    @Deprecated("Deprecated in Java")
    override fun isLoggable(priority: Int): Boolean {
        return priority >= logLevel
    }

    /**
     * Configures the logger with a file size rolling policy (SizeBasedTriggeringPolicy).
     * https://github.com/tony19/logback-android/wiki
     * @param logDir The directory where logs should be stored.
     */
    private fun configureLogger(logDir: String) {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.reset()

        val rollingFileAppender = RollingFileAppender<ILoggingEvent>().apply {
            context = loggerContext
            file = "$logDir/$logFileName.0.log"
        }

        rollingPolicy = FixedWindowRollingPolicy().apply {
            context = loggerContext
            minIndex = 1
            maxIndex = 4
            setParent(rollingFileAppender)
            fileNamePattern = "$logDir/$logFileName.%i.log"
            start()
        }

        val triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>().apply {
            context = loggerContext
            maxFileSize = "$fileSizeInKb"
            start()
        }

        val encoder = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = "%-27(%date{ISO8601}) [%-5level] [%thread] %msg%n"
            start()
        }

        rollingFileAppender.apply {
            this.encoder = encoder
            rollingPolicy = rollingPolicy
            this.triggeringPolicy = triggeringPolicy
            start()
        }

        val rootLogger = LoggerFactory.getLogger(
            Logger.ROOT_LOGGER_NAME
        ) as ch.qos.logback.classic.Logger
        rootLogger.addAppender(rollingFileAppender)
    }
}
