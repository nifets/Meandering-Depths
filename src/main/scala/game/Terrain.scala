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
import utils.MarchingCubes
import library.OpenSimplex2F
import graphics._
import library._
import library.Timer

import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack
import scala.collection.mutable.Queue
import scala.collection.immutable.Set
import scala.math._

import maths._

class Terrain {
    //profiling
    var timeSpentPreparingLoad = 0D
    var timeSpentSettingUpCompute = 0D
    var timeSpentLoadingChunks = 0D
    var timeSpentNoiseSampling = 0D

    //Keeps track of chunks currently loaded
    private val loadedChunks = HashMap[Vector3i, Chunk]()
    //Chunks that have yet to be loaded
    private val loadingStack = Stack[Vector3i]()

    //Used to see whether the player has moved from the chunk it was last in, in order to update loaded chunks
    private var lastCenterChunkPos = Vector3i(-20,0,0)

    private val MAX_CHUNKS_PER_COMPUTE = 30


    private var isComputing = false
    private var syncObject: Option[Long] = None
    private var numberOfChunks = 0
    val chunkPosArray = new Array[Vector3i](MAX_CHUNKS_PER_COMPUTE)
    val ssbomesh = glGenBuffers()
    val ssbocount = glGenBuffers()
    /**Load a compute shader for creating the mesh of the chunks*/
    private val computeShader = {
        println("loading compute shader")
        val res = ShaderProgram.computeProgram("shaders/marchingCubes.glsl")
        res.use()
        val ssbo = glGenBuffers()
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
        val buffer = MemoryUtil.memAllocInt(256*16 + 12*2 + 8)
        buffer.put(MarchingCubes.CUBE_TO_POLYGONS.flatten)
              .put(MarchingCubes.EDGE_ENDPOINTS)
              .put(MarchingCubes.FIX_CORNERS)
              .flip()
        glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_STATIC_DRAW)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssbo)
        MemoryUtil.memFree(buffer)
        res
    }


    /** Start a flood fill within the chunk at position p, beginning from the edges of the chunks and going inwards. Bits that are not connected to the chunk edges do not get visited, which is then used in the sampling method to remove floating bits. This is a bit crude, as small floating bits at the edge of a chunk will still be visited, but improving it requires information from neighbouring chunks.*/
    private def floodFill(p: Vector3i): Array[Array[Array[Boolean]]] = {
        val pos = (p * Chunk.SIZE).toVector3
        val visited = Array.ofDim[Boolean](Chunk.SIZE + 1,Chunk.SIZE + 1,Chunk.SIZE + 1)

        val stack = scala.collection.mutable.Stack[(Int, Int, Int)]()
        //BRACE FOR REALLY BAD CODE AHEAD
        for (i <- 0 until Chunk.SIZE + 1)
            for (j <- 0 until Chunk.SIZE + 1) {
                for ((a,b,c) <- List((i,j,0), (i,j, Chunk.SIZE), (i, 0, j), (i, Chunk.SIZE, j), (0, i, j), (Chunk.SIZE, i, j)))
                    if (isovalue(a + pos.z, b + pos.y, c + pos.x) > 0 && !visited(a)(b)(c)) {
                        visited(a)(b)(c) = true
                        stack.push((a,b,c))
                    }
            }
        val neighbours = List((-1,0,0),(1,0,0),(0,-1,0),(0,1,0),(0,0,-1),(0,0,1))
        while (!stack.isEmpty) {
            val (i, j, k) = stack.pop()
            for ((a,b,c) <- neighbours
                if 0 <= a+i && a + i <= Chunk.SIZE //keep within chunk borders
                && 0 <= b+j && b + j <= Chunk.SIZE
                && 0 <= c+k && c + k <= Chunk.SIZE
                && !visited(i+a)(j+b)(k+c)
                && isovalue(a+i+pos.z, b+j +pos.y, c + k + pos.x) > 0) {

                    visited(a+i)(b+j)(c+k) = true
                    stack.push((a+i,b+j,c+k))
            }
        }
        visited
    }

    private def genChunkInputData(p: Vector3i): FloatBuffer = {
        val visited = floodFill(p)
        val pos = (p * Chunk.SIZE).toVector3
        val data = MemoryUtil.memAllocFloat(4 * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1))
        val r = (0 until Chunk.SIZE + 1)
        for (i <- r; j <- r; k <- r) {
            val (value,red) = {
                val v = isovalue(i + pos.z, j + pos.y, k + pos.x)
                //remove if not found by the floodfill
                if (v > 0 && !visited(i)(j)(k)) {
                    //print("floating stuff")
                    (-v,v + 0.4f)
                }
                else if (visited(i)(j)(k))
                    (v,v + 0.4f)
                else
                    (v,-v + 0.4f)
            }
            data.put(red). //RED
                 put(red + 0.3f). //GREEN
                 put(red + 0.342f). //BLUE
                 put(value) //ISOVALUE
        }
        data.flip()
        data
    }

    private def noiseSampling(ps: List[Vector3i]) = {
        val list = for (p <- ps) yield Future {(p, genChunkInputData(p))}
        Future.sequence(list)
    }

    def update(pos: Vector3, dt: Double) = {
        /**println("TIME SPENT PREPARING LOAD: " + timeSpentPreparingLoad)
        println("TIME SPENT SETTING UP COMPUTE: " + timeSpentSettingUpCompute)
        println("TIME SPENT LOADING CHUNKS: " + timeSpentLoadingChunks)
        println("TIME SPENT NOISE SAMPLING: " + timeSpentNoiseSampling)*/
        val profiler = new Timer()
        profiler.start()

        val centerChunkPos = (pos / Chunk.SIZE.toFloat).toVector3i
        //Update which chunks should be loaded
        if (centerChunkPos != lastCenterChunkPos) {
            profiler.getDelta()
            val s = List(0,1,-1,2,-2,-3,3)
            val newChunks: List[Vector3i] = for (i <- s; j <- s; k <- s if i*i + j*j + k*k < 13)
                yield Vector3i(i + centerChunkPos.x,j + centerChunkPos.y,k + centerChunkPos.z)

            val oldChunks = loadedChunks.keySet.toList

            val profiler2 = new Timer()
            profiler2.start()
            //Add the new chunks to the loading queue
            val toLoad = (newChunks diff oldChunks).sortWith(_.squaredDistanceTo(centerChunkPos) > _.squaredDistanceTo(centerChunkPos))
            //val toLoad = Await.result(noiseSampling(newChunks diff oldChunks), Duration.Inf)
            timeSpentNoiseSampling += profiler2.getDelta()

            loadingStack.pushAll(toLoad)

            //Delete the old chunks
            for (chunkPos <- ((oldChunks diff newChunks) diff loadingStack.toList)) {
                loadedChunks get chunkPos match {
                    case Some(chunk) => chunk.dispose()
                    case None =>
                }
                loadedChunks -= chunkPos
            }

            lastCenterChunkPos = centerChunkPos

            timeSpentPreparingLoad += profiler.getDelta()
        }

        syncObject match {
            case Some(s) => glClientWaitSync(s, 0, 0) match {
                case GL_TIMEOUT_EXPIRED => //println("not done yet")
                case GL_WAIT_FAILED => //println("wait failed>")
                case _ => {
                    profiler.getDelta()
                    glMemoryBarrier(GL_ALL_BARRIER_BITS)
                    //GET VERTEX COUNT
                    val triangleCountBuffer = MemoryUtil.memAllocInt(numberOfChunks)
                    glGetNamedBufferSubData(ssbocount, 0, triangleCountBuffer)

                    for (i <- 0 until numberOfChunks) {

                        val vertexCount = 3 * triangleCountBuffer.get()
                        val vbo = glGenBuffers()
                        glNamedBufferData(vbo, 4 * 12 * vertexCount, GL_STATIC_DRAW)
                        glCopyNamedBufferSubData(ssbomesh, vbo, 4 * (i * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * MarchingCubes.MAX_TRIANGLES * Chunk.FLOATS_IN_TRIANGLE), 0, 4 * 12 * vertexCount)
                        //bytes per float/int * (vertexCountArray + currentChunk * chunk dimension * triangles per cube * floats per triangle)
                        val ch = new Chunk(chunkPosArray(i), vbo, vertexCount)
                        loadedChunks += (chunkPosArray(i) -> ch)

                    }
                    MemoryUtil.memFree(triangleCountBuffer)
                    glDeleteSync(s)
                    syncObject = None
                    isComputing = false
                    timeSpentLoadingChunks += profiler.getDelta()
                }
            }
            case None =>  //println("not computing anything")
        }


        //Run compute shader if needed
        if (!isComputing && !loadingStack.isEmpty) {
            profiler.getDelta()
            numberOfChunks = scala.math.min(MAX_CHUNKS_PER_COMPUTE, loadingStack.length)
            val list = Await.result(noiseSampling(loadingStack.take(numberOfChunks).toList), Duration.Inf)
            val inputData = MemoryUtil.memAllocFloat(4 * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1)
                                                       * numberOfChunks)
            val chunkPosBuffer = MemoryUtil.memAllocInt(numberOfChunks * 4)
            var i = 0
            for ((chunkPos, buff) <- list) {
                loadingStack.pop()
                inputData.put(buff)
                chunkPosArray(i) = chunkPos
                chunkPosBuffer.put(chunkPos.x).put(chunkPos.y).put(chunkPos.z).put(2)
                MemoryUtil.memFree(buff)
                i += 1
            }
            /**for (i <- 0 until numberOfChunks) {
                val (chunkPos, buff) = loadingStack.pop()
                //change this: copies whole buffer so its twice as slow as it should be
                inputData.put(buff)
                chunkPosArray(i) = chunkPos
                chunkPosBuffer.put(chunkPos.x).put(chunkPos.y).put(chunkPos.z).put(2)
                MemoryUtil.memFree(buff)
            }*/

            computeShader.use()

            //BIND CHUNK POS INPUT TO BINDING 1
            val chunkPosSsbo = glGenBuffers()
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPosSsbo)
            chunkPosBuffer.flip()
            glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPosBuffer, GL_STATIC_DRAW)
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, chunkPosSsbo)
            MemoryUtil.memFree(chunkPosBuffer)

            //BIND CHUNK INPUT DATA TO BINDING 2
            val ssboinput = glGenBuffers()
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboinput)
            inputData.flip()
            glBufferData(GL_SHADER_STORAGE_BUFFER, inputData, GL_STATIC_DRAW)
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, ssboinput)
            MemoryUtil.memFree(inputData)

            //SET UP OUTPUT BUFFER FOR COMPUTE SHADER IN BINDING 3

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbomesh)
            glBufferData(GL_SHADER_STORAGE_BUFFER, numberOfChunks * Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * Chunk.FLOATS_IN_TRIANGLE * MarchingCubes.MAX_TRIANGLES *4, GL_DYNAMIC_COPY)
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, ssbomesh)

            //SET UP OUTPUT BUFFER FOR VERTEX COUNT IN BINDING 4
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbocount)
            val countbuffer = MemoryUtil.memAllocInt(numberOfChunks)
            for (i <- 0 until numberOfChunks)
                countbuffer.put(0)
            countbuffer.flip()
            glBufferData(GL_SHADER_STORAGE_BUFFER, countbuffer, GL_DYNAMIC_COPY)
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, ssbocount)
            MemoryUtil.memFree(countbuffer)

            //RUN COMPUTE SHADER
            computeShader.dispatch(numberOfChunks, 1, Chunk.SIZE)
            isComputing = true
            syncObject = Some(glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0))
            timeSpentSettingUpCompute += profiler.getDelta()
        }

    }

    def getLoadedChunks: List[Chunk] = loadedChunks.values.toList

    //For terrain gen, should probably abstract into a different class at some point
    val noise = Array(new OpenSimplex2F(212123), new OpenSimplex2F(361222), new OpenSimplex2F(661222), new OpenSimplex2F(961222))
    val freq = Array(0.007, 0.019, 0.059, 0.12)
    val freqY = Array(0.012, 0.032, 0.072, 0.23)
    val amp = Array(0.7F, 0.2F, 0.1F, 0.07F)
    val offset = 0.2F
    private def isovalue(x: Float, y: Float, z: Float): Float = {
        var res = -0.1F
        for (i <- 0 until 4)
            res += amp(i) * noise(i).noise3_XZBeforeY(freq(i) * x, freqY(i) * y,freq(i) * z).toFloat
        res
    }
}
