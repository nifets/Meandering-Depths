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
import library.Timer._
import physics._

import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack
import scala.collection.mutable.Queue
import scala.collection.immutable.Set
import scala.math._

import maths._

class Terrain {
    val chunkLoader = new ChunkLoader()

    //Keeps track of chunks currently loaded
    private val loadedChunks = HashMap[Vector3i, Chunk]()

    val changedVoxels = HashMap[Vector3i, Float]()

    //Chunks that have yet to be loaded
    private var loadingList = List[Vector3i]()

    //Used to see whether the player has moved from the chunk it was last in, in order to update loaded chunks
    var centerChunkPos = Vector3i(-20,0,0)

    val terrainCollisionMesh = new TerrainCollisionMesh

    /** Start a flood fill within the chunk at position p, beginning from the edges of the chunks and going inwards. Bits that are not connected to the chunk edges do not get visited, which is then used in the sampling method to remove floating bits. This is a bit crude, as small floating bits at the edge of a chunk will still be visited, but improving it requires information from neighbouring chunks.*/
    private def floodFill(p: Vector3i): Array[Array[Array[Boolean]]] = {
        val pos = (p * Chunk.SIZE).toVector3
        val visited = Array.ofDim[Boolean](Chunk.SIZE + 1,Chunk.SIZE + 1,Chunk.SIZE + 1)

        val stack = scala.collection.mutable.Stack[(Int, Int, Int)]()
        //BRACE FOR REALLY BAD CODE AHEAD
        for (i <- 0 until Chunk.SIZE + 1)
            for (j <- 0 until Chunk.SIZE + 1) {
                for ((a,b,c) <- List((i,j,0), (i,j, Chunk.SIZE), (i, 0, j), (i, Chunk.SIZE, j), (0, i, j), (Chunk.SIZE, i, j)))
                    if (getDataAt(a + pos.z, b + pos.y, c + pos.x)._3 > 0 && !visited(a)(b)(c)) {
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
                && getDataAt(a+i+pos.z, b+j +pos.y, c + k + pos.x)._3 > 0) {

                    visited(a+i)(b+j)(c+k) = true
                    stack.push((a+i,b+j,c+k))
            }
        }
        visited
    }

