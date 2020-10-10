package maths

import org.scalatest.funsuite._

class Matrix3test extends AnyFunSuite {
    test("inverse") {
        val m = Matrix3(1f,2f,3f,
                        2f,1f,-2f,
                        -8f,-1f,7f)
        //assert(m * m.inverse === Matrix4(1f))
        assert(m.inverse * m === Matrix3(1f))
        assert(Matrix3(1f).inverse === Matrix3(1f))
    }
}
