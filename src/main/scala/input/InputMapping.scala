package input

trait InputMapping {
    def getEventsfromInput(input: RawInput): Set[InputEvent]
}
