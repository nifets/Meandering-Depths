package graphics

import org.lwjgl._
import org.lwjgl.system.MemoryUtil._
import org.lwjgl.glfw._
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.Callbacks._
import org.lwjgl.opengl._
import org.lwjgl.system._
import org.lwjgl.opengl.GL11._

import scala.collection.mutable.Map

import library.GLFW

import input.InputHandler

/**Facade for a double-buffered GLFW window/OpenGL context*/
class Window private(width: Int, height: Int, title: String) {
    private var id = genGLFWwindowID()
    private lazy val capabilities = GL.createCapabilities()
    setCurrent()
    setCallbacks()
    //use vsync
    glfwSwapInterval(1)

    glfwSetInputMode(id, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

    def isClosing = glfwWindowShouldClose(id)

    /** Update what is shown on the window, by swapping the back buffer (where the rendering is done between frames) with the front buffer (what is actually shown on screen). */
    def updateScreen() = glfwSwapBuffers(id)


    //Set the window as the current context
    def setCurrent() = {
        glfwMakeContextCurrent(id)
        GL.setCapabilities(capabilities)
    }

    def destroy() = {
        glfwFreeCallbacks(id)
        glfwDestroyWindow(id)
        GL.setCapabilities(null)
        Window.windowMap -= id
        GL.destroy()
    }

    private def setCallbacks() = InputHandler.setWindowCallbacks(this, id)

    private def genGLFWwindowID(): Long = {
        val id = glfwCreateWindow(width, height, title, NULL, NULL)
        if (id == NULL) {
          GLFW.quit()
          throw new RuntimeException("Failed to create the GLFW window!")
        }

        id
    }

}

object Window {
    private val windowMap = Map[Long, Window]()
    def create(width: Int, height: Int, title: String): Window = {
        val window = new Window(width, height, title)
        windowMap += (window.id -> window)
        window
    }

    def getWindowFromId(id: Long): Option[Window] = windowMap.get(id)
}
