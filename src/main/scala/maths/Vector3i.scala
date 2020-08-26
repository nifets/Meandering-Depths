package maths

import scala.math._

import scala.language.implicitConversions

object Vector3i {
    implicit def intToVector3i(i: Int): Vector3i = Vector3i(i,i,i)
}

case class Vector3i(x: Int, y: Int, z: Int) {
    def unary_- : Vector3i = Vector3i(-x, -y, -z)
    def +(v: Vector3i): Vector3i = Vector3i(x + v.x, y + v.y, z + v.z)
    def -(v: Vector3i): Vector3i = Vector3i(x - v.x, y - v.y, z - v.z)
    def *(i: Int): Vector3i = Vector3i(i*x, i*y, i*z)

    //element wise multiplication
    def *(v: Vector3i): Vector3i = Vector3i(x * v.x, y * v.y, z * v.z)

    def dot(v: Vector3i): Int = x * v.x + y * v.y + z * v.z

    def normSquared = this dot this
    def norm: Float = sqrt(normSquared).toFloat

    def distanceTo(v: Vector3i): Float = (this - v).norm

    def toVector3: Vector3 = Vector3(x.toFloat, y.toFloat, z.toFloat)


    def xxx = Vector3i(x, x, x)
    def xxy = Vector3i(x, x, y)
    def xxz = Vector3i(x, x, z)

    def xyx = Vector3i(x, y, x)
    def xyy = Vector3i(x, y, y)
    def xyz = Vector3i(x, y, z)

    def xzx = Vector3i(x, z, x)
    def xzy = Vector3i(x, z, y)
    def xzz = Vector3i(x, z, z)

    def yxx = Vector3i(y, x, x)
    def yxy = Vector3i(y, x, y)
    def yxz = Vector3i(y, x, z)

    def yyx = Vector3i(y, y, x)
    def yyy = Vector3i(y, y, y)
    def yyz = Vector3i(y, y, z)

    def yzx = Vector3i(y, z, x)
    def yzy = Vector3i(y, z, y)
    def yzz = Vector3i(y, z, z)

    def zxx = Vector3i(z, x, x)
    def zxy = Vector3i(z, x, y)
    def zxz = Vector3i(z, x, z)

    def zyx = Vector3i(z, y, x)
    def zyy = Vector3i(z, y, y)
    def zyz = Vector3i(z, y, z)

    def zzx = Vector3i(z, z, x)
    def zzy = Vector3i(z, z, y)
    def zzz = Vector3i(z, z, z)
}
