package graphics


import org.lwjgl.opengl.GL11.GL_TRUE
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL43._
import org.lwjgl.system._

import maths._

/** Facade for OpenGL shader program*/
class ShaderProgram {
    protected val id = glCreateProgram()

    def use() = {
        if(ShaderProgram.currentProgram != Some(this)) {
            glUseProgram(id)
            ShaderProgram.currentProgram = Some(this)
        }
    }

    /** Setting the uniform requires the shader program to be in use*/
    def setUniform(name: String, value: Int) = {
        use()
        glUniform1i(glGetUniformLocation(id, name), value)
    }


    def setUniform(name: String, value: Float) = {
        use()
        glUniform1f(glGetUniformLocation(id, name), value)
    }

    def setUniform(name: String, vector: Vector3): Unit = {
        use()
        glUniform3f(glGetUniformLocation(id, name), vector.x, vector.y, vector.z)
    }

    def setUniform(name: String, vector: Vector4) = {
        use()
        glUniform4f(glGetUniformLocation(id, name), vector.x, vector.y, vector.z, vector.w)
    }

    def setUniform(name: String, m: Matrix4): Unit = {
        use()
        val stack = MemoryStack.stackPush()
        val data = stack.mallocFloat(4*4)
        data.put(m.a00).put(m.a01).put(m.a02).put(m.a03)
            .put(m.a10).put(m.a11).put(m.a12).put(m.a13)
            .put(m.a20).put(m.a21).put(m.a22).put(m.a23)
            .put(m.a30).put(m.a31).put(m.a32).put(m.a33).flip()
        glUniformMatrix4fv(glGetUniformLocation(id, name), false, data)
        MemoryStack.stackPop()
    }

    def setUniform(name: String, m: Material): Unit = {
        use()
        setUniform(name + ".ambient", m.ambient)
        setUniform(name + ".diffuse", m.diffuse)
        setUniform(name + ".specular", m.specular)
        setUniform(name + ".shininess", m.shininess)
    }

    def setUniform(name: String, l: PointLight): Unit = {
        use()
        setUniform(name + ".position", l.position)
        setUniform(name + ".colour", l.colour)
        setUniform(name + ".constant", l.constant)
        setUniform(name + ".linear", l.linear)
        setUniform(name + ".quadratic", l.quadratic)
    }

    //to do: make this generic function work
    /**def setUniform[T](name: String, array: Array[T]): Unit = {
        use()
        val length = array.length
        for (i <- 0 until length) {
            setUniform(name + "[" + i + "]", array(i))
        }
    }*/
    def setUniform(name: String, array: Array[Matrix4]): Unit = {
        use()
        val length = array.length
        for (i <- 0 until length) {
            setUniform(name + "[" + i + "]", array(i))
        }
    }

    def setUniform(name: String, array: Array[PointLight]): Unit = {
        use()
        val length = array.length
        for (i <- 0 until length) {
            setUniform(name + "[" + i + "]", array(i))
        }
    }

    def setUniform(name: String, array: Array[Material]): Unit = {
        use()
        val length = array.length
        for (i <- 0 until length) {
            setUniform(name + "[" + i + "]", array(i))
        }
    }

    def setUniform(name: String, array: Array[Int]) = {
        use()
        val stack = MemoryStack.stackPush()
        val data = stack.mallocInt(array.size)
        data.put(array).flip()
        glUniform1iv(glGetUniformLocation(id, name), data)
        MemoryStack.stackPop()
    }


    def delete() = glDeleteProgram(id)

    protected def attachShader(shader: Shader) = glAttachShader(id, shader.id)

    protected def checkStatus() = {
        val status = glGetProgrami(id, GL_LINK_STATUS)
        if (status != GL_TRUE) {
            throw new RuntimeException(glGetProgramInfoLog(id))
        }
    }

    protected def link() = {
        glLinkProgram(id)
        checkStatus()
    }
}

class ComputeShaderProgram extends ShaderProgram {
    def dispatch(numGroupsX: Int, numGroupsY: Int, numGroupsZ: Int) = {
        glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ)
    }
}

object ShaderProgram {
    private var currentProgram: Option[ShaderProgram] = None

    def vertexFragmentProgram(vertexPath: String, fragmentPath: String): ShaderProgram = {
        val program = new ShaderProgram()
        val vertexShader = Shader(GL_VERTEX_SHADER, vertexPath)
        val fragmentShader = Shader(GL_FRAGMENT_SHADER, fragmentPath)
        program.attachShader(vertexShader)
        program.attachShader(fragmentShader)
        program.link()

        vertexShader.delete()
        fragmentShader.delete()

        program
    }

    def computeProgram(computePath: String): ComputeShaderProgram = {
        val program = new ComputeShaderProgram()

        val computeShader = Shader(GL_COMPUTE_SHADER, computePath)
        println("compute program: created compute shader")
        program.attachShader(computeShader)
        println("attached shader to program")
        program.link()
        println("linked program")
        computeShader.delete()

        program
    }
}
