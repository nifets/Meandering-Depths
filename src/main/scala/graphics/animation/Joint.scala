package graphics.animation

import maths._


case class JointTransform(val position: Vector3 = Vector3(0f), val orientation: Quaternion = Quaternion.identity, val scaling: Vector3 = Vector3(1f)) {
    def lerp(alpha: Float, that: JointTransform): JointTransform =
        JointTransform(position.lerp(alpha, that.position), orientation.lerp(alpha, that.orientation),
                       scaling.lerp(alpha, that.scaling))

}

class Joint(val id: Int, val children: List[Joint], val inverseBindTransform: Matrix4) {
    var transform = JointTransform(Vector3(0f), Quaternion.identity, Vector3(1f))

    def position = transform.position
    def orientation = transform.orientation
    def scaling = transform.scaling

    def localTransform: Matrix4 = Matrix4.translation(transform.position) *
        Matrix4.rotation(transform.orientation) * Matrix4.scaling(transform.scaling)

    /**The matrix that gets loaded to the GPU in order to move the vertices in the model to the right place.*/
    var animationTransform = Matrix4.identity

}