    /** Generate input data for the given chunk, returning it in a readable floatbuffer*/
    private def genChunkInputData(p: Vector3i): FloatBuffer = {
        val visited = floodFill(p)
        val pos = (p * Chunk.SIZE).toVector3
        val data = MemoryUtil.memAllocFloat(4 * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1) * (Chunk.SIZE+ 1))
        //data: colour_variation, material_id, isovalue
        val r = (0 until Chunk.SIZE + 1)
        for (i <- r; j <- r; k <- r) {
            val (colourVariation, materialId, isovalue) = {
                /**val pdest = changedVoxels.get(Vector3i(i,j,k)) match {
                    case Some(f) => f
                    case None => 0f
                }*/
                val (inColVar, inMatId, inIsoval) = getDataAt(i + pos.z, j + pos.y, k + pos.x)
                //remove if not found by the floodfill
                if (inIsoval > 0 && !visited(i)(j)(k))
                    //print("floating stuff")
                    (inColVar, inMatId, -inIsoval)
                else
                    (inColVar, inMatId, inIsoval)
            }
            data.put(materialId)
                .put(colourVariation)
                .put(isovalue)
                .put(0f)
        }
        data.flip()
        data
    }

    /** Generate chunk input data for several chunks simultaneously*/
    private def asyncGenChunksInputData(ps: List[Vector3i]): List[(Vector3i, FloatBuffer)] = {
        val list = for (p <- ps) yield Future {(p, genChunkInputData(p))}
        Await.result(Future.sequence(list), Duration.Inf)
    }

    /** For each chunk at the positions given in the input, clears the resources used by the chunk and removes it from the hashmap.*/
    private def deleteChunks(ps: List[Vector3i]): Unit = {
        for (p <- ps) {
            loadedChunks get p match {
                case Some(chunk) => chunk.dispose()
                case None =>
            }
            loadedChunks -= p
            terrainCollisionMesh.removeChunkMesh(p)
        }
    }

    /** Prepares the chunks surrounding the center chunk for loading, returning the high priority ones and adding the low priority ones to the loading stack, deleting the old chunks in the process. (the last part should be done by a different function maybe)*/
    private def prepareChunks(): List[Vector3i] = {
        /** The chunks directly neighbouring the player should be loaded immediatedly to have working collisions asap*/
        val nearChunks = {
            val s = List(0,1,-1)
            for (i <- s; j <- s; k <- s)
                yield Vector3i(i + centerChunkPos.x, j + centerChunkPos.y, k + centerChunkPos.z)
        }

        /** These chunks have lower priority*/
        val farChunks = {
            val s = List(0,1,-1,2,-2,3,-3,4,-4,5,-5)
            val l = for (i <- s; j <- s; k <- s if i*i + j*j + k*k < 25)
                        yield Vector3i(i + centerChunkPos.x, j + centerChunkPos.y, k + centerChunkPos.z)
            l diff nearChunks
        }

        /** The chunks which were loaded previously*/
        val oldChunks = loadedChunks.keySet.toList

        /** Update the loading list, adding the low priority chunks in order of their distance to the player*/
        loadingList = (farChunks diff oldChunks).sortWith(_.squaredDistanceTo(centerChunkPos) < _.squaredDistanceTo(centerChunkPos))

        //Delete the old chunks
        deleteChunks(oldChunks diff farChunks diff nearChunks)

        nearChunks diff oldChunks
    }

    def reload(chPos: Vector3i): Unit = {
        deleteChunks(List(chPos))
        loadingList = chPos :: loadingList
    }

    def render(alpha: Float): Unit = {

    }

    def update(pos: Vector3, dt: Double): Unit = {
        //The position of the chunk the player is in currently
        val newCenterChunkPos = (pos / Chunk.SIZE.toFloat).toVector3i

        //Update which chunks should be loaded
        if (newCenterChunkPos != centerChunkPos) {
            centerChunkPos = newCenterChunkPos
            val nearChunks = prepareChunks()

            //Add the high priority chunks to the loader
            chunkLoader.computeChunks(asyncGenChunksInputData(nearChunks), true)

        }

        //Add low priority chunks to the loader
        if (!chunkLoader.isComputing && !loadingList.isEmpty) {
            val toLoad = loadingList.take(5)
            loadingList = loadingList.drop(5)
            chunkLoader.computeChunks(asyncGenChunksInputData(toLoad), false)

        }

        //Read output from the chunk loader if possible
        if (chunkLoader.checkStatus()) {
            for (ch <- chunkLoader.getOutput()) {
                terrainCollisionMesh.addChunkMesh(ch.pos, ch.collisionBox)
                loadedChunks += (ch.pos -> ch)

            }
        }
    }

    def getLoadedChunks: List[Chunk] = {
        loadedChunks.values.toList
    }

    //For terrain gen, should probably abstract into a different class at some point
    val noise = Array(new OpenSimplex2F(212123), new OpenSimplex2F(361222), new OpenSimplex2F(661222), new OpenSimplex2F(961222))
    val freq = Array(0.007, 0.019, 0.059, 0.12)
    val freqY = Array(0.012, 0.032, 0.072, 0.23)
    val amp = Array(0.7F, 0.2F, 0.1F, 0.07F)
    val offset = 0.2F

    val materialNoise = new OpenSimplex2F(1112222111)


    //colour_var, material_id, isovalue
    private def getDataAt(x: Float, y: Float, z: Float): (Float, Float, Float) = {
        var isoval = -0.1F
        for (i <- 0 until 4)
            isoval += amp(i) * noise(i).noise3_XZBeforeY(freq(i) * x, freqY(i) * y,freq(i) * z).toFloat

        val colourVar = materialNoise.noise3_XZBeforeY(0.005 * x,0.009 * y,0.005* z).toFloat * 1f/20

        if (colourVar > 0)
            (colourVar, 1f, isoval)
        else
            (colourVar, 0f, isoval)

    }
}

object Terrain {
    val materials = Array(
        //Ambient, diffuse, specular, shininess
        Material(Vector3(0.05375f, 0.05f, 0.06625f), Vector3(0.18275f, 0.17f, 0.22525f), Vector3(0.332741f, 0.328634f, 0.346435f), 	0.3f), //obsidian ID 0
        Material(Vector3(0.25f, 0.20725f, 0.20725f), Vector3(1f, 0.829f, 0.829f), Vector3(0.296648f, 0.296648f, 0.296648f), 0.088f), //pearl ID 1
        Material(Vector3(0.025f, 0.20725f, 0.20725f), Vector3(0.6f, 0.829f, 0.829f), Vector3(0.296648f, 0.296648f, 0.296648f), 0.0188f) //custom ID 2
    )
}
