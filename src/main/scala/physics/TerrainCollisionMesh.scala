package physics

import maths._
import scala.math._
import scala.collection.mutable.HashMap
import game._

class TerrainCollisionMesh {
    private val chunkMap = HashMap[Vector3i, ChunkCollisionMesh]()
    def collide(rayPos: Vector3, rayDir: Vector3, rayLength: Float): Option[Float] = {
        var searching = true
        var result: Option[Float] = None
        for (pos <- TerrainCollisionMesh.rayVoxelIntersection(rayPos, rayDir, rayLength) if searching) {
            voxel(pos) match {
                case Some(v) => v.collide(rayPos - pos.toVector3, rayDir) match {
                    case Some(t) => result = Some(t); searching = false
                    case None =>
                }
                case None => println("voxel not loaded"); result = Some(0f); searching = false
            }
        }
        result
    }



    def voxel(pos: Vector3i): Option[Voxel] = {
        val chunkPos = (pos.toVector3/ Chunk.SIZE.toFloat).toVector3i
        chunkMap.get(chunkPos) match {
            case Some(chunkMesh) => {
                val voxelPos = pos - chunkPos * Chunk.SIZE
                Some(chunkMesh(voxelPos))
            }
            case None => None
        }
    }

    def addChunkMesh(pos: Vector3i, data: ChunkCollisionMesh) = chunkMap += (pos -> data)

    def removeChunkMesh(pos: Vector3i) = chunkMap -= pos

}

object TerrainCollisionMesh {

    /** citation: A fast voxel traversal algorithm for ray tracing - ‎Amanatides*/
    def rayVoxelIntersection(rayPos: Vector3, rayDir: Vector3, rayLength: Float): List[Vector3i] = {
        val voxelPos = rayPos.toVector3i
        var list = List[Vector3i]()
        var x = voxelPos.x
        var y = voxelPos.y
        var z = voxelPos.z
        def sign(x: Float): Int = if (x >= 0) 1 else -1
        val eps = 0.00001f
        val maxf = max(5 * rayLength, 2000.0f)
        /** Direction of the ray for each axis*/
        val stepX = sign(rayDir.x)
        val stepY = sign(rayDir.y)
        val stepZ = sign(rayDir.z)
        /** How much should the ray move for the x/y/z component to equal 1 (can be calculated using similar triangles)*/
        val tDeltaX = if (abs(rayDir.x) > eps) 1f/(rayDir.x * stepX) else maxf
        val tDeltaY = if (abs(rayDir.y) > eps) 1f/(rayDir.y * stepY) else maxf
        val tDeltaZ = if (abs(rayDir.z) > eps) 1f/(rayDir.z * stepZ) else maxf


        /** How much should the ray move from its origin in order to cross the next x/y/z boundary (can be calculated using similar triangles) The values will always be positive, so no need to take the absolute value.*/
        var tMaxX = if (abs(rayDir.x) > eps) (voxelPos.x + (1+stepX)/2 - rayPos.x)/rayDir.x else maxf
        var tMaxY = if (abs(rayDir.y) > eps) (voxelPos.y + (1+stepY)/2 - rayPos.y)/rayDir.y else maxf
        var tMaxZ = if (abs(rayDir.z) > eps) (voxelPos.z + (1+stepZ)/2 - rayPos.z)/rayDir.z else maxf


        var t = 0f

        while (t <= rayLength) {
            list = Vector3i(x,y,z) :: list
            t = tMaxX min tMaxY min tMaxZ
            if (tMaxX == t) {
                x += stepX
                tMaxX += tDeltaX
            }
            else if (tMaxY == t) {
                y += stepY;
                tMaxY += tDeltaY;
            }
            else if (tMaxZ == t) {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            else {
                println("WEIRD BUG")
            }
        }
        list.reverse
    }
}
