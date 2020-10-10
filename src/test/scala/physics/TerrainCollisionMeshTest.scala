package physics

import org.scalatest.funsuite._
import scala.math._
import maths._


class TerrainCollisionMeshTest extends AnyFunSuite {
    test("ray parallel with x axis") {
        val rayPos = Vector3(0.2f, 0.2f, 0f)
        val rayDir = Vector3(0f,0f,-1f)
        val rayLength = 3f

        assert(TerrainCollisionMesh.rayVoxelIntersection(rayPos, rayDir, rayLength) === List(Vector3i(0,0,0), Vector3i(0,0,-1), Vector3i(0,0,-2), Vector3i(0,0,-3), Vector3i(0,0,-4)))
    }

}
