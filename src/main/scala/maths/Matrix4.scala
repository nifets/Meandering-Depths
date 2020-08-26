package maths

import scala.math._

import scala.language.implicitConversions

object Matrix4 {
    lazy val identity: Matrix4 = Matrix4(1f)

    def diag(x: Float, y: Float, z: Float, w: Float): Matrix4 =
        Matrix4(x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, z, 0f,
                0f, 0f, 0f, w)

    def diag(v: Vector4): Matrix4 = diag(v.x,v.y,v.z,v.w)

    def scaling(x: Float, y: Float, z: Float): Matrix4 = diag(x,y,z,1f)

    def translation(x: Float, y: Float, z: Float): Matrix4 = Matrix4(1f, 0f, 0f, 0f,
                                                                     0f, 1f, 0f, 0f,
                                                                     0f, 0f, 1f, 0f,
                                                                     x,  y,  z,  1f)

    def translation(v: Vector3): Matrix4 = translation(v.x, v.y, v.z)

    def rotation(q: Quaternion): Matrix4 = {
        val ins = 1f/q.normSquared //inverse norm squared
        Matrix4(1f - 2*ins*(q.y*q.y + q.z*q.z), 2*ins*(q.x*q.y + q.z*q.s), 2*ins*(q.x*q.z - q.y*q.s), 0f,
                2*ins*(q.x*q.y - q.z*q.s), 1f - 2*ins*(q.x*q.x + q.z*q.z), 2*ins*(q.y*q.z + q.x*q.s), 0f,
                2*ins*(q.x*q.z + q.y*q.s), 2*ins*(q.y*q.z - q.x*q.s), 1f - 2*ins*(q.x*q.x + q.y*q.y), 0f,
                0f                       , 0f                       , 0f                            , 1f)
    }


    /** Creates a perspective projection matrix
        @param fov horizontal field of view angle, in radians
        @param aspect width/height aspect ratio of the view
        @param near distance from view position to near culling plane
        @param far distance from view position to far culling plane
    */
    def perspective(fov: Float, aspect: Float, near: Float, far: Float): Matrix4 = {
        val cotan = (1 / tan(fov/2)).toFloat
        Matrix4(cotan/aspect,  0f,     0f,                             0f,
                0f,            cotan,  0f,                             0f,
                0f,            0f,     -(far + near)/(far - near),     -1f,
                0f,            0f,     -far * near/(far - near),       0f)
    }


    def apply(): Matrix4 = Matrix4(0f)

    def apply(f: Float): Matrix4 = diag(f,f,f,f)

    def apply(v0: Vector4, v1: Vector4, v2: Vector4, v3: Vector4): Matrix4 =
        Matrix4(v0.x, v0.y, v0.z, v0.w,
                v1.x, v1.y, v1.z, v1.w,
                v2.x, v2.y, v2.z, v2.w,
                v3.x, v3.y, v3.z, v3.w)

    implicit def floatToMatrix4(f: Float): Matrix4 = Matrix4(f)
}

