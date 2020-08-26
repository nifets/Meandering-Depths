package maths

import scala.math._

import scala.language.implicitConversions

object Matrix3 {
    lazy val identity: Matrix3 = Matrix3(1f)

    def diag(x: Float, y: Float, z: Float): Matrix3 =
        Matrix3(x, 0f, 0f,
                0f, y, 0f,
                0f, 0f, z)

    def diag(v: Vector3): Matrix3 = diag(v.x,v.y,v.z)

    def scaling(x: Float, y: Float, z: Float): Matrix3 = diag(x,y,z)

    def rotation(q: Quaternion): Matrix3 = {
        val ins = 1f/q.normSquared //inverse norm squared
        Matrix3(1f - 2*ins*(q.y*q.y + q.z*q.z), 2*ins*(q.x*q.y + q.z*q.s), 2*ins*(q.x*q.z - q.y*q.s),
                2*ins*(q.x*q.y - q.z*q.s), 1f - 2*ins*(q.x*q.x + q.z*q.z), 2*ins*(q.y*q.z + q.x*q.s),
                2*ins*(q.x*q.z + q.y*q.s), 2*ins*(q.y*q.z - q.x*q.s), 1f - 2*ins*(q.x*q.x + q.y*q.y))
    }


    def apply(): Matrix3 = Matrix3(0f)

    def apply(f: Float): Matrix3 = diag(f,f,f)

    def apply(v0: Vector3, v1: Vector3, v2: Vector3): Matrix3 =
        Matrix3(v0.x, v0.y, v0.z,
                v1.x, v1.y, v1.z,
                v2.x, v2.y, v2.z)

    implicit def floatToMatrix3(f: Float): Matrix3 = Matrix3(f)
}

/** Column major 3x3 matrix with float entries*/
case class Matrix3( a00: Float, a01: Float, a02: Float,   //COLUMN 0
                    a10: Float, a11: Float, a12: Float,   //COLUMN 1
                    a20: Float, a21: Float, a22: Float)   //COLUMN 2
                    {

    def unary_- : Matrix3 = Matrix3( -a00, -a01, -a02,
                                     -a10, -a11, -a12,
                                     -a20, -a21, -a22)

    def +(m: Matrix3): Matrix3 =
        Matrix3(a00 + m.a00, a01 + m.a01, a02 + m.a02,
                a10 + m.a10, a11 + m.a11, a12 + m.a12,
                a20 + m.a20, a21 + m.a21, a22 + m.a22)

    def -(m: Matrix3): Matrix3 = this + (-m)

    def *(m: Matrix3): Matrix3 =
        Matrix3(a00 * m.a00 + a10 * m.a01 + a20 * m.a02,
                a01 * m.a00 + a11 * m.a01 + a21 * m.a02,
                a02 * m.a00 + a12 * m.a01 + a22 * m.a02,

                a00 * m.a10 + a10 * m.a11 + a20 * m.a12,
                a01 * m.a10 + a11 * m.a11 + a21 * m.a12,
                a02 * m.a10 + a12 * m.a11 + a22 * m.a12,

                a00 * m.a20 + a10 * m.a21 + a20 * m.a22,
                a01 * m.a20 + a11 * m.a21 + a21 * m.a22,
                a02 * m.a20 + a12 * m.a21 + a22 * m.a22)

    def *(v: Vector3): Vector3 =
        Vector3(a00 * v.x + a10 * v.y + a20 * v.z,
                a01 * v.x + a11 * v.y + a21 * v.z,
                a02 * v.x + a12 * v.y + a22 * v.z)

    def transpose: Matrix3 = Matrix3( a00, a10, a20,
                                      a01, a11, a21,
                                      a02, a12, a22)
    def t = transpose

    def determinant: Float = a00 * (a11*a22 - a21*a12) - a10 * (a01*a22 - a02*a21) + a20 * (a01*a12 - a11*a02)

    def det = determinant

    def toMatrix4: Matrix4 = Matrix4(a00, a01, a02, 0f,
                                     a10, a11, a12, 0f,
                                     a20, a21, a22, 0f,
                                      0f,  0f,  0f, 1f)
    def expand: Matrix4 = toMatrix4

    def inverse: Matrix3 = {
        val dett = 1/det
        (dett * Matrix3(a11*a22 - a21*a12, a02*a21 - a01*a22, a01*a12 - a02*a11,
                        a12*a20 - a10*a22, a00*a22 - a02*a20, a10*a02 - a00*a11,
                        a10*a21 - a20*a11, a20*a01 - a00*a21, a00*a11 - a10*a01)).t
    }

    def inv: Matrix3 = inverse

}
