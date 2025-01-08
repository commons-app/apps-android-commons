package fr.free.nrw.commons.concurrency

interface ExceptionHandler {

    fun onException(t: Throwable)

}