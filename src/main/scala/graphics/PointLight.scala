package graphics

import maths._

/** Represents a light source that emits light in every direction, its radius depending on the given constant, linear and quadratic values. */
class PointLight(var position: Vector3,val constant: Float,val linear: Float,val quadratic: Float,val colour: Vector3)
