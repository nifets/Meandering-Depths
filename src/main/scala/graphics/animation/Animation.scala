package graphics.animation

case class Keyframe(val pose: Pose, val time: Double)

class Animation(val keyframes: Array[Keyframe], val duration: Double) {
    private var currentTime = 0D
    private val numberOfKeyframes = keyframes.length

    def advance(time: Double) = {
        currentTime = (currentTime + time) % duration
    }

    def currentPose: Pose = {
        val (prev,next) = beforeAndAfterKeyframe(currentTime)
        val (pt,nt) = if (prev.time < next.time) (prev.time, next.time)
                      else (prev.time, next.time + duration)

        val alpha = (currentTime - pt) / (nt - pt)
        prev.pose.lerp(alpha.toFloat, next.pose)
    }

    /**Returns the keyframes before and after the given time in the animation. time should be between 0 and length */
    private def beforeAndAfterKeyframe(time: Double): (Keyframe, Keyframe) = {
        var i = 0
        while (i < numberOfKeyframes && keyframes(i).time <= time)
            i += 1

        if (i == numberOfKeyframes) {
            //println((i-1) + " " + 0)
            (keyframes(i - 1), keyframes(0))

        }
        else {
            //println((i-1) + " " + i)
            (keyframes(i - 1), keyframes(i))

        }
    }
}
