package core

import org.lwjgl.glfw._
import org.lwjgl.glfw.GLFW._

import java.nio._
import org.lwjgl.system._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL14._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL42._
import org.lwjgl.opengl.GL43._

import library.GLFW
import library._
import graphics.Window
import graphics._
import input.InputHandler
import input._
import game._
import scala.math._
import maths._

/** The main class for the game.*/
class MeanderingDepths extends FixedTimeStepLoop {
    
    /** The window through which the game is rendered.*/
    private val window = Window.create(1920, 1080, "Meandering Depths")
    
    
    /** Set up the input of the game */
    private val mainInput = InputHandler.createContext(window, new InputMapping {
        // Later on, the input mapping should be loaded from a config file, rather than be hard-coded here.
        override def getEventsfromInput(input: RawInput): Set[InputEvent] = {
            input match {
                case(Key(GLFW_KEY_W, GLFW_PRESS)) => Set(InputEvent("MOVE_FORWARD", 1F))
                case(Key(GLFW_KEY_W, GLFW_RELEASE)) => Set(InputEvent("MOVE_FORWARD", 0F))
                case(Key(GLFW_KEY_S, GLFW_PRESS)) => Set(InputEvent("MOVE_BACKWARD", 1F))
                case(Key(GLFW_KEY_S, GLFW_RELEASE)) => Set(InputEvent("MOVE_BACKWARD", 0F))
                case(Key(GLFW_KEY_A, GLFW_PRESS)) => Set(InputEvent("MOVE_LEFT", 1F))
                case(Key(GLFW_KEY_A, GLFW_RELEASE)) => Set(InputEvent("MOVE_LEFT", 0F))
                case(Key(GLFW_KEY_D, GLFW_PRESS)) => Set(InputEvent("MOVE_RIGHT", 1F))
                case(Key(GLFW_KEY_D, GLFW_RELEASE)) => Set(InputEvent("MOVE_RIGHT", 0F))
                case(Key(GLFW_KEY_SPACE, GLFW_PRESS)) => Set(InputEvent("MOVE_UP", 1F))
                case(Key(GLFW_KEY_SPACE, GLFW_RELEASE)) => Set(InputEvent("MOVE_UP", 0F))
                case(Key(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS)) => Set(InputEvent("MOVE_DOWN", 1F))
                case(Key(GLFW_KEY_LEFT_SHIFT, GLFW_RELEASE)) => Set(InputEvent("MOVE_DOWN", 0F))
                case(Key(GLFW_KEY_ESCAPE, GLFW_RELEASE)) => Set(InputEvent("EXIT_APPLICATION", 1F))
                case(Key(GLFW_KEY_P, GLFW_PRESS)) => Set(InputEvent("DESTROY", 1F))
                case(Key(GLFW_KEY_P, GLFW_RELEASE)) => Set(InputEvent("DESTROY", 0F))
                case (MouseCursorPos(xpos, ypos)) =>
                                    Set(InputEvent("PITCH_VIEW", ypos), InputEvent("YAW_VIEW", xpos))
                case _ => Set[InputEvent]()
            }
        }
    })
    
    /** This does not really belong here, and will be removed once a proper scene system is set up. */
    private val inputComponent = new InputComponent(mainInput,
        Map("EXIT_APPLICATION" -> ((_: Double)=> quit())
    ))
    
    
    
    /** Object responsible with the game logic. */
    val world = new World(mainInput)

    
    
    /** TEMPORARY -- Rendering stuff
        I have to set up a proper render manager that is in charge of setting up shaders and handling all the low level OpenGL tasks. Until then, this is all handled here... */
    
    //There is a lot of repetition with these 2 shader programs, especially since the fragment shader is the same for both. Later on I might merge them into a single program
    val renderProgram = ShaderProgram.vertexFragmentProgram(
        "shaders/staticVertexShader.glsl",
        "shaders/fragmentShader.glsl")

