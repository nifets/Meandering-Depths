#version 430 core

flat in uint material_id;
in vec3 normal;
in vec3 fragPos;
in vec3 viewPos;
in float visibility;
in float colourVar;

out vec4 color_out;

#define MAX_MATERIALS 30
#define MAX_LIGHTS 30


struct Material {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

struct SpotLight {
    vec3 position;
    vec3 direction;
    float cutOff;
    float outerCutOff;
};

struct PointLight {
    vec3 position;

    float constant;
    float linear;
    float quadratic;

    vec3 colour;
};

uniform vec3 skyColour;
uniform int numOfPointLights;
uniform PointLight pointLights[MAX_LIGHTS];
uniform Material materials[MAX_MATERIALS];

vec3 computePointLight(PointLight light, vec3 normal, vec3 fragPos, vec3 viewDir, Material mat) {
    vec3 lightDir = normalize(light.position - fragPos);

    //ambient
    vec3 ambient = mat.ambient;

    //diffuse
    float diffuseStrength = max(dot(normal, lightDir), 0.0);
    vec3 diffuse = diffuseStrength * mat.diffuse;

    //specular
    vec3 reflectDir = reflect(-lightDir, normal);
    float specularStrength = pow(max(dot(viewDir, reflectDir), 0.0), mat.shininess);
    vec3 specular = specularStrength * mat.specular;

    //attenuate light strength based on the dist from light source
    float dist = length(light.position - fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * dist + light.quadratic * dist * dist);

    return light.colour * (ambient + diffuse + specular) * attenuation;
}


void main()
{
    vec3 viewDir = normalize(viewPos - fragPos);
    Material material = materials[material_id];

    //Calculate effect of each light source on the fragment
    vec3 result = vec3(colourVar, colourVar, colourVar);
    for (int i = 0; i < numOfPointLights; i++) {
        result += computePointLight(pointLights[i], normal, fragPos, viewDir, material);
    }

    //Apply fog
    color_out = vec4(mix(skyColour, result, visibility), 1.0);
}
