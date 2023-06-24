#version 150 core

in vec2 vPos;
out vec2 TexCoord;

out vec4 vertexColor;
out vec3 vertexWorldPos;
out float vertexYPos;


void main()
{
    gl_Position = vec4(vPos, 1.0, 1.0);
    TexCoord = vPos.xy * 0.5 + 0.5;
}