#version 150 core


in vec4 vertexColor;
in vec3 vertexWorldPos;
in float vertexYPos;
in vec4 vPos;

out vec4 fragColor;


uniform float fogScale;
uniform float fogVerticalScale;
uniform float nearFogStart;
uniform float nearFogLength;
uniform int fullFogMode;

uniform vec4 fogColor;



// method definitions
// ==== The below 5 methods will be run-time generated. ====
float getNearFogThickness(float dist);
float getFarFogThickness(float dist);
float getHeightFogThickness(float dist);
float calculateFarFogDepth(float horizontal, float dist, float nearFogStart);
float calculateHeightFogDepth(float vertical, float realY);
float mixFogThickness(float near, float far, float height);
// =========================================================




/**
 * Fragment shader for fog.
 * This should be passed last so it applies above other affects like AO
 *
 * version: 2023-6-21
 */
void main() {
    float horizontalDist = length(vertexWorldPos.xz) * fogScale;
    float heightDist = calculateHeightFogDepth(
        vertexWorldPos.y, vertexYPos) * fogVerticalScale;
    float farDist = calculateFarFogDepth(horizontalDist,
        length(vertexWorldPos.xyz) * fogScale, nearFogStart);

    float nearFogThickness = getNearFogThickness(horizontalDist);
    float farFogThickness = getFarFogThickness(farDist);
    float heightFogThickness = getHeightFogThickness(heightDist);
    float mixedFogThickness = clamp(mixFogThickness(
        nearFogThickness, farFogThickness, heightFogThickness), 0.0, 1.0);

    fragColor = mix(vertexColor, vec4(fogColor.rgb, 1.0), mixedFogThickness);
}



// Are these still needed?
float linearFog(float x, float fogStart, float fogLength, float fogMin, float fogRange) {
    x = clamp((x-fogStart)/fogLength, 0.0, 1.0);
    return fogMin + fogRange * x;
}

float exponentialFog(float x, float fogStart, float fogLength,
float fogMin, float fogRange, float fogDensity) {
    x = max((x-fogStart)/fogLength, 0.0) * fogDensity;
    return fogMin + fogRange - fogRange/exp(x);
}

float exponentialSquaredFog(float x, float fogStart, float fogLength,
float fogMin, float fogRange, float fogDensity) {
    x = max((x-fogStart)/fogLength, 0.0) * fogDensity;
    return fogMin + fogRange - fogRange/exp(x*x);
}