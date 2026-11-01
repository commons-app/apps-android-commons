package fr.free.nrw.commons.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ExecutorUtils {
    private const val THREAD_POOL_SIZE = 3

    @JvmStatic
    private val uiExecutor: Executor =
        Executor { command ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                command.run()
            } else {
                Handler(Looper.getMainLooper()).post(command)
            }
        }

    @JvmStatic
    fun uiExecutor(): Executor = uiExecutor

    @JvmStatic
    private val executor: ExecutorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

    @JvmStatic
    fun get(): ExecutorService = executor
}
