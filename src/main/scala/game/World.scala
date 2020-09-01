package game

import breeze.linalg._
import input._
import scala.math._
import maths._
import graphics._

class World(gameInput: InputContext) {
    val player = new Player(gameInput)
    val camera = new Camera(player, gameInput)
    var currentTime = 0D
    val terrain = new Terrain()

    def view(alpha: Float): Matrix4 = camera.viewMatrix(alpha)

    def update(dt: Double) = {
        currentTime += dt
        player.update()
        camera.update()
        terrain.update(player.currPosition, dt)
    }

    def getRenderables: List[Renderable] = List(player) ++ terrain.getLoadedChunks

}
