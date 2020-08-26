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

import org.lwjgl.system.MemoryStack._
import org.lwjgl.system.MemoryUtil._
import utils.MarchingCubes
import library.OpenSimplex2F
import graphics._
import library._
import library.Timer

import scala.collection.mutable.HashMap
import scala.math._

import maths._

class Terrain {
    private val loadedChunks = HashMap[Vector3i, Chunk]()
    var lastCenterChunkPos = Vector3i(-20,0,0)

    val noise = Array(new OpenSimplex2F(212123), new OpenSimplex2F(361222), new OpenSimplex2F(661222), new OpenSimplex2F(961222))
    val freq = Array(0.007, 0.019, 0.059, 0.12)
    val freqY = Array(0.012, 0.032, 0.072, 0.23)
    val amp = Array(0.7F, 0.2F, 0.1F, 0.07F)
    val offset = 0.2F

    val computeShader = {
        val res = ShaderProgram.computeProgram("shaders/marchingCubes.glsl")
        val ssbo = glGenBuffers()
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
        val auxTables = MemoryUtil.memAllocInt(256*16 + 12*2 + 8)
        auxTables.put(MarchingCubes.CUBE_TO_POLYGONS.flatten)
        auxTables.put(MarchingCubes.EDGE_ENDPOINTS).put(MarchingCubes.FIX_CORNERS).flip()
        glBufferData(GL_SHADER_STORAGE_BUFFER, auxTables, GL_STATIC_DRAW)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, ssbo)
        MemoryUtil.memFree(auxTables)