/** Column major 4x4 matrix with float entries*/
case class Matrix4( a00: Float, a01: Float, a02: Float, a03: Float,   //COLUMN 0
                    a10: Float, a11: Float, a12: Float, a13: Float,   //COLUMN 1
                    a20: Float, a21: Float, a22: Float, a23: Float,   //COLUMN 2
                    a30: Float, a31: Float, a32: Float, a33: Float) { //COLUMN 3

    def unary_- : Matrix4 = Matrix4( -a00, -a01, -a02, -a03,
                                    -a10, -a11, -a12, -a13,
                                    -a20, -a21, -a22, -a23,
                                    -a30, -a31, -a32, -a33)

    def +(m: Matrix4): Matrix4 =
        Matrix4(a00 + m.a00, a01 + m.a01, a02 + m.a02, a03 + m.a03,
                a10 + m.a10, a11 + m.a11, a12 + m.a12, a13 + m.a13,
                a20 + m.a20, a21 + m.a21, a22 + m.a22, a23 + m.a23,
                a30 + m.a30, a31 + m.a31, a32 + m.a32, a33 + m.a33)

    def -(m: Matrix4): Matrix4 = this + (-m)

    def *(m: Matrix4): Matrix4 =
        Matrix4(a00 * m.a00 + a10 * m.a01 + a20 * m.a02 + a30 * m.a03,
                a01 * m.a00 + a11 * m.a01 + a21 * m.a02 + a31 * m.a03,
                a02 * m.a00 + a12 * m.a01 + a22 * m.a02 + a32 * m.a03,
                a03 * m.a00 + a13 * m.a01 + a23 * m.a02 + a33 * m.a03,

                a00 * m.a10 + a10 * m.a11 + a20 * m.a12 + a30 * m.a13,
                a01 * m.a10 + a11 * m.a11 + a21 * m.a12 + a31 * m.a13,
                a02 * m.a10 + a12 * m.a11 + a22 * m.a12 + a32 * m.a13,
                a03 * m.a10 + a13 * m.a11 + a23 * m.a12 + a33 * m.a13,

                a00 * m.a20 + a10 * m.a21 + a20 * m.a22 + a30 * m.a23,
                a01 * m.a20 + a11 * m.a21 + a21 * m.a22 + a31 * m.a23,
                a02 * m.a20 + a12 * m.a21 + a22 * m.a22 + a32 * m.a23,
                a03 * m.a20 + a13 * m.a21 + a23 * m.a22 + a33 * m.a23,

                a00 * m.a30 + a10 * m.a31 + a20 * m.a32 + a30 * m.a33,
                a01 * m.a30 + a11 * m.a31 + a21 * m.a32 + a31 * m.a33,
                a02 * m.a30 + a12 * m.a31 + a22 * m.a32 + a32 * m.a33,
                a03 * m.a30 + a13 * m.a31 + a23 * m.a32 + a33 * m.a33)

    def *(v: Vector4): Vector4 =
        Vector4(a00 * v.x + a10 * v.y + a20 * v.z + a30 * v.w,
                a01 * v.x + a11 * v.y + a21 * v.z + a31 * v.w,
                a02 * v.x + a12 * v.y + a22 * v.z + a32 * v.w,
                a03 * v.x + a13 * v.y + a23 * v.z + a33 * v.w)

    def transpose: Matrix4 = Matrix4( a00, a10, a20, a30,
                                      a01, a11, a21, a31,
                                      a02, a12, a22, a32,
                                      a03, a13, a23, a33)
    def t = transpose

    def determinant: Float =
        a00 * (a11 * a22 * a33 + a21 * a32 * a13 + a31 * a12 * a23 - a13 * a22 * a31 - a23 * a32 * a11 - a33 * a12 * a21) -
         a10 * (a01 * a22 * a33 + a21 * a32 * a03 + a31 * a02 * a23 - a03 * a22 * a31 - a23 * a32 * a01 - a33 * a02 * a21) +
         a20 * (a01 * a12 * a33 + a11 * a32 * a03 + a31 * a02 * a13 - a03 * a12 * a31 - a13 * a32 * a01 - a33 * a02 * a11) -
         a30 * (a01 * a12 * a23 + a11 * a22 * a03 + a21 * a02 * a13 - a03 * a12 * a21 - a13 * a22 * a01 - a23 * a02 * a11)

    def det = determinant

    def inverse: Matrix4 = {

        val A2323 = a22 * a33 - a23 * a32
        val A1323 = a21 * a33 - a23 * a31
        val A1223 = a21 * a32 - a22 * a31
        val A0323 = a20 * a33 - a23 * a30
        val A0223 = a20 * a32 - a22 * a30
        val A0123 = a20 * a31 - a21 * a30
        val A2313 = a12 * a33 - a13 * a32
        val A1313 = a11 * a33 - a13 * a31
        val A1213 = a11 * a32 - a12 * a31
        val A2312 = a12 * a23 - a13 * a22
        val A1312 = a11 * a23 - a13 * a21
        val A1212 = a11 * a22 - a12 * a21
        val A0313 = a10 * a33 - a13 * a30
        val A0213 = a10 * a32 - a12 * a30
        val A0312 = a10 * a23 - a13 * a20
        val A0212 = a10 * a22 - a12 * a20
        val A0113 = a10 * a31 - a11 * a30
        val A0112 = a10 * a21 - a11 * a20

        val dett = 1/(a00 * ( a11 * A2323 - a12 * A1323 + a13 * A1223 )
                    - a01 * ( a10 * A2323 - a12 * A0323 + a13 * A0223 )
                    + a02 * ( a10 * A1323 - a11 * A0323 + a13 * A0123 )
                    - a03 * ( a10 * A1223 - a11 * A0223 + a12 * A0123 ))

        Matrix4(dett *  ( a11 * A2323 - a12 * A1323 + a13 * A1223 ),
                dett * -( a01 * A2323 - a02 * A1323 + a03 * A1223 ),
                dett *  ( a01 * A2313 - a02 * A1313 + a03 * A1213 ),
                dett * -( a01 * A2312 - a02 * A1312 + a03 * A1212 ),
                dett * -( a10 * A2323 - a12 * A0323 + a13 * A0223 ),
                dett *  ( a00 * A2323 - a02 * A0323 + a03 * A0223 ),
                dett * -( a00 * A2313 - a02 * A0313 + a03 * A0213 ),
                dett *  ( a00 * A2312 - a02 * A0312 + a03 * A0212 ),
                dett *  ( a10 * A1323 - a11 * A0323 + a13 * A0123 ),
                dett * -( a00 * A1323 - a01 * A0323 + a03 * A0123 ),
                dett *  ( a00 * A1313 - a01 * A0313 + a03 * A0113 ),
                dett * -( a00 * A1312 - a01 * A0312 + a03 * A0112 ),
                dett * -( a10 * A1223 - a11 * A0223 + a12 * A0123 ),
                dett *  ( a00 * A1223 - a01 * A0223 + a02 * A0123 ),
                dett * -( a00 * A1213 - a01 * A0213 + a02 * A0113 ),
                dett *  ( a00 * A1212 - a01 * A0212 + a02 * A0112 )).t
    }

    def inv: Matrix4 = inverse

}
