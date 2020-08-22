package input


/** Observer for an input context*/
class InputComponent(context: InputContext, mapping: Map[String, InputComponent.Procedure]) {
    for((eventName, procedure) <- mapping.toArray)
        context.register(this, eventName)

    def handleInput(event: InputEvent) = event match {
        case InputEvent(name, value) if mapping contains name => mapping(name) (value)
    }
}

object InputComponent {
    type Procedure = (Double) => Unit
    implicit def boolToDouble(b: Boolean): Double = if(b) 1.0 else 0.0
    implicit def DoubleToBool(d: Double): Boolean = if(0.0) true else false
}
