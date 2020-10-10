package physics

import game._
import maths._

class ChunkCollisionMesh(private val array: Array[Array[Array[Voxel]]]) {

    def apply(pos: Vector3i): Voxel = array(pos.x)(pos.y)(pos.z)
}
