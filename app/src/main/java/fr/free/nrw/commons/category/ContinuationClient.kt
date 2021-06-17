package fr.free.nrw.commons.category

import io.reactivex.Single


abstract class ContinuationClient<Network, Domain> {
    private val continuationStore: MutableMap<String, Map<String, String>?> = mutableMapOf()
    private val continuationExists: MutableMap<String, Boolean> = mutableMapOf()

    private fun hasMorePagesFor(key: String) = continuationExists[key] ?: true
    fun continuationRequest(
        prefix: String,
        name: String,
        requestFunction: (Map<String, String>) -> Single<Network>
    ): Single<List<Domain>> {
        val key = "$prefix$name"
        return if (hasMorePagesFor(key)) {
            responseMapper(requestFunction(continuationStore[key] ?: emptyMap()), key)
        } else {
            Single.just(emptyList())
        }
    }

    abstract fun responseMapper(networkResult: Single<Network>, key: String?=null): Single<List<Domain>>

    fun handleContinuationResponse(continuation:Map<String,String>?, key:String?){
        if (key != null) {
            continuationExists[key] =
                continuation?.let { continuation ->
                    continuationStore[key] = continuation
                    true
                } ?: false
        }
    }

    protected fun resetContinuation(prefix: String, category: String) {
        continuationExists.remove("$prefix$category")
        continuationStore.remove("$prefix$category")
    }

    /**
     * Remove the existing the key from continuationExists and continuationStore
     *
     * @param prefix
     * @param userName the username
     */
    protected fun resetUserContinuation(prefix: String, userName: String) {
        continuationExists.remove("$prefix$userName")
        continuationStore.remove("$prefix$userName")
    }

}
