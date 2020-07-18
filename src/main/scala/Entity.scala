
import physics.PhysicsComponent
import graphics.RenderComponent
import maths.GraphicsMaths._

trait Entity {
    val renderComponent: RenderComponent
    val physicsComponent: PhysicsComponent
    var position = Vector3()

}
