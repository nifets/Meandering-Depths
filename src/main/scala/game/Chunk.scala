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
import org.lwjgl.opengl.GL45._

import org.lwjgl.system.MemoryStack._
import org.lwjgl.system.MemoryUtil._

import scala.math._
import utils.MarchingCubes
import graphics._
import library._
import library.Timer
import maths._
import physics._

class Chunk(val pos: Vector3i, vbo: Int, val vertexCount: Int, val collisionBox: ChunkCollisionMesh) extends Renderable {

    val offset = (pos * Chunk.SIZE).toVector3
    val worldTransform = Matrix4.translation(offset)
    val normalTransform = worldTransform.t.inv

    //SET UP VAO
    val vao = glGenVertexArrays()
    glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)

    //vertex positions attribute
    glVertexAttribPointer(0, 3, GL_FLOAT, false, 8*4, 0)
    glEnableVertexAttribArray(0)
    //vertex material id attribute
    glVertexAttribPointer(1, 1, GL_FLOAT, false, 8*4, 3*4)
    glEnableVertexAttribArray(1)
    //vertex normal attribute
    glVertexAttribPointer(2, 3, GL_FLOAT, false, 8*4, 4*4)
    glEnableVertexAttribArray(2)
    //vertex colour variation attribute
    glVertexAttribPointer(3, 1, GL_FLOAT, false, 8*4, 7*4)
    glEnableVertexAttribArray(3)

    //println(vertexCount)

    override def render(alpha: Float, shader: ShaderProgram) = {
        glBindVertexArray(vao)
        shader.setUniform("worldTransform", worldTransform)
        shader.setUniform("normalTransform", normalTransform.inv.t)
        glDrawArrays(GL_TRIANGLES, 0, vertexCount)
    }

    def dispose() = {
        glDeleteBuffers(vbo)
        glDeleteVertexArrays(vao)
    }
}

object Chunk {
    val SIZE = 20
    val FLOATS_IN_TRIANGLE = 24
}
