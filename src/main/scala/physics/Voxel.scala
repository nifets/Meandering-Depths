package physics

import maths._

class Voxel(private val triangles: List[Triangle]) {
    def isEmpty: Boolean = triangles.isEmpty
    def collide(rayPos: Vector3, rayDir: Vector3): Option[Float] = {
        val l = triangles.map((t: Triangle) => t.collide(rayPos, rayDir))
        l.flatten match {
            case Nil => None
            case l => {
                val res = l.min
                if (res > 0)
                    Some(res)
                else
                    None
            }
        }
    }
}
