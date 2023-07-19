#version 150 core

in uvec4 vPosition; // Fixme
in vec2 vPos;
out vec2 TexCoord;

out vec4 vertexColor;
out vec3 vertexWorldPos;
out float vertexYPos;


void main()
{
    vertexWorldPos = vec3(vPosition.x, vPosition.y, vPosition.z); // Fixme
    gl_Position = vec4(vPos, 1.0, 1.0);
    TexCoord = vPos.xy * 0.5 + 0.5;
}