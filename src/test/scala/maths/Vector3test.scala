package maths

import org.scalatest.funsuite._

class Vector3test extends AnyFunSuite {
    test("dot product") {
        val v = Vector3(0f,-1f,2.5f)
        assert((v dot v) === 7.25f)
    }

    test("cross product") {

    }

    test("linear interpolation") {
        val v1 = Vector3(0f,1f,2f)
        val v2 = Vector3(1f,2f,4f)
        assert(v1.lerp(0.6f,v2) === Vector3(0.6f, 1.6f, 3.2f))
    }

}
