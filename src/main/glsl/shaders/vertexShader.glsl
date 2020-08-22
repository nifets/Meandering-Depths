#version 430 core
layout (location=0) in vec4 position;
layout (location=1) in vec4 colour;
layout (location=2) in vec4 normal;

out vec3 ourColour;
out vec3 ourNormal;
out vec3 fragPos;

uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform mat4 worldTransform;
uniform mat4 normalTransform;

void main()
{
    gl_Position = projectionTransform * viewTransform * worldTransform *  position;
    fragPos = vec3(worldTransform * position);
    ourColour = vec3(colour);
    ourNormal = normalize(mat3(normalTransform) * vec3(normal));
    //ourNormal = normalize(vec3(normal));
}