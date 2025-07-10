package za.co.synthesis.halo.multiactivitytemplate

class Timer {
    private var startTime: Long = 0
    private var endTime: Long = 0

    /**
     * Start the timer
     */
    fun start() {
        startTime = System.currentTimeMillis()
    }

    /**
     * End the timer and return the elapsed time
     */
    fun end(): Long {
        endTime = System.currentTimeMillis()
        return endTime - startTime
    }
}
