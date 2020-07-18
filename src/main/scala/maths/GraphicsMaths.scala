package maths

import breeze.linalg._
import scala.math._

object GraphicsMaths {

  //State: Vector3 is a 3 dimensional vector of floats
  type Vector3 = DenseVector[Float]
  def Vector3(): Vector3 = DenseVector.zeros[Float](3)
  def Vector3(a: Float, b: Float, c: Float): Vector3 = DenseVector[Float](a,b,c)

  //State: Vector4 is a 4 dimensional vector of floats
  type Vector4 = DenseVector[Float]
  def Vector4(): Vector4 = DenseVector.zeros[Float](3)
  def Vector4(a: Float, b: Float, c: Float, d: Float): Vector4 = DenseVector[Float](a,b,c,d)
  def Vector4(v: Vector3, w: Float): Vector4 = DenseVector[Float](v(0),v(1),v(2),w)

  //State: Matrix4 is a 4x4 matrix of floats
  type Matrix4 = DenseMatrix[Float]
  def Matrix4(): Matrix4 = DenseMatrix.eye[Float](4)

  //expands 3x3 matrix to 4x4 matrix
  def expand(a: DenseMatrix[Float]): Matrix4 = {
    require(a.rows == 3 && a.cols == 3)
    DenseMatrix.horzcat(DenseMatrix.vertcat(a,DenseMatrix.zeros[Float](1,3)),DenseMatrix((0F),(0F),(0F),(1F)))
  }

  //creates a rotation matrix corresponding to a rotation of angle around axis
  //TODO: compute the matrix using quaternions to reduce precision loss
  def rotate(axis: Vector3, angle: Double): Matrix4 = {
    val u = normalize(axis)
    val c = cos(angle%(2*Pi)).toFloat
    val s = sin(angle%(2*Pi)).toFloat
    var R = c * DenseMatrix.eye[Float](3) + (1 - c) * u * u.t
    R(0,1) -= s * u(2); R(0,2) += s * u(1); R(1,0) += s * u(2); R(1,2) -= s * u(0); R(2,0) -= s * u(1); R(2,1) += s * u(0)
    expand(R)
  }

  //creates a translation matrix corresponding to a translation by displacement
  def translate(displacement: Vector3): Matrix4 = {
    var m = DenseMatrix.eye[Float](4)
    m(0,3) = displacement(0); m(1,3) = displacement(1); m(2,3) = displacement(2)
    m
  }

  //creates a scaling matrix
  def scale(x: Float, y: Float, z: Float): Matrix4 = diag(DenseVector(x,y,z,1F))

  //creates a perspective projection matrix given the value of the vertical
  // field of view (in radians), the aspect ratio (width/height) and how
  // close the near and far clipping planes are
  // far - near should be as small as possible to avoid z-fighting
  def perspectiveProjection
  (FoV: Double, aspectRatio: Float, nearPlane: Float, farPlane: Float): Matrix4 = {
    var m =  DenseMatrix.zeros[Float](4,4)
    val ct = (1 / tan(FoV/2)).toFloat
    m(1,1) = ct
    m(0,0) = ct / aspectRatio
    m(2,2) = -(farPlane + nearPlane)/(farPlane - nearPlane)
    m(2,3) = -2 * farPlane * nearPlane/(farPlane - nearPlane)
    m(3,2) = -1
    m
  }
}