        res
    }

    /**def floodFill(p: Vector3) = {
        val visited = Array.ofDim[Boolean](Chunk.SIZE + 1,Chunk.SIZE + 1,Chunk.SIZE + 1)

        val stack = scala.collection.mutable.Stack[(Int, Int, Int)]()
        //BRACE FOR REALLY BAD CODE AHEAD
        for (i <- 0 until Chunk.SIZE + 1)
            for (j <- 0 until Chunk.SIZE + 1) {
                for ((a,b,c) <- List((i,j,0), (i,j, Chunk.SIZE), (i, 0, j), (i, Chunk.SIZE, j), (0, i, j), (Chunk.SIZE, i, j)))
                    if (isovalue((a + p(2)*Chunk.SIZE).toDouble, (b + p(1)*Chunk.SIZE).toDouble,
                    (c + p(0)*Chunk.SIZE).toDouble) > 0) {
                        visited(a)(b)(c) = true
                        stack.push((a,b,c))
                    }
            }
        val neighbours = List((-1,0,0),(1,0,0),(0,-1,0),(0,1,0),(0,0,-1),(0,0,1))
        while (!stack.isEmpty) {
            val (i, j, k) = stack.pop()
            for ((a,b,c) <- neighbours if 0 <= a+i && a + i <= Chunk.SIZE && 0 <= b+j && b + j <= Chunk.SIZE && 0 <= c+k && c + k <= Chunk.SIZE && !visited(i+a)(j+b)(k+c) && isovalue((a+i + p(2)*Chunk.SIZE).toDouble, (b +j +p(1)*Chunk.SIZE).toDouble, (c +k +p(0)*Chunk.SIZE).toDouble) > 0) {
                visited(a+i)(b+j)(c+k) = true
                stack.push((a+i,b+j,c+k))
            }
        }
        visited
    }*/

    /** Generate the raw input data of the chunk at position (x,y,z)*/
    /**def genChunkInputData(p: Vector3, visited: Array[Array[Array[Boolean]]]): FloatBuffer = {
        val data = MemoryUtil.memAllocFloat(4 * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1))
        val r = (0 until Chunk.SIZE + 1)
        for (i <- r; j <- r; k <- r) {
            data.put(0.1F + 0.5F*min(i, Chunk.SIZE - i).toFloat/ Chunk.SIZE). //RED
                      put(0.1F + 0.5F*min(j, Chunk.SIZE - j).toFloat/ Chunk.SIZE). //GREEN
                      put(0.1F + 0.5F*min(k, Chunk.SIZE - k).toFloat/ Chunk.SIZE) //BLUE
            val v = isovalue((i + p(2)*Chunk.SIZE).toDouble, (j + p(1)*Chunk.SIZE).toDouble, (k + p(0)*Chunk.SIZE).toDouble) //ISOVALUE
            if (v > 0 && !visited(i)(j)(k))
                data.put(-v)
            else
                data.put(v)
        }
        data.flip()
        data
    }*/
    def genChunkInputData(p: Vector3i): FloatBuffer = {
        val pos = (p * Chunk.SIZE).toVector3
        val data = MemoryUtil.memAllocFloat(4 * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1))
        val r = (0 until Chunk.SIZE + 1)
        for (i <- r; j <- r; k <- r) {
            data.put(0.3F). //RED
                 put(0.3F + 0.3F*min(i, Chunk.SIZE - i).toFloat/ Chunk.SIZE). //GREEN
                 put(0.3F + 0.4F*min(i, Chunk.SIZE - i).toFloat/ Chunk.SIZE). //BLUE
                 put(isovalue(i + pos.z, j + pos.y, k + pos.x)) //ISOVALUE
        }
        data.flip()
        data
    }

    def noiseSampling(ps: Set[Vector3i]) = {
        val list = for (p <- ps) yield Future {(p, genChunkInputData(p))}
        Future.sequence(list)
    }

    def update(pos: Vector3) = {

        val centerChunkPos = (pos / Chunk.SIZE.toFloat).toVector3i

        if (centerChunkPos != lastCenterChunkPos) {
            val profiler2 = new Timer()
            profiler2.start()

            var inputSampling = 0D
            var textureLoad = 0D
            var compShader = 0D
            var vaoSetup = 0D
            var vertexCount = 0

            val s = Set(-2,-1,0,1,2)
            val newChunks = for (i <- s; j <- s; k <- s)
                yield Vector3i(i + centerChunkPos.x,j + centerChunkPos.y,k + centerChunkPos.z)

            val oldChunks = loadedChunks.keySet



            println("BEFORE CHUNK INIT: " + profiler2.getLowPrecisionTime())
            for ((chunkPos, data) <- Await.result(noiseSampling(newChunks diff oldChunks), Duration.Inf)) {
                inputSampling += profiler2.getDelta()
                val ch = new Chunk(chunkPos, data, computeShader)
                loadedChunks += (chunkPos -> ch)
                println("INITIALIZED CHUNK " + ch.pos)
                println(profiler2.getLowPrecisionTime())
                //inputSampling += ch.inputSampling
                textureLoad += ch.textureLoad
                compShader += ch.compShader
                vaoSetup += ch.vaoSetup
                vertexCount += ch.vertexCount
                val t = profiler2.getDelta()
            }

            /**for (chunkPos <- (newChunks diff oldChunks)) {
                val ch = new Chunk(chunkPos, inputDataMap(chunkPos), computeShader)
                loadedChunks += (chunkPos -> ch)
                println("INITIALIZED CHUNK " + ch.pos)
                println(profiler2.getLowPrecisionTime())
                //inputSampling += ch.inputSampling
                textureLoad += ch.textureLoad
                compShader += ch.compShader
                vaoSetup += ch.vaoSetup
            }*/
            for (chunkPos <- (oldChunks diff newChunks)) {
                loadedChunks get chunkPos match {
                    case Some(chunk) => chunk.dispose()
                    case None =>
                }
                loadedChunks -= chunkPos
            }
            println("DELETE OLD CHUNKS TIME: " + profiler2.getDelta())
            println("INPUT SAMPLING TIME: " + inputSampling)
            println("TEXTURE LOAD TIME: " + textureLoad)
            println("COMPUTE SHADER TIME: " + compShader)
            println("VAO SETUP TIME: " + vaoSetup)
            println("VERTEX COUNT: " + vertexCount)
            println("---------------------------------")
        }
        lastCenterChunkPos = centerChunkPos
    }

    def getLoadedChunks: List[Chunk] = loadedChunks.values.toList

    private def isovalue(x: Float, y: Float, z: Float): Float = {
        var res = -0.1F
        for (i <- 0 until 4)
            res += amp(i) * noise(i).noise3_XZBeforeY(freq(i) * x, freqY(i) * y,freq(i) * z).toFloat
        res
    }
}
