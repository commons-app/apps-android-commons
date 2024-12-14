package fr.free.nrw.commons.utils

import android.os.Handler
import android.os.Looper

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ExecutorUtils {

    @JvmStatic
    private val uiExecutor: Executor = Executor { command ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            command.run()
        } else {
            Handler(Looper.getMainLooper()).post(command)
        }
    }

    @JvmStatic
    fun uiExecutor(): Executor {
        return uiExecutor
    }

    @JvmStatic
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)

    @JvmStatic
    fun get(): ExecutorService {
        return executor
    }
}
