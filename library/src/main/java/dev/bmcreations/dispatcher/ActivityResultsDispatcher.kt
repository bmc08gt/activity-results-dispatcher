package dev.bmcreations.dispatcher

interface ActivityResultDispatcher<S> {
    fun onSuccess(result: S? = null)
    fun onFailure(error: Throwable)
    fun onCancel()
}
