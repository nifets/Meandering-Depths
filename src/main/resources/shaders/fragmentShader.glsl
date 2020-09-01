#version 430 core
in vec3 ourColour;
in vec3 ourNormal;
in vec3 fragPos;
in float visibility;

out vec4 color_out;

uniform struct Slight {
    vec3 position;
    vec3 direction;
    float cutOff;
    float outerCutOff;
}light;

void main()
{
    vec3 lightColor = vec3(1.0, 1.0, 1.0);
    vec3 lightDir = normalize(light.position.xyz - fragPos);

    float ambientStrength = 0.4;
    vec3 ambient = ambientStrength * lightColor;

    float diffuseStrength = max(dot(-ourNormal, lightDir), 0.0f);
    vec3 diffuse = diffuseStrength * lightColor;


    float theta = dot(-lightDir, normalize(-light.direction.xyz));
    float eps = light.cutOff - light.outerCutOff;
    float intensity = 1.0 - clamp((theta - light.outerCutOff) / eps, 0.0, 0.9);

    float dist = length(light.position.xyz - fragPos);

    vec3 result = (ambient + + diffuse * 0.2 + (diffuse  * (-0.2 + intensity * (1.5/(1.0f + 0.049*dist + 0.0032 * dist * dist))))) * ourColour;
    color_out = mix(vec4(0.34,0.1,0.64,1),vec4(result, 1), visibility);
}
