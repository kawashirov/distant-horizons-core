#version 150 core


//uniform vec3 modelOffset;
//uniform float worldYOffset;

//in uvec4 vPosition;
in vec2 vPos;
out vec3 vertexWorldPos;
out float vertexYPos;


void main()
{
//    vertexWorldPos = vPosition.xyz + modelOffset;
//    vertexWorldPos = vPosition.xyz;
    vertexWorldPos = vec3(0.);
    vertexYPos = 0.;
//    vertexYPos = vPosition.y + worldYOffset;

    gl_Position = vec4(vPos, 1.0, 1.0);
}