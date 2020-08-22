package library

import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.glfw.GLFW.glfwSetTime

/** Facade for the GLFW Timer */
class Timer {
    private var startTime = 0D
    private var lastTime = 0D
    private var isRunning = false


    //Must call this method to use the timer
    def start() = {
        require(GLFW.isRunning)
        startTime = glfwGetTime()
        isRunning = true
    }

    def getTime() = {
        if(isRunning)
            glfwGetTime() - startTime
        else
            -1D
    }

    def getLowPrecisionTime() = ((getTime() * 1000).toInt).toDouble / 1000

    /** Returns the time elapsed since the last getDelta call for this timer object*/
    def getDelta() = {
        var currentTime = getTime()
        val delta = currentTime - lastTime
        lastTime = currentTime
        delta
    }
}