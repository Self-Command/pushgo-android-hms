package io.ethan.pushgo.update

/** Persists only the automatic failure notice; update evaluation always continues. */
internal class UpdateCheckFailureNoticeGate(
    private val lock: Any,
    private val hasNotified: () -> Boolean,
    private val markNotified: () -> Boolean,
) {
    fun shouldNotify(manual: Boolean): Boolean {
        // An explicit check must continue to return its result to the user.
        if (manual) return true
        return synchronized(lock) {
            if (hasNotified()) false else markNotified()
        }
    }
}
