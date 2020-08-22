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

import scala.math._
import utils.MarchingCubes
import graphics._
import library._
import library.Timer
import maths._

class Chunk(val pos: Vector3, inputData: FloatBuffer, computeShader: ComputeShaderProgram) extends Renderable {

    val offset = pos * Chunk.SIZE.toFloat
    var textureLoad = 0D
    var compShader = 0D
    var vaoSetup = 0D
    val worldTransform = Matrix4.translation(offset)
    val normalTransform = worldTransform.t.inv

    val (vbo, vao, vertexCount) = load()

    def load() = {
        val profiler = new Timer()
        profiler.start()

        //BIND INPUT DATA TO IMAGE BINDING 0

        val tex_input = glGenTextures()
        glBindTexture(GL_TEXTURE_3D, tex_input)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Chunk.SIZE+ 1, Chunk.SIZE+ 1, Chunk.SIZE+ 1, 0, GL_RGBA, GL_FLOAT, inputData)
        MemoryUtil.memFree(inputData)
        glBindImageTexture(0, tex_input, 0, false, 0, GL_READ_ONLY, GL_RGBA32F)

        textureLoad = profiler.getDelta()

        //SET UP OUTPUT BUFFER FOR COMPUTE SHADER
        val ssbo = glGenBuffers()
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
        glBufferData(GL_SHADER_STORAGE_BUFFER, Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * Chunk.FLOATS_IN_TRIANGLE * MarchingCubes.MAX_TRIANGLES *4, GL_DYNAMIC_COPY)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, ssbo)



        //SET UP TRIANGLE COUNTER
        val counter = glGenBuffers()
        glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, counter)
        val stack3 = MemoryStack.stackPush()
        val buffer3 = stack3.mallocInt(1)
        buffer3.put(0).flip()
        glBufferData(GL_ATOMIC_COUNTER_BUFFER, buffer3, GL_DYNAMIC_READ)
        MemoryStack.stackPop()
        glBindBufferBase(GL_ATOMIC_COUNTER_BUFFER, 2, counter)

        val t111 = profiler.getDelta()


        //RUN COMPUTE SHADER
        computeShader.use()
        computeShader.dispatch(Chunk.SIZE/Chunk.GROUP_SIZE, Chunk.SIZE/Chunk.GROUP_SIZE, Chunk.SIZE/Chunk.GROUP_SIZE)

        glMemoryBarrier(GL_ALL_BARRIER_BITS)
        //res.delete()
        //GET VERTEX COUNT
        val stack4 = MemoryStack.stackPush()
        val buffer4 = stack4.mallocInt(1)
        glGetBufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, buffer4)
        val vCount = 3 * buffer4.get()
        MemoryStack.stackPop()

        compShader = profiler.getDelta()

        //SET UP VAO
        val vao = glGenVertexArrays()
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, ssbo)

        //vertex positions attribute
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 12*4, 0)
        glEnableVertexAttribArray(0)

        //vertex colour attribute
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 12*4, 4*4)
        glEnableVertexAttribArray(1)

        //vertex normal attribute
        glVertexAttribPointer(2, 4, GL_FLOAT, false, 12*4, 8*4)
        glEnableVertexAttribArray(2)

        println(vCount)

        vaoSetup = profiler.getDelta()

        (ssbo, vao, vCount)
    }

    override def render(shader: ShaderProgram) = {
        //println("RENDERING " + pos)
        shader.use()
        glBindVertexArray(vao)
        shader.setUniform("worldTransform", worldTransform)
        //to change later
        shader.setUniform("normalTransform", worldTransform)
        glDrawArrays(GL_TRIANGLES, 0, vertexCount)
    }

    def dispose() = {
        glDeleteBuffers(vbo)
        glDeleteVertexArrays(vao)
    }
}

object Chunk {
    val SIZE = 32
    val GROUP_SIZE = 4
    val FLOATS_IN_TRIANGLE = 36
}
