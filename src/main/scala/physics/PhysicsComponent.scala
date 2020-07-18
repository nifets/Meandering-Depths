package physics

import maths.GraphicsMaths._
//Describes how an entity interacts with the world
//Holds data such as collision boxes, mass, velocity and methods to resolve interactions with the world

class PhysicsComponent {
    var mass: Float = 0
    var velocity = Vector3()
}
