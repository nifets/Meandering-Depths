package graphics

import org.lwjgl._
import org.lwjgl.system.MemoryUtil._
import org.lwjgl.glfw._
import org.lwjgl.glfw.Callbacks._
import org.lwjgl.glfw.GLFW._
import org.lwjgl.opengl._
import org.lwjgl.system._
import org.lwjgl.opengl.GL11._

/**Facade for a GLFW window/OpenGL context*/
class Window(width: Int, height: Int, title: String) {

    private val id = glfwCreateWindow(width, height, title, NULL, NULL)

    if (id == NULL) {
      glfwTerminate()
      throw new RuntimeException("Failed to create the GLFW window!")
    }



    //Create OpenGL context -- this "syncs" OpenGL with the GLFW context (i think?)
    glfwMakeContextCurrent(id)
    GL.createCapabilities()

    //Set the clear colour
    glClearColor(1.0f, 0.0f, 0.0f, 0.0f)

    def isClosing = glfwWindowShouldClose(id)

    def update() = {
        glfwSwapBuffers(id)
        glfwPollEvents()
    }

    def destroy() ={
        glfwDestroyWindow(id)
    }


}
