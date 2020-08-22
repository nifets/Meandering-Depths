package input

import org.lwjgl.glfw._
import org.lwjgl.glfw.GLFW._

import library.GLFW

import graphics.Window

import utils.Multimap

sealed trait RawInput
case class Key(key: Int, action: Int)                   extends RawInput
case class MouseButton(button: Int, action: Int)        extends RawInput
case class MouseCursorPos(xpos: Double, ypos: Double)   extends RawInput
case class MouseScroll(offset: Double)                  extends RawInput

case class InputEvent(name: String, value: Double)

/** Object in charge of getting the raw user input and sending it to input contexts*/
object InputHandler {


    /**
    case class Action(name: String)                extends InputEvent
    //This could also be achieved with 2 actions (one off and one on)
    case class State(name: String, state: Boolean) extends InputEvent
    //Range is normalized in [-1,1]
    case class Range(name: String, value: Double)  extends InputEvent
    */


    private val contexts = new Multimap[Window, InputContext]()

    def createContext(window: Window, mapping: InputMapping): InputContext = {
        val context = new InputContext(window, mapping)
        contexts += (window, context)
        context
    }

    def pollEvents() = glfwPollEvents()


    def setWindowCallbacks(window: Window, id: Long) = {
        glfwSetKeyCallback(id, new GLFWKeyCallback {
            override def invoke(w_id: Long, key: Int, sc: Int, action: Int, m: Int) =
                key_callback(window, w_id, key, sc, action, m)
        })
        glfwSetCursorPosCallback(id, new GLFWCursorPosCallback {
            override def invoke(w_id: Long, xpos: Double, ypos: Double) =
                cursor_position_callback(window, w_id, xpos, ypos)
        })
        glfwSetMouseButtonCallback(id, new GLFWMouseButtonCallback {
            override def invoke(w_id: Long, button: Int, action: Int, m: Int) =
                mouse_button_callback(window, w_id, button, action, m)
        })
        glfwSetScrollCallback(id, new GLFWScrollCallback {
            override def invoke(w_id: Long, xoffset: Double, yoffset: Double) =
                mouse_scroll_callback(window, w_id, xoffset, yoffset)
        })
    }

    private def key_callback(window: Window, w_id: Long, key: Int, sc: Int, action: Int, m: Int) = {
        for (context <- contexts(window))
            context.handle(Key(key, action))
    }

    private def cursor_position_callback(window: Window, w_id: Long, xpos: Double, ypos: Double) = {
        for (context <- contexts(window))
            context.handle(MouseCursorPos(xpos.toFloat, ypos.toFloat))
    }

    private def mouse_button_callback(window: Window, w_id: Long, button: Int, action: Int, m: Int) = {
        for (context <- contexts(window))
            context.handle(MouseButton(button, action))
    }

    private def mouse_scroll_callback(window: Window, w_id: Long, xoffset: Double, yoffset: Double) = {
        for (context <- contexts(window))
            context.handle(MouseScroll(yoffset))
    }
}
