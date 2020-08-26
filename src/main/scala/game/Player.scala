package game

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

import org.lwjgl.system.MemoryStack._
import org.lwjgl.system.MemoryUtil._

import input._
import maths._
import graphics._

class Player(gameInput: InputContext) extends Renderable {
    private val inputComponent = new InputComponent(gameInput,
        Map("MOVE_FORWARD"  -> ((s: Double)=> goingForward = s != 0.0),
            "MOVE_BACKWARD" -> ((s: Double)=> goingBackward = s != 0.0),
            "MOVE_LEFT"     -> ((s: Double)=> goingLeft = s != 0.0),
            "MOVE_RIGHT"    -> ((s: Double)=> goingRight = s != 0.0),
            "MOVE_UP"       -> ((s: Double)=> goingUp = s != 0.0),
            "MOVE_DOWN"     -> ((s: Double)=> goingDown = s != 0.0),
        )
    )
    //A lot of repetition and ugly code here, should rewrite at some point
    var lastPosition = Vector3(0f)
    var currPosition = Vector3(0f)
    var lastFront = Vector3(0f,0f,-1f)
    var currFront = Vector3(0f,0f,-1f)
    var lastRight = Vector3(1f,0f,0f)
    var currRight = Vector3(1f,0f,0f)

    def position(alpha: Float) = lastPosition.lerp(alpha, currPosition)
    def front(alpha: Float) = lastFront.lerp(alpha, currFront).normalize
    def right(alpha: Float) = lastRight.lerp(alpha, currRight).normalize


    val worldUp = Vector3(0f,1f,0f)

    //TODO: SET UP PROPER FLAG SYSTEM
    var goingForward = false
    var goingBackward = false
    var goingLeft = false
    var goingRight = false
    var goingUp = false
    var goingDown = false
    def isMoving = goingForward || goingBackward || goingLeft || goingRight || goingUp || goingDown

    //simple cube mesh for now, will replace with something more complex later on
    val meshVao = {
        val vertices = Array(-2f,-3f,-2f, 20f, 0.2f, 0.7f, 0f, 20f, -2f,-3f,-2f,20f,
                              2f,-3f,-2f, 20f, 0.2f, 0.7f, 0f, 20f,  2f,-3f,-2f,20f,
                             -2f, 3f,-2f, 20f, 0.2f, 0.7f, 0f, 20f, -2f, 3f,-2f,20f,
                              2f, 3f,-2f, 20f, 0.2f, 0.7f, 0f, 20f,  2f, 3f, 2f,20f,
                             -2f,-3f, 2f, 20f, 1f, 1f, 1f, 20f, -2f,-3f, 2f,20f,
                              2f,-3f, 2f, 20f, 1f, 1f, 1f, 20f,  2f,-3f, 2f,20f,
                              -2f, 3f, 2f, 20f, 1f, 1f, 1f, 20f,-2f, 3f, 2f,20f,
                               2f, 3f, 2f, 20f, 1f, 1f, 1f, 20f, 2f, 3f, 2f,20f)

        val indices = Array(0,2,1, 1,2,3, 0,4,2, 2,4,6, 0,1,4, 1,5,4,
                            2,6,3, 3,6,7, 3,7,1, 1,7,5, 7,4,5, 7,6,4)

        val vao = glGenVertexArrays()
        glBindVertexArray(vao)

        val vbo = glGenBuffers
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val stack = MemoryStack.stackPush()
        val verticesBuffer = stack.mallocFloat(vertices.length)
        verticesBuffer.put(vertices).flip()
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW)

        val ebo = glGenBuffers
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indicesBuffer = stack.mallocInt(indices.length)
        indicesBuffer.put(indices).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW)
        MemoryStack.stackPop()

        //vertex positions attribute
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 12*4, 0)
        glEnableVertexAttribArray(0)

        //vertex colour attribute
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 12*4, 4*4)
        glEnableVertexAttribArray(1)

        //vertex normal attribute
        glVertexAttribPointer(2, 4, GL_FLOAT, false, 12*4, 8*4)
        glEnableVertexAttribArray(2)


        vao
    }

    val vertexCount = 36

    def render(alpha: Float, shader: ShaderProgram) = {
        glBindVertexArray(meshVao)
        val m = worldTransform(alpha)
        shader.setUniform("worldTransform", m)
        //TO DO: change to normal matrix
        shader.setUniform("normalTransform", m)
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0)
    }

    //rotate player based on orientation then translate to current position
    def worldTransform(alpha: Float): Matrix4 = Matrix4.translation(position(alpha)) * orientationMatrix(alpha) * Matrix4.scaling(2f,2f,2f)

    def orientationMatrix(alpha: Float): Matrix4 = Matrix3(right(alpha), worldUp, -front(alpha)).expand

    def update() = {
        lastPosition = currPosition

        if (goingForward)
            currPosition += currFront * 0.3F
        if (goingBackward)
            currPosition -= currFront * 0.3F
        if (goingLeft)
            currPosition -= currRight * 0.3F
        if (goingRight)
            currPosition += currRight * 0.3F
        if (goingUp)
            currPosition += worldUp * 0.3F
        if (goingDown)
            currPosition -= worldUp * 0.3F

    }
}
