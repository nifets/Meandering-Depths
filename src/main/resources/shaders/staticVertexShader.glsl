#version 430 core

layout (location=0) in vec3 in_position;
layout (location=1) in float in_material_id;
layout (location=2) in vec3 in_normal;
layout (location=3) in float in_colour_var;

flat out uint material_id;
out float colourVar;
out vec3 normal;
out vec3 fragPos;
out vec3 viewPos;
out float visibility;

uniform vec3 cameraPos;
uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform mat4 worldTransform;
uniform mat4 normalTransform;

const float fog_density = 0.0125;
const float fog_gradient = 3.5;

float getFogVisibility(float dist) {
    return clamp(exp(-pow((dist * fog_density), fog_gradient)),0.0,1.0);
}

void main()
{
    vec4 worldPosition = worldTransform * vec4(in_position, 1.0);
    vec4 viewPosition = viewTransform * worldPosition;

    uint mid = uint(in_material_id);
    material_id = mid;
    gl_Position = projectionTransform * viewPosition;
    visibility = getFogVisibility(length(viewPosition.xyz));
    fragPos = vec3(worldPosition);
    colourVar = in_colour_var;
    viewPos = cameraPos;
    normal = normalize(mat3(normalTransform) * in_normal);
}
