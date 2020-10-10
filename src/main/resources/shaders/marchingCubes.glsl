#version 430 core
//MARCHING CUBES COMPUTE SHADER
//Given a sequence of cubic chunks and the isovalue data at each voxel, the shader returns the isosurface at value 0 as an array of triangle data (vertex position, colour variation, material id and normal) for each chunk.

#define eps 0.000001
#define CHUNK_SIZE 20
#define MAX_CHUNKS_PER_COMPUTE 30
#define MAX_TRIANGLES_PER_CUBE 5


            /*      .2------3          .*--2---*
                  .' |    .'|        .11|    10|
                 6---+--7'  |       *-6-+--*'  1
                 |   y  |   |       |   3  |   |
                 |  ,0-x+---1       7  ,*--+-0-*
                 |.z    | .'        |.8    5 .9
                 4------5'          *--4---*'                                     */

//AUXILIARY LOOK-UP TABLES
layout(std430, binding = 0) readonly buffer Smctable {
    int MCTable[256][16];
    int edgePoints[12][2];
    int fixCorners[8];
};

//For some reason, the limit of the group size is really low, so one local group represents only a vertical slice of a cubic chunk.
//number of work groups: x - number of chunks, y - 1, z - CHUNK_SIZE

layout(local_size_x = CHUNK_SIZE, local_size_y = CHUNK_SIZE, local_size_z = 1) in;

//INPUT: stores the location in the world of each chunk
layout(std430, binding = 1) readonly buffer SChunkPos {
    ivec4 chunkPos[MAX_CHUNKS_PER_COMPUTE];
};

//INPUT: material, colour and isovalue data
// x: material id, y: colour var, z: isovalue
layout(std430, binding = 2) readonly buffer Sinput {
    vec4 inputData[MAX_CHUNKS_PER_COMPUTE][CHUNK_SIZE+1][CHUNK_SIZE+1][CHUNK_SIZE+1];
};




struct Vertex {             //8 floats
    vec3 position;
    float material_id;
    vec3 normal;
    float colour_variation;
};

struct Triangle {           //24 floats
    Vertex vertex[3];
};


//OUTPUT: vertex data for each chunk
layout(std430, binding = 3) restrict writeonly buffer Smesh {
    Triangle mesh[];
};

//OUTPUT: number of triangles per chunk
layout(std430, binding = 4) restrict writeonly buffer Svcount {
    int vertexCount[MAX_CHUNKS_PER_COMPUTE];
};

struct CollTriangle { //12 floats
    vec4 vertices[3];
};

struct Voxel { //60 floats  60*4 bytes
    CollTriangle triangles[MAX_TRIANGLES_PER_CUBE];
};

//OUTPUT: collision mesh
layout(std430, binding = 5) writeonly buffer Scoll {
    Voxel collision[];
};

//OUTPUT: triangles per voxel
layout(std430, binding = 6) restrict writeonly buffer scolln {
    int trianglesPerVoxel[];
};




//Returns input data at the vertex position in the given chunk
vec3 getDataAt(uint chunkIndex, ivec3 vertexPosition) {
    return inputData[chunkIndex][vertexPosition.z][vertexPosition.y][vertexPosition.x].xyz;
}

void addTriangleToMesh(uint chunkIndex, Triangle triangle) {
    uint dataIndex = atomicAdd(vertexCount[chunkIndex], 1);
    mesh[chunkIndex * CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * MAX_TRIANGLES_PER_CUBE + dataIndex] = triangle;
}

void addVoxelData(uint chunkIndex, ivec3 cubeCoords, Voxel voxel, int numberOfTriangles) {
    trianglesPerVoxel[chunkIndex * CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE + cubeCoords.x * CHUNK_SIZE * CHUNK_SIZE + cubeCoords.y * CHUNK_SIZE + cubeCoords.z] = numberOfTriangles;

    collision[chunkIndex * CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE + cubeCoords.x * CHUNK_SIZE * CHUNK_SIZE + cubeCoords.y * CHUNK_SIZE + cubeCoords.z] = voxel;
}

//Returns the index of the polygonisation of a given cube (based on density of its corners)
int cubeIndex(float corners[8]) {
    float fixedCorners[8];
    for (int i = 0; i < 8; i++)
        fixedCorners[i] = corners[fixCorners[i]];
    int index = 0;
    int power = 1; //2^i
    for (int i = 0; i < 8; i++) {
        if (fixedCorners[i] > 0)
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


//Find the vector perpendicular to the given triangle
vec3 triangleNormal(vec3 vertex0, vec3 vertex1, vec3 vertex2) {
    vec3 edge1 = vertex1 - vertex0;
    vec3 edge2 = vertex2 - vertex0;
    return normalize(cross(edge1, edge2));
}

void main() {

    const uint currentChunkIndex = gl_WorkGroupID.x;
    const ivec3 currentChunkPos = chunkPos[currentChunkIndex].xyz;

    //The coordinates of the current cube within the current chunk
    const ivec3 cubeCoords = ivec3(gl_LocalInvocationID.xy, gl_WorkGroupID.z);

    float density[8];
    float colour_var[8];
    float material_id[8];

    for (int i = 0; i < 8; i++) {
        ivec3 offset = intToBits(i);
        vec3 rawData = getDataAt(currentChunkIndex, cubeCoords + offset);
        material_id[i] = rawData.x;
        colour_var[i] = rawData.y;
        density[i] = rawData.z;
    }

    const int index = cubeIndex(density);


    //MESH OF A CUBE
    Voxel voxel;
    int numberOfTriangles = 0;
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
            triangle.vertex[j].position = mix(vertex0coords, vertex1coords, alpha);

            //SET TRIANGLE VERTEX COLOUR VARIATION
            triangle.vertex[j].colour_variation = mix(colour_var[vertex0], colour_var[vertex1], alpha);

            //SET MATERIAL

            triangle.vertex[j].material_id = floor((material_id[vertex0] + material_id[vertex1]) / 2);
        }

        //SET TRIANGLE NORMAL --- FLAT SHADING

        vec3 normal = triangleNormal(triangle.vertex[0].position, triangle.vertex[1].position, triangle.vertex[2].position);
        for (int j = 0; j < 3; j++) {
            triangle.vertex[j].normal = normal;
        }

        for (int j = 0; j < 3; j++) {
            voxel.triangles[numberOfTriangles].vertices[j] = vec4(triangle.vertex[j].position - cubeCoords, 0.0);
        }
        numberOfTriangles++;

        //WRITE TRIANGLE TO OUTPUT
        addTriangleToMesh(currentChunkIndex, triangle);
    }
    addVoxelData(currentChunkIndex, cubeCoords, voxel, numberOfTriangles);
    barrier();
}
