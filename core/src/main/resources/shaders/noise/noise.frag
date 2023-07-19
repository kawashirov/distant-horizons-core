#version 150 core

in vec4 vertexColor;
in vec4 vPos;
in vec3 vertexWorldPos;
out vec4 fragColor;

uniform float distanceScale;

uniform int noiseSteps;
uniform float noiseIntensity;
uniform float noiseDropoff;



// The random functions for diffrent dimentions
float rand(float co) { return fract(sin(co*(91.3458)) * 47453.5453); }
float rand(vec2 co){ return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453); }
float rand(vec3 co){ return rand(co.xy+rand(co.z)); }

// Puts steps in a float
// EG. setting stepSize to 4 then this would be the result of this function
// In:  0.0, 0.1, 0.2, 0.3,  0.4,  0.5, 0.6, ..., 1.1, 1.2, 1.3
// Out: 0.0, 0.0, 0.0, 0.25, 0.25, 0.5, 0.5, ..., 1.0, 1.0, 1.25
float quantize(float val, int stepSize) {
    return floor(val*stepSize)/stepSize;
}

// The modulus function dosnt exist in GLSL so I made my own
// To speed up the mod function, this only accepts full numbers for y
float mod(float x, int y) {
    return x - y * floor(x/y);
}




/**
 * Fragment shader for adding noise to lods.
 * This should be passed close to first as it affects the base color of the lod
 *
 * version: 2023-6-21
 */
void main() {
    // This bit of code is required to fix the vertex position problem cus of floats in the verted world position varuable
    vec3 vertexNormal = normalize(cross(dFdx(vPos.xyz), dFdy(vPos.xyz)));
    vec3 fixedVPos = vec3(
        vPos.x - vertexNormal.x * 0.001,
        vPos.y - vertexNormal.y * 0.001,
        vPos.z - vertexNormal.z * 0.001
    );


    float noiseAmplification = noiseIntensity / 100;
    noiseAmplification = (-1 * pow(2*((vertexColor.x + vertexColor.y + vertexColor.z) / 3) - 1, 2) + 1) * noiseAmplification; // Lessen the effect on depending on how dark the object is, equasion for this is -(2x-1)^{2}+1
    noiseAmplification *= vertexColor.w; // The effect would lessen on transparent objects

    // Random value for each position
    float randomValue = rand(vec3(
        quantize(fixedVPos.x, noiseSteps),
        quantize(fixedVPos.y, noiseSteps),
        quantize(fixedVPos.z, noiseSteps)
    ))
        * 2. * noiseAmplification - noiseAmplification;


    // Modifies the color
    // A value of 0 on the randomValue will result in the original color, while a value of 1 will result in a fully bright color
    vec3 newCol = (vec3(1.0) - vertexColor.rgb) * randomValue;

    // Clamps it and turns it back into a vec4
    fragColor = vec4(
        clamp(newCol.r, 0., 1.),
        clamp(newCol.g, 0., 1.),
        clamp(newCol.b, 0., 1.),
        clamp(length(vertexWorldPos) * distanceScale * noiseDropoff, 0., 1.) // The further away it gets, the less noise gets applied
    );
    fragColor = vec4(
        0f, 0f, 0f,
        randomValue // The further away it gets, the less noise gets applied
    );

    // For testing
//    if (vertexColor.r != 69420.) {
//        fragColor = vec4(
//            mod(fixedVPos.x, 1),
//            mod(fixedVPos.y, 1),
//            mod(fixedVPos.z, 1),
//        1f);
//    }
}