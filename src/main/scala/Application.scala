import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.opengl._
import org.lwjgl.system._

import java.nio._

import org.lwjgl.glfw.Callbacks._
import org.lwjgl.glfw.GLFW._
import org.lwjgl.opengl.GL11._
import org.lwjgl.system.MemoryStack._
import org.lwjgl.system.MemoryUtil._
import breeze.linalg._

import maths.GraphicsMaths._
import scala.math._

object Application {

  private var window: Long = 0

  private def init(): Unit = {
    // Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set()
		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW")

		// Configure GLFW
		glfwDefaultWindowHints() // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

		// Create the window
		window = glfwCreateWindow(1280, 720, "Hello World!", NULL, NULL)
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window")

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		/*glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		});*/

		// Get the thread stack and push a new frame
		val stack = stackPush(): MemoryStack
		val pWidth = stack.mallocInt(1): 	IntBuffer // int*
		val	pHeight = stack.mallocInt(1): IntBuffer  // int*

		// Get the window size passed to glfwCreateWindow
		glfwGetWindowSize(window, pWidth, pHeight);

		// Get the resolution of the primary monitor
		val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor()): GLFWVidMode

		// Center the window
		glfwSetWindowPos(window,(vidmode.width() - pWidth.get(0)) / 2,	(vidmode.height() - pHeight.get(0)) / 2)
		// the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window)
		// Enable v-sync
		glfwSwapInterval(1)

		// Make the window visible
		glfwShowWindow(window)

		    // This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities()

		// Set the clear color
		glClearColor(0.0f, 0.5f, 0.7f, 0.0f)
  }

  private def loop(): Unit = {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (!glfwWindowShouldClose(window)) {

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT) // clear the framebuffer

			glfwSwapBuffers(window) // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents()
		}
  }
  private def quit(): Unit = {
    // Free the window callbacks and destroy the window
		glfwFreeCallbacks(window)
		glfwDestroyWindow(window)

		// Terminate GLFW and free the error callback
		glfwTerminate()
		glfwSetErrorCallback(null).free()
  }


  def main(args: Array[String]): Unit = {
    //
    //Plan:
    //The world consists of (has) different components: static components (do not change by themselves)
    // and entities (dynamic components) that have a physical (hitbox) and visual component (model)
    // structures: terrain (caves), plants, buildings
    // entities: player, bats, etc.
    // the game loop consists of handling the input (player movement, camera movement, other actions)
    // then update the components of the world based on the input/ other things (world.update() )
    // then finally render everything (RenderManager.render(world))

    // some of the classes have fairly standard implementations (e.g. opengl stuff like shaders) but others
    // have more ways of handling the implementation so we will make them abstract/trait and extend them
    // with our implementation
    // will also try to use traits as much as possible and functional language
    // the world will be finite at first with possibility of making it infinite with the right
    // implementation
    // rendering will be done in chunks and hopefully switched to ray tracing at some point
    // storage of the world will also be a problem but could be handled with hashtables
    // but will have to see what information is stored exactly
    // the hard part is that i mean to make the structures editable (as in you can mine through the caves
    // and plants and structures etc if you have the right tools)
    // will also have to add a GUI as some point for inventory managing etc.
    //


    // game loop:
    // 1. handle input and AI -- e.g. player issues a jump and throws grappling hook, bat decides to dive
    // 2. update the world: player, entities, environment
    // 3. render
    var a = Vector3(1,0,0)
    var b = Vector3(0,1,0)
    println(cross(a,b))
    var c = Vector4(1,0,0,0)
    val dm = rotate(b, (2*math.Pi).toFloat)
    println(dm)
    println(dm * c)
    println(normalize(c))
    println(sin((toRadians(360))))
    println("Hello LWJGL " + Version.getVersion() + "!")
    init()
    loop()
    quit()

  }

}
