package game

import breeze.linalg._
import input._
import scala.math._
import maths._

class World(gameInput: InputContext) {
    private val inputComponent = new InputComponent(gameInput,
        Map("MOVE_FORWARD"  -> ((s: Double)=> goingForward = s != 0.0),
            "MOVE_BACKWARD" -> ((s: Double)=> goingBackward = s != 0.0),
            "MOVE_LEFT"     -> ((s: Double)=> goingLeft = s != 0.0),
            "MOVE_RIGHT"    -> ((s: Double)=> goingRight = s != 0.0),
            "MOVE_UP"       -> ((s: Double)=> goingUp = s != 0.0),
            "MOVE_DOWN"     -> ((s: Double)=> goingDown = s != 0.0),
            "PITCH_VIEW"    -> ((s: Double)=> {pitch(sensitivity* (s.toFloat - lastX)); lastX = s.toFloat}),
            "YAW_VIEW"      -> ((s: Double)=> {yaw(-sensitivity* (lastY - s.toFloat)); lastY = s.toFloat}),
        )
    )

    val terrain = new Terrain()


    var goingForward = false
    var goingBackward = false
    var goingLeft = false
    var goingRight = false
    var goingUp = false
    var goingDown = false
    var prevPlayerPosition = Vector3(16F,16F,16F)
    var playerPosition = Vector3(16F,16F,16F)
    var lastX = 0F
    var lastY = 0F
    var sensitivity = 0.1F
    var front = Vector3(0F, 0F, -1F)
    var right = Vector3(1F, 0F, 0F)
    val trueUp = Vector3(0F, 1F, 0F)
    var up = Vector3(0F, 1F, 0F)
    var pitchAngle = 0.0F
    var yawAngle = 0.0F


    def pitch(angle: Float) = {
        pitchAngle += angle
        if (pitchAngle > 89.0F)
            pitchAngle = 89.0F
        if (pitchAngle < -89.0F)
            pitchAngle = -89.0F
    }

    def yaw(angle: Float) = {
        yawAngle -= angle
    }

    def view(alpha: Float): Matrix4 = {
        val pos = prevPlayerPosition.lerp(alpha, playerPosition)
        Matrix4(right.x, right.y, right.z, 0F,
                  up.x,    up.y,    up.z,    0F,
                  front.x, front.y, front.z, 0F,
                  0F,      0F,      0F,      1F).t * Matrix4.translation(-pos)
    }

    def update() = {
        if (goingForward)
            playerPosition -= front * 0.3F
        if (goingBackward)
            playerPosition += front * 0.3F
        if (goingLeft)
            playerPosition -= right * 0.3F
        if (goingRight)
            playerPosition += right * 0.3F
        if (goingUp)
            playerPosition += trueUp * 0.3F
        if (goingDown)
            playerPosition -= trueUp * 0.3F

        val dir = Vector3((cos(yawAngle.toRadians) * cos (pitchAngle.toRadians)).toFloat,
                          sin(pitchAngle.toRadians).toFloat,
                          (sin(yawAngle.toRadians) * cos (pitchAngle.toRadians)).toFloat)
        front = dir.normalize
        right = (front cross trueUp).normalize
        up = (right cross front).normalize
        prevPlayerPosition = playerPosition
        terrain.update(playerPosition)
    }

    def getRenderables = terrain.getLoadedChunks

}
