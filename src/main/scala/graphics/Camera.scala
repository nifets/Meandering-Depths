package graphics

import game._
import input._
import maths._
import scala.math._
class Camera(player: Player, gameInput: InputContext) {
    private val inputComponent = new InputComponent(gameInput,
        Map("PITCH_VIEW"    -> ((s: Double)=> {pitch(sensitivity* (s.toFloat - lastX)); lastX = s.toFloat}),
            "YAW_VIEW"      -> ((s: Double)=> {yaw(sensitivity* (lastY - s.toFloat)); lastY = s.toFloat}),
        )
    )

    val sensitivity = 0.3f
    //for yaw, pitch calculations
    var lastX = 0F
    var lastY = 0F

    //relative to defaultPos
    var pitchAngle = 0.0F
    var yawAngle = 0.0F

    //distance to player
    var distance = 2f
    //position of camera by default, relative to player orientation
    val defaultPos = Vector3(0f,0.2f,1f)

    //orientation in local player coordinates
    var lastOrientation = Quaternion.identity
    var currOrientation = Quaternion.identity

    //orientation of camera at time of rendering (interpolated between the last 2 updates)
    def orientation(alpha: Float) = lastOrientation.lerp(alpha, currOrientation).normalize

    def pitch(angle: Float) = {
        pitchAngle += angle
        if (pitchAngle > 89.0F)
            pitchAngle = 89.0F
        if (pitchAngle < -89.0F)
            pitchAngle = -89.0F
    }

    def yaw(angle: Float) = {
        yawAngle += angle
        if (yawAngle > 160.0F)
            yawAngle = 160.0F
        if (yawAngle < -160.0F)
            yawAngle = -160.0F
    }

    /** Get the view matrix at the time of rendering, by intepolating between the last 2 update states */
    def viewMatrix(alpha: Float): Matrix4 =
        (Matrix4.translation(-defaultPos * distance) //translate the space to move the camera behind the player
        * Matrix4.rotation(orientation(alpha)) //rotate the space to get view from camera
        * player.orientationMatrix(alpha).t //rotate the space to align axes to the local player ones
        * Matrix4.translation(-player.position(alpha))) //translate the space so that player is at origin

    def update(): Unit = {
        //if player is moving, camera is locked: yawing will instead cause the rotation of player's moving direction, pitching stays the same
        lastOrientation = currOrientation
        if (player.isMoving) {
            player.lastFront = player.currFront
            player.lastRight = player.currRight
            player.currFront = (player.currFront).rotate(Quaternion.axisAngleRotation(player.worldUp, yawAngle.toRadians)).normalize
            player.currRight = (player.currFront cross player.worldUp).normalize
            yawAngle = 0f
            currOrientation = Quaternion.axisAngleRotation(Vector3(1f,0f,0f), pitchAngle.toRadians)
        }
        else {
            currOrientation = Quaternion.axisAngleRotation(Vector3(1f,0f,0f), pitchAngle.toRadians) * Quaternion.axisAngleRotation(Vector3(0f,-1f,0f),yawAngle.toRadians)
        }
    }
}
