package physics

import org.scalatest.funsuite._
import scala.math._
import maths._


class TriangleTest extends AnyFunSuite {
    test("intersect triangle with perpendicular ray") {
        val t = Triangle(Vector3(0f,0f,0f), Vector3(1f,0f,0f), Vector3(0f,1f,0f))
        val rayPos = Vector3(0.2f, 0.2f, 0f)
        val rayDir = Vector3(0f,0f,-1f)
        val collisionPoint = t.collide(rayPos, rayDir).get * rayDir + rayPos

        assert(collisionPoint === rayPos)
    }
    test("intersect edge of triangle with some ray") {
        val t = Triangle(Vector3(1f,0f,0f), Vector3(0f,1f,0f), Vector3(0f,0f,1f))
        val rayPos = Vector3(0f,1f,1f)
        val rayDir = Vector3(0f,-1f,-1f).normalize
        val collisionPoint = t.collide(rayPos, rayDir).get * rayDir + rayPos

        assert(collisionPoint === Vector3(0f,0.5f,0.5f))
    }

    test("intersect vertex of triangle with some ray") {
        val t = Triangle(Vector3(1f,0f,0f), Vector3(0f,1f,0f), Vector3(0f,0f,1f))
        val rayPos = Vector3(0f,2f,0f)
        val rayDir = Vector3(0f, -1f, 0f)
        val collisionPoint = t.collide(rayPos, rayDir).get * rayDir + rayPos
        assert(collisionPoint === Vector3(0f,1f,0f))
    }
    test("intersect triangle with ray which is perpendicular but misses the triangle") {
        val t = Triangle(Vector3(0f,0f,0f), Vector3(1f,0f,0f), Vector3(0f,1f,0f))
        val rayPos = Vector3(3f, 0.2f, 0f)
        val rayDir = Vector3(0f,0f,-1f)
        assert(t.collide(rayPos, rayDir) === None)
    }
    test("intersect triangle with ray which is parallel to the triangle") {
        val t = Triangle(Vector3(0f,0f,0f), Vector3(1f,0f,0f), Vector3(0f,1f,0f))
        val rayPos = Vector3(0f, 0f, 0f)
        val rayDir = Vector3(1f,0f,0f)
        assert(t.collide(rayPos, rayDir) === None)
    }

}
