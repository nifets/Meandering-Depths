package game

import scala.concurrent.duration._
import scala.concurrent.{Future,Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

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
import org.lwjgl.opengl.GL45._
import org.lwjgl.opengl.ARBSync._

import org.lwjgl.system.MemoryStack._
import org.lwjgl.system.MemoryUtil._

import library._
import library.Timer._
import physics._
import graphics._
import maths._
import scala.math._

import utils.MarchingCubes

/**Manager of chunk loading using a compute shader */
class ChunkLoader {
    /** The number of chunks and their positions in the world in the current computation*/
    private var numberOfChunks = 0
    private val chunkPosArray = new Array[Vector3i](ChunkLoader.MAX_CHUNKS_PER_COMPUTE)

    /** Output ssbo's*/
    private val renderDataSsbo = glGenBuffers()
    private val renderTriangleCountSsbo = glGenBuffers()
    private val collisionDataSsbo = glGenBuffers()
    private val collisionTriangleCountSsbo = glGenBuffers()

    /**Whether the compute shader is doing any work at the moment */
    def isComputing: Boolean = !syncObject.isEmpty

    /**The sync object is used to check the status of the compute shader, if it is running. When checking the status, it will block the thread for at most toWait nanoseconds while waiting for a signal from the compute shader. toWait is basically used as an indicator for the priority of the current computation.*/
    private var syncObject: Option[Long] = None
    private var toWait = 0

    /** Load the compute shader and prepare the output ssbo's*/
    private val computeShader = {
        val res = ShaderProgram.computeProgram("shaders/marchingCubes.glsl")
        res.use()

        /** Load auxilary look-up tables into the shader*/
        val auxSsbo = glGenBuffers()
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, auxSsbo)
        val auxBuffer = MemoryUtil.memAllocInt(256*16 + 12*2 + 8)
        auxBuffer.put(MarchingCubes.CUBE_TO_POLYGONS.flatten)
                 .put(MarchingCubes.EDGE_ENDPOINTS)
                 .put(MarchingCubes.FIX_CORNERS)
                 .flip()
        glBufferData(GL_SHADER_STORAGE_BUFFER, auxBuffer, GL_STATIC_DRAW)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, auxSsbo)
        MemoryUtil.memFree(auxBuffer)

        /** Prepare output ssbo for rendering data */
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, renderDataSsbo)
        glBufferData(GL_SHADER_STORAGE_BUFFER, ChunkLoader.MAX_CHUNKS_PER_COMPUTE * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * Chunk.FLOATS_IN_TRIANGLE * MarchingCubes.MAX_TRIANGLES_PER_CUBE * 4, GL_DYNAMIC_COPY)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, renderDataSsbo)

        /** Prepare output ssbo for rendering triangle count */
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, renderTriangleCountSsbo)
        glBufferData(GL_SHADER_STORAGE_BUFFER, 4 * ChunkLoader.MAX_CHUNKS_PER_COMPUTE, GL_DYNAMIC_READ)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, renderTriangleCountSsbo)

        /** Prepare output ssbo for collision data*/
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, collisionDataSsbo)
        glBufferData(GL_SHADER_STORAGE_BUFFER, ChunkLoader.MAX_CHUNKS_PER_COMPUTE * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 60 *4, GL_DYNAMIC_READ)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, collisionDataSsbo)

        /** Prepare output ssbo for */
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, collisionTriangleCountSsbo)
        glBufferData(GL_SHADER_STORAGE_BUFFER, ChunkLoader.MAX_CHUNKS_PER_COMPUTE * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 4, GL_DYNAMIC_READ)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, collisionTriangleCountSsbo)

        //glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
        //glDeleteBuffers(auxSsbo)

        res
    }

    /**Dispatch the compute shader to load the given chunks, cancelling the current computation if there is one in progress*/
    def computeChunks(chunks: List[(Vector3i, FloatBuffer)], isHighPriority: Boolean): Unit = {
        /**If a computation is in progress, cancel it. */
        if (isComputing){
            glDeleteSync(syncObject.get)
            syncObject = None
            glMemoryBarrier(GL_ALL_BARRIER_BITS)
        }

        numberOfChunks = chunks.length


        /** Prepare input buffers*/
        val inputDataBuffer = MemoryUtil.memAllocFloat(4 * (Chunk.SIZE+1) * (Chunk.SIZE+1) * (Chunk.SIZE+1)
                                                   * numberOfChunks)
        val chunkPosBuffer = MemoryUtil.memAllocInt(numberOfChunks * 4)

        var i = 0
        for ((pos, data) <- chunks) {
            //This could be optimized -- currently it concatenates several buffers into one, resulting in the copying of all input data.
            inputDataBuffer.put(data)
            chunkPosArray(i) = pos
            chunkPosBuffer.put(pos.x).put(pos.y).put(pos.z).put(2)
            MemoryUtil.memFree(data)
            i += 1
        }

        computeShader.use()

        /** Load chunk position data into the shader*/
        val chunkPosSsbo = glGenBuffers()
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPosSsbo)
        chunkPosBuffer.flip()
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPosBuffer, GL_STATIC_DRAW)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, chunkPosSsbo)

        /** Load chunk voxel data into the shader*/
        val inputSsbo = glGenBuffers()
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, inputSsbo)
        inputDataBuffer.flip()
        glBufferData(GL_SHADER_STORAGE_BUFFER, inputDataBuffer, GL_STATIC_DRAW)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, inputSsbo)

        /** Reset values of render triangle count to 0 */
        val tcbuff = MemoryUtil.memAllocInt(1)
        tcbuff.put(0).flip()
        glClearNamedBufferData(renderTriangleCountSsbo, GL_R32I, GL_RED, GL_INT, tcbuff)


        /** Clear resources*/
        MemoryUtil.memFree(chunkPosBuffer)
        MemoryUtil.memFree(inputDataBuffer)
        MemoryUtil.memFree(tcbuff)
        //glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
        //glDeleteBuffers(chunkPosSsbo)
        //glDeleteBuffers(inputSsbo)

        //RUN COMPUTE SHADER
        computeShader.dispatch(numberOfChunks, 1, Chunk.SIZE)

        if (isHighPriority)
            toWait = 1000
        else
            toWait = 0

        syncObject = Some(glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0))
    }

    /** Check whether the running compute shader has finished its work, returning false if there is no compute shader running*/
    def checkStatus(): Boolean = syncObject match {

        case Some(s) => glClientWaitSync(s, 0, toWait) match {
            case GL_TIMEOUT_EXPIRED => false
            case GL_WAIT_FAILED => false
            case _ => true
        }
        case None => false
    }

    /** Gets the output from the running compute shader and processes it to create the required chunks*/
    def getOutput(): List[Chunk] = {
        var chunkList = List[Chunk]()

        require(isComputing)

        //Force opengl to update output ssbo's, might block if compute shader is not done
        glMemoryBarrier(GL_ALL_BARRIER_BITS)
        //Read output data from opengl to nio buffers
        val renderTriangleCountBuffer = MemoryUtil.memAllocInt(numberOfChunks)
        glGetNamedBufferSubData(renderTriangleCountSsbo, 0, renderTriangleCountBuffer)
        val collisionDataBuffer = MemoryUtil.memAllocFloat(numberOfChunks * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 60)
        //this operation takes a long time; bottlenecks the entire thing
        //Thread.sleep(2000)
        glGetNamedBufferSubData(collisionDataSsbo, 0, collisionDataBuffer)

        val collisionTriangleCountBuffer = MemoryUtil.memAllocInt(numberOfChunks * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE)
        glGetNamedBufferSubData(collisionTriangleCountSsbo, 0, collisionTriangleCountBuffer)


        for (u <- 0 until numberOfChunks) {
            //Processing chunk u
            val vertexCount = 3 * renderTriangleCountBuffer.get()

            //Create vbo for rendering
            val vbo = glGenBuffers()

            glNamedBufferData(vbo, 4 * 8 * vertexCount, GL_STATIC_DRAW)
            glCopyNamedBufferSubData(renderDataSsbo, vbo, 4 * (u * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * MarchingCubes.MAX_TRIANGLES_PER_CUBE * Chunk.FLOATS_IN_TRIANGLE), 0, 4 * 8 * vertexCount)
            //bytes per float/int * (vertexCountArray + currentChunk * chunk dimension * triangles per cube * floats per triangle)

            //Create collision box
            val arr = Array.ofDim[Voxel](Chunk.SIZE, Chunk.SIZE, Chunk.SIZE)

            for (i <- 0 until Chunk.SIZE)
                for (j <- 0 until Chunk.SIZE)
                    for (k <- 0 until Chunk.SIZE) {
                        val numberOfTriangles = collisionTriangleCountBuffer.get()
                        var list = List[Triangle]()
                        for (l <- 0 until 5) {
                            def buff = collisionDataBuffer
                            val a = Vector3(buff.get(),buff.get(),buff.get())
                            buff.get()
                            val b = Vector3(buff.get(),buff.get(),buff.get())
                            buff.get()
                            val c = Vector3(buff.get(),buff.get(),buff.get())
                            buff.get()
                            val t = Triangle(a,b,c)
                            if (l < numberOfTriangles)
                                list = t :: list
                        }
                        arr(i)(j)(k) = new Voxel(list)
                    }
            chunkList = new Chunk(chunkPosArray(u), vbo, vertexCount, new ChunkCollisionMesh(arr)) :: chunkList
        }
        MemoryUtil.memFree(renderTriangleCountBuffer)
        MemoryUtil.memFree(collisionDataBuffer)
        MemoryUtil.memFree(collisionTriangleCountBuffer)

        glDeleteSync(syncObject.get)
        syncObject = None
        numberOfChunks = 0

        chunkList
    }

}

object ChunkLoader {
    val MAX_CHUNKS_PER_COMPUTE = 30
}
