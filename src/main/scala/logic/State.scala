package logic

//Model class --
class State {
    //temporary
    private var tickCounter = 0
    def update() = {
        tickCounter += 1
        println("tickCounter: " + tickCounter)
    }
    def render(alpha: Double) = {
        println("rendering... " + alpha)
    }
}
