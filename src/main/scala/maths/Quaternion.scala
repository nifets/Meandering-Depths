package maths

import scala.math._

import scala.language.implicitConversions

object Quaternion {
    /** Quaternion that represents a rotation of degree angle (in radians) about axis
    */
    def axisAngleRotation(axis: Vector3, angle: Float): Quaternion =
        Quaternion(cos(angle/2).toFloat, sin(angle/2).toFloat * axis)

    /** Quaternion that represents a rotation in euler angles (counter clock wise) in order
    */
    def eulerRotation(xAngle: Float, yAngle: Float, zAngle: Float): Quaternion = {
        val cx = cos(xAngle/2).toFloat
        val sx = sin(xAngle/2).toFloat
        val cy = cos(yAngle/2).toFloat
        val sy = sin(yAngle/2).toFloat
        val cz = cos(zAngle/2).toFloat
        val sz = sin(zAngle/2).toFloat

        Quaternion( cx * cy * cz + sx * sy * sz,
                    sx * cy * cz - cx * sy * sz,
                    cx * sy * cz + sx * cy * sz,
                    cx * cy * sz - sx * sy * cz)
    }

    lazy val identity = Quaternion(1f,0f,0f,0f)
    def apply(s: Float, v: Vector3): Quaternion = Quaternion(s, v.x, v.y, v.z)
    implicit def floatToQuaternion(s: Float) = Quaternion(s, 0f, 0f, 0f)
    implicit def vector3ToQuaternion(v: Vector3) = Quaternion(0f, v)
    implicit def quaternionToVector3(q: Quaternion) = q.vector3
}

/** Quaternion for rotations and orientation
    @param s scalar part
    @param x x component of vector part
    @param y y component of vector part
    @param z z component of vector part
*/
case class Quaternion(s: Float, x: Float, y: Float, z: Float) {
    def unary_- : Quaternion = Quaternion(-s, -x, -y, -z)
    def +(q: Quaternion): Quaternion = Quaternion(s + q.s, x + q.x, y + q.y, z + q.z)
    def -(q: Quaternion): Quaternion = this + (-q)
    def *(f: Float): Quaternion = Quaternion(f*s, f*x, f*y, f*z)
    def /(f: Float): Quaternion = Quaternion(s/f, x/f, y/f, z/f)
    def *(q: Quaternion): Quaternion =
        Quaternion( s * q.s - x * q.x - y * q.y - z * q.z,
                    s * q.x + x * q.s + y * q.z - z * q.y,
                    s * q.y - x * q.z + y * q.s + z * q.x,
                    s * q.z + x * q.y - y * q.x + z * q.s)

    def vector3: Vector3 = Vector3(x, y, z)

    def conj: Quaternion = Quaternion(s, -x, -y, -z)

    def norm: Float = sqrt(s*s + x*x + y*y + z*z).toFloat

    def normSquared: Float = s*s + x*x + y*y + z*z

    def normalize: Quaternion = 1f/norm * this

    def inverse: Quaternion = conj / (s*s + x*x + y*y + z*z)

    def inv: Quaternion = inverse

    def lerp(alpha: Float, q: Quaternion): Quaternion =
        Quaternion(Maths.lerp(s,q.s,alpha),
                   Maths.lerp(x,q.x,alpha),
                   Maths.lerp(y,q.y,alpha),
                   Maths.lerp(z,q.z,alpha))
}
