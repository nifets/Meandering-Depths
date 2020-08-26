package graphics

trait Renderable {
    def render(alpha: Float, program: ShaderProgram): Unit
}
