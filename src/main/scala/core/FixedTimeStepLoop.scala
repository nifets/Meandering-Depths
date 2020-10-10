package core

import library.GLFW
import library._

trait FixedTimeStepLoop {
    val TARGET_TPS = 30
    private var isRunning = false

    GLFW.init()

    final def loop() = {
        val loopTimer = new Timer()
        loopTimer.start()
        isRunning = true
        //Keeps track of the time not yet simulated
        var accumulator = 0D

        //Duration of a unit of game time in seconds. If the game runs smoothly, the number of ticks per second should be constant. This is used for game logic calculations.
        val tick = 1D / TARGET_TPS

        //Main game loop
        while (isRunning) {
            //Time elapsed since last frame
            val delta = loopTimer.getDelta()
            accumulator += delta

            //Handle input (user and/or AI)
            input()

            //Bring the simulation as up to date as possible
            while (accumulator >= tick) {
                update(tick)
                accumulator -= tick
            }

            /**Render an interpolated image between the previous and current game state
            Note: Ideally, the interpolation would be done between the current state and the next one, but this requires the extrapolation of that future state. By using the previous state, the extrapolation is not needed, but there will be some very slight latency (bounded by the tick duration). */
            val alpha = accumulator / tick
            render(alpha)
        }
    }
    protected def input(): Unit

    protected def update(dt: Double): Unit

    protected def render(alpha: Double): Unit

    def quit() = isRunning = false

    protected def dispose() = {
        GLFW.quit()
    }
}
