package core

import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.glfw.GLFW.glfwSetTime

/** Facade for the GLFW Timer*/
class Timer {
    private var lastTime = 0D

    def start() = glfwSetTime(0)

    def getTime() = glfwGetTime()


    /** Returns the time elapsed since the last getDelta call */
    def getDelta() = {
        var currentTime = glfwGetTime()
        val delta = currentTime - lastTime
        lastTime = currentTime
        delta
    }
}

object Timer {
    val TargetTPS = 30
}
