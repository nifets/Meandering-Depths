package core

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
import scala.math._
import maths.GraphicsMaths._
import graphics.Window
import core.Timer._
import logic._

//credit to SilverTiger for the lwjgl tutorial, gafferongames.com, sebastian lague, ...

class Application {

    private var window: Window = null

    private val timer = new Timer()

    private val state = new State()

    private var running = false

    def run() = {
        init()
        loop()
        quit()
    }

    private def init() = {
        /** Initialize GLFW */
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW!");

        /** Start timer */
        timer.start()

        /** Create window */
        window = new Window(720,480,"Hello world!")

        /** Update flag */
        running = true
    }

    private def loop() = {
        //Keeps track of the time not yet simulated
        var accumulator = 0D

        //Duration of a unit of game time in seconds. If the game runs smoothly, the number of ticks per second should be constant. This is used for game logic calculations.
        val tick = 1D / TargetTPS

        //The actual game loop
        while (running) {
            val delta = timer.getDelta()
            accumulator += delta
            
            //Handle user input; in the future: handle AI
            input()

            //Bring the simulation as up to date as possible
            while (accumulator >= tick) {
                update()
                accumulator -= tick
            }

            /**Render an interpolated image between the previous and current game state
            Note: Ideally, the interpolation would be done between the current state and the next one, but this requires the extrapolation of that future state. By using the previous state, the extrapolation is not needed, but there will be some very slight latency (bounded by the tick duration). */
            val alpha = accumulator / tick
            render(alpha)

        }
    }

    private def input() = {

    }

    private def update() = {
        state.update()
    }
    private def render(alpha: Double) = {
        state.render(alpha)
        window.update()
        if (window.isClosing)
            running = false
    }

    private def quit() = {
        window.destroy()
        glfwTerminate()
    }

}

object Application {

    def main(args: Array[String]): Unit = {
        val app = new Application()
        app.run()
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
  }

}
