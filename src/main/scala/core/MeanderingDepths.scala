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

class MeanderingDepths extends FixedTimeStepLoop {
    private val window = Window.create(1920, 1080, "Meandering Depths")
    private val mainInput = InputHandler.createContext(window, new InputMapping {
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
    private val inputComponent = new InputComponent(mainInput,
        Map("EXIT_APPLICATION" -> ((_: Double)=> quit())
    ))
    val world = new World(mainInput)

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

    loop()
    dispose()

    override def input() = {
        InputHandler.pollEvents()
    }

    override def update(dt: Double) = {
        world.update(dt)
    }

    override def render(alpha: Double) = {
        val a = alpha.toFloat
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        renderProgram.use()
        renderProgram.setUniform("viewTransform", world.view(a))
        renderProgram.setUniform("cameraPos", -world.view(a) * Vector4(0f, 0f, 0f, 1f))
        animationRenderProgram.setUniform("viewTransform", world.view(a))
        animationRenderProgram.setUniform("cameraPos", -world.view(a) * Vector4(0f, 0f, 0f, 1f))

        playerLight.position = world.player.position(a) + world.player.front(a) * 2 + Vector3(0f,1f,0f) * 3

        renderProgram.setUniform("pointLights[0]", playerLight)
        animationRenderProgram.setUniform("pointLights[0]", playerLight)

        world.player.render(a, animationRenderProgram)
        for (r <- world.getRenderables)
            r.render(a, renderProgram)
        window.updateScreen()
    }

    override def dispose() = {
        renderProgram.delete()
        window.destroy()
        super.dispose()
    }
}

object MeanderingDepths {
    def main(args: Array[String]) = {
        val game = new MeanderingDepths()
    }
}
