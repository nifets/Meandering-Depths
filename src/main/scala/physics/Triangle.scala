package physics

import maths._

/** Represents a one sided triangle, with counter-clockwise orientation*/
case class Triangle(a: Vector3, b: Vector3, c: Vector3) {

    /** @param rayPos - the origin of the ray
        @param rayDir - the direction of the ray
        Calculates the point of intersection between the triangle and the given ray, returning None if the two do not intersect, or Some(t) where t is the distance from the rayPos to the point of intersection. The point of intersection can then be calculated as O = rayPos + t * rayDir. If the ray meets the back of the triangle, it does not count as a collision.

        The computation is done using the Moller-Trumbore method, converting the space to barycentric coordinates and using Cramer's rule to solve the resulting system of linear equations, culling some of the non-intersecting cases as early as possible. The determinant is a measure of the angle between the ray and the triangle, quickly approaching 0 as the ray becomes parallel to the triangle, which yields unstable results. */
    def collide(rayPos: Vector3, rayDir: Vector3): Option[Float] = {
        val edge1 = b - a
        val edge2 = c - a
        val newPos = rayPos - a
        val p = rayDir cross edge2
        val q = newPos cross edge1

        val det = p dot edge1

        if (det < 0.00001f)
            return None


        val u = p dot newPos
        if (u < 0f || u > det)
            return None


        val v = q dot rayDir
        if (v < 0f || u + v > det)
            return None

        val t = q dot edge2 /det

        Some(t)
    }
}
