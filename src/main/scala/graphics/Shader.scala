package graphics

import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL11.GL_TRUE
import scala.io.Source


/** Facade for OpenGL shader*/
class Shader(shaderType: Int) {

    val id = glCreateShader(shaderType)

    private def source(source: String) = glShaderSource(id, source)

    private def compile() = {
        glCompileShader(id)
        checkStatus()
    }

    private def checkStatus() = {
        val status = glGetShaderi(id, GL_COMPILE_STATUS)
        if (status != GL_TRUE)
            throw new RuntimeException(glGetShaderInfoLog(id))
    }

    def delete() = glDeleteShader(id)
}

object Shader {

    def apply(shaderType: Int, path: String): Shader = {
        val shader = new Shader(shaderType)
        val source = Source.fromResource(path)
        shader.source(source.mkString)
        source.close
        shader.compile()
        shader
    }
}
