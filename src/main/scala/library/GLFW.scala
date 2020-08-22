package library

import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.opengl._
import org.lwjgl.system._

import org.lwjgl.glfw.Callbacks._
import org.lwjgl.glfw.GLFW._

object GLFW {
    private var isRunning_ = false

    def isRunning = isRunning_

    def init() = {
        //Setup the error callback to print errors to System.err
        GLFWErrorCallback.createPrint(System.err).set()
        if (isRunning)
            println("GLFW ALREADY INITIALIZED")
        else {
            //Attempt to initialize GLFW
            if (!glfwInit())
                throw new IllegalStateException("Unable to initialize GLFW!");
            isRunning_ = true

        }
    }

    def quit() = {
        glfwTerminate()
        isRunning_ = false
    }

}
