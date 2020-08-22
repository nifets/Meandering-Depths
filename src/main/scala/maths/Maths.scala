package maths
import scala.math._

private [maths] object Maths {
    def clamp(f: Float, minf: Float, maxf: Float): Float = {
        min(max(f, minf), maxf)
    }

    def lerp(a: Float, b: Float, alpha: Float): Float = {
        (1 - alpha) * a + alpha * b
    }
}
