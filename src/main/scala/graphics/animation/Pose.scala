package graphics.animation

import maths._
import org.lwjgl.assimp._




case class Pose(val jointTransforms: Array[JointTransform]) {
    val numberOfJoints = jointTransforms.length

    def lerp(alpha: Float, that: Pose): Pose =
        Pose(this.jointTransforms zip that.jointTransforms map
            ((p: (JointTransform, JointTransform)) => p._1.lerp(alpha,p._2)))

    override def toString: String = jointTransforms.toList.toString

}
