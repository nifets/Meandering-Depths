#version 430 core
//MARCHING CUBES COMPUTE SHADER

#define eps 0.000001

#define CHUNK_SIZE = gl_NumWorkGroups * gl_WorkGroupSize.x * gl_WorkGroupSize.y * gl_WorkGroupSize.z

layout(local_size_x = 4, local_size_y = 4, local_size_z = 4) in;

layout(rgba32f, binding = 0) readonly uniform image3D inputImg; //INPUT: 3D DENSITY FIELD

struct Vertex {             //12 floats
    vec3 position;
    float padding1;
    vec3 colour;
    float padding2;
    vec3 normal;
    float padding3;
};

struct Triangle {           //36 floats
    Vertex vertex[3];
};

layout(std430, binding = 1) coherent restrict writeonly buffer Soutput { //OUTPUT: SEQUENCE OF TRIANGLES
    Triangle mesh[];
};

layout(binding = 2) uniform atomic_uint numberOfTriangles; //ATOMIC COUNTER OF NUMBER OF TRIANGLES

layout(std430, binding = 3) buffer Smctable { //AUXILIARY LOOK-UP TABLES
    int MCTable[256][16];
    int edgePoints[12][2];
    int fixCorners[8];
};

        /*      .2------3          .*--2---*
              .' |    .'|        .11|    10|
             6---+--7'  |       *-6-+--*'  1
             |   y  |   |       |   3  |   |
             |  ,0-x+---1       7  ,*--+-0-*
             |.z    | .'        |.8    5 .9
             4------5'          *--4---*'                                     */




//Returns the index of the polygonisation of a given cube (based on density of its corners)
int cubeIndex(float corners[8]) {
    float fixedCorners[8];
    for (int i = 0; i < 8; i++)
        fixedCorners[i] = corners[fixCorners[i]];
    int index = 0;
    int power = 1; //2^i
    for (int i = 0; i < 8; i++) {
        if (fixedCorners[i] > eps)
            index += power;         //(+= 2^i)
        power *= 2;
    }
    return index;
}

//Used to convert cube index to its binary representation (limited to 3 bits)
ivec3 intToBits(int n) {
    return ivec3(n % 2, n/2 % 2, n/4 % 2);
}

//val0 <- [-1,0], val1 <- [0,1] interpolation is done around 0
float interpolate(float val0, float val1) {
    if (val1 < eps)
        return 1.0;
    if (val0 > -eps)
        return 0.0;
    return -val0 / (val1 - val0);
}

vec3 vec3Interpolation(vec3 vertex0, vec3 vertex1, float alpha) {
    return vertex0 + (vertex1 - vertex0) * alpha;
}

//Find the vector perpendicular to the given triangle
vec3 triangleNormal(vec3 vertex0, vec3 vertex1, vec3 vertex2) {
    vec3 edge1 = vertex1 - vertex0;
    vec3 edge2 = vertex2 - vertex0;
    return normalize(cross(edge1, edge2));
}

void main() {

    ivec3 cubeCoords = ivec3(gl_GlobalInvocationID.x,gl_GlobalInvocationID.y,gl_GlobalInvocationID.z);
    //ivec3 cubeCoords = gl_GlobalInvocationID;
    float density[8];
    vec3 colour[8];
    for (int i = 0; i < 8; i++) {
        ivec3 offset = intToBits(i);
        vec4 rawData = imageLoad(inputImg, cubeCoords + offset);
        density[i] = rawData.w;
        colour[i] = rawData.xyz;
    }

    const int index = cubeIndex(density);

    //MESH
    for (int i = 0; i < 5 && MCTable[index][3*i] != -1; i++) {
        //TRIANGLE i
        Triangle triangle;
        for (int j = 0; j < 3; j++) {
            //POINT j
            int edge = MCTable[index][3*i + j];
            int vertex0 = edgePoints[edge][0];
            int vertex1 = edgePoints[edge][1];


            if (density[vertex0] > density[vertex1]) {
                int aux = vertex0;
                vertex0 = vertex1;
                vertex1 = aux;
            }

            //density[vertex0] <= 0 <= density[vertex1]
            float alpha = interpolate(density[vertex0], density[vertex1]);
            vec3 vertex0coords = cubeCoords + intToBits(vertex0);
            vec3 vertex1coords = cubeCoords + intToBits(vertex1);

            //SET TRIANGLE VERTICES POSITION
            triangle.vertex[j].position = vec3Interpolation(vertex0coords , vertex1coords, alpha);
            //SET TRIANGLE VERTEX COLOUR
            triangle.vertex[j].colour = vec3Interpolation(colour[vertex0], colour[vertex1], alpha);
            //might be better to have all the vertices have the same colour

            triangle.vertex[j].padding1 = 1.0;
            triangle.vertex[j].padding2 = 1.0;
            triangle.vertex[j].padding3 = 1.0;
        }

        //SET TRIANGLE NORMAL
        vec3 normal = triangleNormal(triangle.vertex[0].position, triangle.vertex[1].position, triangle.vertex[2].position);
        //vec3 colour = triangle.vertex[0].colour + triangle.vertex[1].colour + triangle.vertex[2].colour;
        for (int j = 0; j < 3; j++) {
            triangle.vertex[j].normal = normal;
            //triangle.vertex[j].colour = colour;
        }
        //WRITE TRIANGLE TO OUTPUT
        uint dataIndex = atomicCounterIncrement(numberOfTriangles);
        mesh[dataIndex] = triangle;
    }
    barrier();
}
