package fr.free.nrw.commons.logging

import android.util.Log
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.slf4j.Logger
import java.lang.reflect.Method
import java.util.concurrent.Executor

class FileLoggingTreeUnitTest {

    private lateinit var fileLoggingTree: FileLoggingTree

    @Mock
    private lateinit var executor: Executor

    @Mock
    private lateinit var logger: Logger

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        fileLoggingTree = FileLoggingTree(
            Log.VERBOSE,
            "test",
            "test",
            1000,
            executor
        )
        Whitebox.setInternalState(fileLoggingTree, "logger", logger)
    }

    @Test
    fun testSetLogLevel() {
        var logLevel = Log.VERBOSE
        Whitebox.setInternalState(fileLoggingTree, "logLevel", logLevel)
        fileLoggingTree.setLogLevel(Log.DEBUG)
        logLevel = Log.DEBUG
        assertEquals(logLevel, logLevel)
    }

    @Test
    fun testLog() {
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "log", Int::class.java, String::class.java, String::class.java, Throwable::class.java
        )
        method.isAccessible = true
        method.invoke(fileLoggingTree, 0, "test", "test", Throwable())
    }

    @Test
    fun testLogMessageCaseVERBOSE() {
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "logMessage", Int::class.java, String::class.java, String::class.java
        )
        method.isAccessible = true
        method.invoke(fileLoggingTree, Log.VERBOSE, "test", "test")
        val messageWithTag = String.format("[%s] : %s", "test", "test")
        verify(logger, times(1)).trace(messageWithTag)
    }

    @Test
    fun testLogMessageCaseDEBUG() {
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "logMessage", Int::class.java, String::class.java, String::class.java
        )
        method.isAccessible = true
        method.invoke(fileLoggingTree, Log.DEBUG, "test", "test")
        val messageWithTag = String.format("[%s] : %s", "test", "test")
        verify(logger, times(1)).debug(messageWithTag)
    }

    @Test
    fun testLogMessageCaseINFO() {
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "logMessage", Int::class.java, String::class.java, String::class.java
        )
        method.isAccessible = true
        method.invoke(fileLoggingTree, Log.INFO, "test", "test")
        val messageWithTag = String.format("[%s] : %s", "test", "test")
        verify(logger, times(1)).info(messageWithTag)
    }

    @Test
    fun testLogMessageCaseWARN() {
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "logMessage", Int::class.java, String::class.java, String::class.java
        )
        method.isAccessible = true
        method.invoke(fileLoggingTree, Log.WARN, "test", "test")
        val messageWithTag = String.format("[%s] : %s", "test", "test")
        verify(logger, times(1)).warn(messageWithTag)
    }

    @Test
    fun testLogMessageCaseERROR() {
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "logMessage", Int::class.java, String::class.java, String::class.java
        )
        method.isAccessible = true
        method.invoke(fileLoggingTree, Log.ERROR, "test", "test")
        val messageWithTag = String.format("[%s] : %s", "test", "test")
        verify(logger, times(1)).error(messageWithTag)
    }

    @Test
    fun testLogMessageCaseASSERT() {
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "logMessage", Int::class.java, String::class.java, String::class.java
        )
        method.isAccessible = true
        method.invoke(fileLoggingTree, Log.ASSERT, "test", "test")
        val messageWithTag = String.format("[%s] : %s", "test", "test")
        verify(logger, times(1)).error(messageWithTag)
    }

    @Test
    fun testIsLoggableCaseTrue() {
        Whitebox.setInternalState(fileLoggingTree, "logLevel", Log.VERBOSE)
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "isLoggable", Int::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(fileLoggingTree, Log.ASSERT), true)
    }

    @Test
    fun testIsLoggableCaseFalse() {
        Whitebox.setInternalState(fileLoggingTree, "logLevel", Log.ASSERT)
        val method: Method = FileLoggingTree::class.java.getDeclaredMethod(
            "isLoggable", Int::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(fileLoggingTree, Log.VERBOSE), false)
    }

}