    val animationRenderProgram = ShaderProgram.vertexFragmentProgram(
        "shaders/animatedVertexShader.glsl",
        "shaders/fragmentShader.glsl")

    glEnable(GL_CULL_FACE)
    glEnable(GL_BLEND)
    glEnable(GL_DEPTH_TEST)

    val skyColour = Vector3(0.034f, 0.21f, 0.264f)

    val playerLight = new PointLight(Vector3(0f), 1f, 0.14f, 0.007f, Vector3(1f))

    glClearColor(skyColour.x, skyColour.y, skyColour.z, 1.0f)

    renderProgram.use()
    renderProgram.setUniform("projectionTransform", Matrix4.perspective(1.3F, 1920F/1080, 0.1F, 300.0F))
    renderProgram.setUniform("skyColour", skyColour)
    renderProgram.setUniform("numOfPointLights", 1)
    renderProgram.setUniform("materials", Terrain.materials)

    animationRenderProgram.use()
    animationRenderProgram.setUniform("projectionTransform", Matrix4.perspective(1.3F, 1920F/1080, 0.1F, 300.0F))
    animationRenderProgram.setUniform("skyColour", skyColour)
    animationRenderProgram.setUniform("numOfPointLights", 1)
    animationRenderProgram.setUniform("materials", Terrain.materials)
    //End of rendering stuff that doesn't belong in this class.
    
    
    
    // Run the main game until the app is quit (by the user or otherwise)
    loop()
    
    // Clear all the resources used
    dispose()

    
    
    
    // Use GLFW to get the event queue of raw input commands issued by the user, which are then converted into abstract input actions and are handled by the input components.
    override def input() = {
        InputHandler.pollEvents()
    }

    // Update game logic
    override def update(dt: Double) = {
        world.update(dt)
    }

    /** Again, most of the content of this function doesn't belong to this class. A render manager is supposed to handle this, using information from the world */
    // Render a new frame, interpolating the information from the last 2 ticks of game simulation
    override def render(alpha: Double) = {
        val a = alpha.toFloat
        
        // Clear the internal OpenGL buffer to allow rendering on a blank canvas
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        
        renderProgram.use()
        
        // Update camera location and orientation
        renderProgram.setUniform("viewTransform", world.view(a))
        renderProgram.setUniform("cameraPos", -world.view(a) * Vector4(0f, 0f, 0f, 1f))
        animationRenderProgram.setUniform("viewTransform", world.view(a))
        animationRenderProgram.setUniform("cameraPos", -world.view(a) * Vector4(0f, 0f, 0f, 1f))

        //This doesn't belong here OR in the rendering class. Instead, the light position should be incorporated in the game logic class (perhaps a member of the player class)
        playerLight.position = world.player.position(a) + world.player.front(a) * 2 + Vector3(0f,1f,0f) * 3

        renderProgram.setUniform("pointLights[0]", playerLight)
        animationRenderProgram.setUniform("pointLights[0]", playerLight)

        // This is ugly and overreaching. The player class shouldn't be accesible from here. But I don't have the infrastructure yet (render manager) to avoid this.
        world.player.render(a, animationRenderProgram)
        
        // This is likely how rendering will be done for everything, but instead of the renderable being in charge of the rendering, it will be the render manager who does it.
        for (r <- world.getRenderables)
            r.render(a, renderProgram)
        
        /** Swap the front and back buffer. The lack of parameters in this function is disingenious. The window and the OpenGL context (which is responsible for the rendering 
            pipeline) are coupled tightly internally. */
        window.updateScreen()
    }

    /** Clear up all the resources used by the application. */
    override def dispose() = {
        renderProgram.delete()
        animationRenderProgram.delete()
        window.destroy()
        super.dispose()
    }
}

object MeanderingDepths {
    //The main function of the application, creating a single instance of the game class, which manages itself upon construction (runs the game loop, disposes resources, etc.)
    def main(args: Array[String]) = {
        val game = new MeanderingDepths()
    }
}
