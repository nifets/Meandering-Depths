#version 430 core
layout (location=0) in vec3 in_position;
layout (location=1) in float in_material_id;
layout (location=2) in vec3 in_normal;
layout (location=3) in float in_colour_var;
layout (location=4) in vec4 in_jointIds;
layout (location=5) in vec4 in_jointWeights;

#define MAX_JOINTS 100
#define MAX_WEIGHTS 4

flat out uint material_id;
out vec3 normal;
out vec3 fragPos;
out vec3 viewPos;
out float colourVar;
out float visibility;

uniform vec3 cameraPos;
uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform mat4 worldTransform;
uniform mat4 normalTransform;
uniform mat4 jointTransforms[MAX_JOINTS];

const float fog_density = 0.0125;
const float fog_gradient = 5.5;

float getFogVisibility(float dist) {
    return clamp(exp(-pow((dist * fog_density), fog_gradient)),0.0,1.0);
}

void main()
{
    // Position and normal of vertex after applying skinning
    vec4 posePosition = vec4(0.0);
    vec4 poseNormal = vec4(0.0);
    for (int i = 0; i < MAX_WEIGHTS; i++) {
        mat4 jointTransform = jointTransforms[int(in_jointIds[i])];
        float jointWeight = in_jointWeights[i];

        posePosition += jointTransform * vec4(in_position, 1.0) * jointWeight;

        poseNormal += jointTransform * vec4(in_normal, 0.0) * jointWeight;
    }

    vec4 worldPosition = worldTransform * posePosition;
    vec4 viewPosition = viewTransform * worldPosition;

    uint mid = uint(in_material_id);
    material_id = mid;
    gl_Position = projectionTransform * viewPosition;
    visibility = getFogVisibility(length(viewPosition.xyz));
    fragPos = vec3(worldPosition);
    viewPos = cameraPos;
    colourVar = in_colour_var;
    normal = normalize(mat3(normalTransform) * vec3(poseNormal));
}
