package maths

import org.scalatest.FunSuite
import scala.math._

class QuaternionTest extends FunSuite {
    test("rotate vector with 0,0,0 euler angles") {
        val v = Vector3(1f,0f,0f)
        val q = Quaternion.eulerRotation(0f,0f,0f)
        assert(v === v.rotate(q))
    }
    test("rotate vector with euler angles 1") {
        val v = Vector3(1f,0f,0f)
        val q = Quaternion.eulerRotation(0f,90f.toRadians,0f)
        assert(v.rotate(q) === Vector3(0f,0f,-1f))
    }
}
