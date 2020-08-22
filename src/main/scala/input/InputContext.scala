package input

import utils._
import graphics._

class InputContext(window: Window, mapping: InputMapping)  {

    private val inputObservers = new Multimap[String, InputComponent]()

    def handle(input: RawInput) = {
        for (event <- mapping.getEventsfromInput(input))
            for (observer <- inputObservers(event.name))
                observer.handleInput(event)
    }

    def register  (observer: InputComponent, eventName: String) = inputObservers += (eventName, observer)

    def unregister(observer: InputComponent, eventName: String) = inputObservers -= (eventName, observer)
}
