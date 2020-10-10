package maths

import org.scalatest.funsuite._

class Matrix4test extends AnyFunSuite {
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

    test("inverse") {
        val m = Matrix4(1f,2f,3f,5f,
                        2f,1f,-2f,1f,
                        -8f,-1f,2f,7f,
                        0f,1f,-1f,2f)
        //assert(m * m.inverse === Matrix4(1f))
        assert(m.inverse * m === Matrix4(1f))
        assert(Matrix4(1f).inverse === Matrix4(1f))
    }
}
