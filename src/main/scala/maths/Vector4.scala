package maths

import scala.math._

import scala.language.implicitConversions

object Vector4 {
    def apply(v: Vector3, f: Float): Vector4 = Vector4(v.x,v.y,v.z,f)
    //A bit dirty, but this is to allow operations such as f * vec
    //(in maths you tend to put constants before vectors)
    implicit def floatToVector4(f: Float) = Vector4(f, f, f, f)
}

case class Vector4(x: Float, y: Float, z: Float, w: Float) {

    def unary_- : Vector4 = Vector4(-x, -y, -z, -w)
    def +(v: Vector4): Vector4 = Vector4(x + v.x, y + v.y, z + v.z, w + v.w)
    def -(v: Vector4): Vector4 = Vector4(x - v.x, y - v.y, z - v.z, w - v.w)
    def *(f: Float): Vector4 = Vector4(f*x, f*y, f*z, f*w)
    def /(f: Float): Vector4 = Vector4(x/f,y/f,z/f, w/f)

    // Element-wise multiplication
    def *(v: Vector4): Vector4 = Vector4(x * v.x, y * v.y, z * v.z, w * v.w)

    def dot(v: Vector4): Float = x * v.x + y * v.y + z * v.z + w * v.w

    def normSquared = this dot this
    def norm: Float = sqrt(normSquared).toFloat

    def normalize: Vector4 = 1f/norm * this

    def distanceTo(v: Vector4): Float = (this - v).norm

    //use as: v = u lerp(alpha) w
    def lerp(alpha: Float, v: Vector4): Vector4 =
        Vector4(Maths.lerp(x, v.x, alpha),
                Maths.lerp(y, v.y, alpha),
                Maths.lerp(z, v.z, alpha),
                Maths.lerp(w, v.w, alpha))

    def floor: Vector4 = Vector4(x.floor, y.floor, z.floor, w.floor)

    def xxx = Vector3(x, x, x)
    def xxy = Vector3(x, x, y)
    def xxz = Vector3(x, x, z)
    def xxw = Vector3(x, x, w)

    def xyx = Vector3(x, y, x)
    def xyy = Vector3(x, y, y)
    def xyz = Vector3(x, y, z)
    def xyw = Vector3(x, y, w)

    def xzx = Vector3(x, z, x)
    def xzy = Vector3(x, z, y)
    def xzz = Vector3(x, z, z)
    def xzw = Vector3(x, z, w)

    def yxx = Vector3(y, x, x)
    def yxy = Vector3(y, x, y)
    def yxz = Vector3(y, x, z)
    def yxw = Vector3(y, x, w)

    def yyx = Vector3(y, y, x)
    def yyy = Vector3(y, y, y)
    def yyz = Vector3(y, y, z)
    def yyw = Vector3(y, y, w)

    def yzx = Vector3(y, z, x)
    def yzy = Vector3(y, z, y)
    def yzz = Vector3(y, z, z)
    def yzw = Vector3(y, z, w)

    def zxx = Vector3(z, x, x)
    def zxy = Vector3(z, x, y)
    def zxz = Vector3(z, x, z)
    def zxw = Vector3(z, x, w)

    def zyx = Vector3(z, y, x)
    def zyy = Vector3(z, y, y)
    def zyz = Vector3(z, y, z)
    def zyw = Vector3(z, y, w)

    def zzx = Vector3(z, z, x)
    def zzy = Vector3(z, z, y)
    def zzz = Vector3(z, z, z)
    def zzw = Vector3(z, z, w)

    def wxx = Vector3(w, x, x)
    def wxy = Vector3(w, x, y)
    def wxz = Vector3(w, x, z)
    def wxw = Vector3(w, x, w)

    def wyx = Vector3(w, y, x)
    def wyy = Vector3(w, y, y)
    def wyz = Vector3(w, y, z)
    def wyw = Vector3(w, y, w)

    def wzx = Vector3(w, z, x)
    def wzy = Vector3(w, z, y)
    def wzz = Vector3(w, z, z)
    def wzw = Vector3(w, z, w)

}
