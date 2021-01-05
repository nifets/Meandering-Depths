package core

import library.GLFW
import library._

/** Generic game loop that decouples the framerate of the game from the actual time simulated inside the game, so that the performance of the computer does not affect the game
    logic. It also ensures decoupling between the input handling, game logic and rendering, in MVC style. */

trait FixedTimeStepLoop {
    
    /** Desired number of ticks (updates of game logic) per second. This value should probably be set by the class implementing this trait, but this works for now. */
    val TARGET_TPS = 30
    
    private var isRunning = false

    /** GLFW is needed here for the timer, but there is no reason not to use the system's timer directly from some standard java/scala library, in order to avoid this
        dependence. To make this trait truly generic, this should be changed later. */
    GLFW.init()

    
    /** The actual fixed time loop, running the game. */
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

            /** Render an interpolated image between the previous and current game state
                Note: Ideally, the interpolation would be done between the current state and the next one, but this requires the extrapolation of that future state. By using the
                previous state, the extrapolation is not needed, but there will be some very slight latency (bounded by the tick duration). */
            val alpha = accumulator / tick
            render(alpha)
        }
    }
    
    /** What the game actually does is decided by these 3 functions, which need to be implemented. */
    protected def input(): Unit

    protected def update(dt: Double): Unit

    protected def render(alpha: Double): Unit

    def quit() = isRunning = false

    protected def dispose() = {
        GLFW.quit()
    }
}
