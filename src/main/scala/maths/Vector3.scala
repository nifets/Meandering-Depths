package maths

import scala.math._

import scala.language.implicitConversions

object Vector3 {

    def apply(f: Float): Vector3 = Vector3(f,f,f)

    //A bit dirty, but this is to allow operations such as f * vec
    //(in maths you tend to put constants before vectors)
    implicit def floatToVector3(f: Float): Vector3 = Vector3(f)
}

case class Vector3(x: Float, y: Float, z: Float) {

    def unary_- : Vector3 = Vector3(-x, -y, -z)
    def +(v: Vector3): Vector3 = Vector3(x + v.x, y + v.y, z + v.z)
    def -(v: Vector3): Vector3 = Vector3(x - v.x, y - v.y, z - v.z)
    def *(f: Float): Vector3 = Vector3(f*x, f*y, f*z)
    def /(f: Float): Vector3 = Vector3(x/f,y/f,z/f)

    //element wise multiplication
    def *(v: Vector3): Vector3 = Vector3(x * v.x, y * v.y, z * v.z)

    def cross(v: Vector3): Vector3 =
        Vector3(y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.z)

    def dot(v: Vector3): Float = x * v.x + y * v.y + z * v.z

    def normSquared = this dot this
    def norm: Float = sqrt(normSquared).toFloat

    def normalize: Vector3 = 1f/norm * this

    def distanceTo(v: Vector3): Float = (this - v).norm

    def rotate(q: Quaternion): Vector3 = q * this * q.inverse

    //use as: v = u lerp(alpha) w
    def lerp(alpha: Float,v: Vector3): Vector3 =
        Vector3(Maths.lerp(x, v.x, alpha),
                Maths.lerp(y, v.y, alpha),
                Maths.lerp(z, v.z, alpha))

    def floor: Vector3 = Vector3(x.floor, y.floor, z.floor)

    def toVector3i: Vector3i = Vector3i(x.floor.toInt, y.floor.toInt, z.floor.toInt)

    def xxx = Vector3(x, x, x)
    def xxy = Vector3(x, x, y)
    def xxz = Vector3(x, x, z)

    def xyx = Vector3(x, y, x)
    def xyy = Vector3(x, y, y)
    def xyz = Vector3(x, y, z)

    def xzx = Vector3(x, z, x)
    def xzy = Vector3(x, z, y)
    def xzz = Vector3(x, z, z)

    def yxx = Vector3(y, x, x)
    def yxy = Vector3(y, x, y)
    def yxz = Vector3(y, x, z)

    def yyx = Vector3(y, y, x)
    def yyy = Vector3(y, y, y)
    def yyz = Vector3(y, y, z)

    def yzx = Vector3(y, z, x)
    def yzy = Vector3(y, z, y)
    def yzz = Vector3(y, z, z)

    def zxx = Vector3(z, x, x)
    def zxy = Vector3(z, x, y)
    def zxz = Vector3(z, x, z)

    def zyx = Vector3(z, y, x)
    def zyy = Vector3(z, y, y)
    def zyz = Vector3(z, y, z)

    def zzx = Vector3(z, z, x)
    def zzy = Vector3(z, z, y)
    def zzz = Vector3(z, z, z)

}
