package fr.free.nrw.commons.logging;

import android.util.Log;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.Executor;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import timber.log.Timber;

/**
 * Extends Timber's debug tree to write logs to a file
 */
public class FileLoggingTree extends Timber.DebugTree implements LogLevelSettableTree {
    private final Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private int logLevel;
    private final String logFileName;
    private int fileSize;
    private FixedWindowRollingPolicy rollingPolicy;
    private final Executor executor;

    public FileLoggingTree(int logLevel,
                           String logFileName,
                           String logDirectory,
                           int fileSizeInKb,
                           Executor executor) {
        this.logLevel = logLevel;
        this.logFileName = logFileName;
        this.fileSize = fileSizeInKb;
        configureLogger(logDirectory);
        this.executor = executor;
    }

    /**
     * Can be overridden to change file's log level
     * @param logLevel
     */
    @Override
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Check and log any message
     * @param priority
     * @param tag
     * @param message
     * @param t
     */
    @Override
    protected void log(final int priority, final String tag, @NonNull final String message, Throwable t) {
        executor.execute(() -> logMessage(priority, tag, message));

    }

    /**
     * Log any message based on the priority
     * @param priority
     * @param tag
     * @param message
     */
    private void logMessage(int priority, String tag, String message) {
        String messageWithTag = String.format("[%s] : %s", tag, message);
        switch (priority) {
            case Log.VERBOSE:
                logger.trace(messageWithTag);
                break;
            case Log.DEBUG:
                logger.debug(messageWithTag);
                break;
            case Log.INFO:
                logger.info(messageWithTag);
                break;
            case Log.WARN:
                logger.warn(messageWithTag);
                break;
            case Log.ERROR:
                logger.error(messageWithTag);
                break;
            case Log.ASSERT:
                logger.error(messageWithTag);
                break;
        }
    }

    /**
     * Checks if a particular log line should be logged in the file or not
     * @param priority
     * @return
     */
    @Override
    protected boolean isLoggable(int priority) {
        return priority >= logLevel;
    }

    /**
     * Configures the logger with a file size rolling policy (SizeBasedTriggeringPolicy)
     * https://github.com/tony19/logback-android/wiki
     * @param logDir
     */
    private void configureLogger(String logDir) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
        rollingFileAppender.setContext(loggerContext);
        rollingFileAppender.setFile(logDir + "/" + logFileName + ".0.log");

        rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(4);
        rollingPolicy.setParent(rollingFileAppender);
        rollingPolicy.setFileNamePattern(logDir + "/" + logFileName + ".%i.log");
        rollingPolicy.start();

        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
        triggeringPolicy.setContext(loggerContext);
        triggeringPolicy.setMaxFileSize(String.format(Locale.ENGLISH, "%dKB", fileSize));
        triggeringPolicy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%-27(%date{ISO8601}) [%-5level] [%thread] %msg%n");
        encoder.start();

        rollingFileAppender.setEncoder(encoder);
        rollingFileAppender.setRollingPolicy(rollingPolicy);
        rollingFileAppender.setTriggeringPolicy(triggeringPolicy);
        rollingFileAppender.start();
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(rollingFileAppender);
    }
}
