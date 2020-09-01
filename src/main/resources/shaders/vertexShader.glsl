#version 430 core
layout (location=0) in vec4 position;
layout (location=1) in vec4 colour;
layout (location=2) in vec4 normal;

out vec3 ourColour;
out vec3 ourNormal;
out vec3 fragPos;
out float visibility;

uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform mat4 worldTransform;
uniform mat4 normalTransform;

const float density = 0.0125;
const float gradient = 5.5;

void main()
{
    vec4 worldPosition = worldTransform * position;
    vec4 viewPosition = viewTransform * worldPosition;
    gl_Position = projectionTransform * viewPosition;

    float dist = length(viewPosition.xyz);
    visibility = clamp(exp(-pow((dist * density), gradient)),0.0,1.0);
    fragPos = vec3(worldTransform * position);
    ourColour = vec3(colour);
    ourNormal = normalize(mat3(normalTransform) * vec3(normal));
    //ourNormal = normalize(vec3(normal));
}
