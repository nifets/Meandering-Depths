package maths

import org.scalatest.FunSuite

class Matrix4test extends FunSuite {
    test("translate vector using matrix") {
        val m = Matrix4.translation(2f, 3f, 1f)
        val v = Vector4(0f,-1f,2.5f,1f)
        assert((m * v) === Vector4(2f,2f,3.5f,1f))
    }

    test("scale vector using matrix") {
        val m = Matrix4.scaling(2f, 1f, 0f)
        val v = Vector4(1f, 0f, 0f, 1f)
        assert(m * v === Vector4(2f, 0f, 0f, 1f))
    }
}
