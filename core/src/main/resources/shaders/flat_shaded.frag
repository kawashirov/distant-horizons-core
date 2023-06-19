
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

uniform bool noiseEnabled;
uniform int noiseSteps;
uniform float noiseIntensity;
uniform float noiseDropoff;

/* ========MARCO DEFINED BY RUNTIME CODE GEN=========

float farFogStart;
float farFogLength;
float farFogMin;
float farFogRange;
float farFogDensity;

float heightFogStart;
float heightFogLength;
float heightFogMin;
float heightFogRange;
float heightFogDensity;
*/

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

float fade(float value, float start, float end) {
    return (clamp(value,start,end)-start)/(end-start);
}
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


// Some HSV functions I stole somewhere online
vec3 RGB2HSV(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}
vec3 HSV2RGB(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}


/** 
 * Fragment Shader
 * 
 * author: James Seibel
 * author: coolGi
 * version: 7-2-2023
 */
void main()
{
	vec4 returnColor;


    if (fullFogMode != 0) {
        returnColor = vec4(fogColor.rgb, 1.0);
    } else {
        // TODO: add a white texture to support Optifine shaders
        //vec4 textureColor = texture(texImage, textureCoord);
        //fragColor = vertexColor * textureColor;

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

        returnColor = mix(vertexColor, vec4(fogColor.rgb, 1.0), mixedFogThickness);
	}

    if (noiseEnabled) {
        // This bit of code is required to fix the vertex position problem cus of floats in the verted world position varuable
        vec3 vertexNormal = normalize(cross(dFdx(vPos.xyz), dFdy(vPos.xyz)));
        vec3 fixedVPos = vec3(
            vPos.x - vertexNormal.x * 0.001,
            vPos.y - vertexNormal.y * 0.001,
            vPos.z - vertexNormal.z * 0.001
        );


        float noiseAmplification = noiseIntensity / 100;
        noiseAmplification = (-1 * pow(2*((returnColor.x + returnColor.y + returnColor.z) / 3) - 1, 2) + 1) * noiseAmplification; // Lessen the effect on depending on how dark the object is, equasion for this is -(2x-1)^{2}+1
        noiseAmplification *= returnColor.w; // The effect would lessen on transparent objects

        // Random value for each position
        float randomValue = rand(vec3(
            quantize(fixedVPos.x, noiseSteps),
            quantize(fixedVPos.y, noiseSteps),
            quantize(fixedVPos.z, noiseSteps)
        ))
            * 2. * noiseAmplification - noiseAmplification;


        // Modifies the color
        // A value of 0 on the randomValue will result in the original color, while a value of 1 will result in a fully bright color
        vec3 newCol = returnColor.rgb + (vec3(1.0) - returnColor.rgb) * randomValue;

        // Clamps it and turns it back into a vec4
        returnColor = mix(
            vec4(
                clamp(newCol.r, 0., 1.),
                clamp(newCol.g, 0., 1.),
                clamp(newCol.b, 0., 1.),
                returnColor.w
            ), returnColor,
            clamp(length(vertexWorldPos) * fogScale * noiseDropoff, 0., 1.) // The further away it gets, the less noise gets applied
        );

        // For testing
//        if (returnColor.r != 69420.) {
//            returnColor = vec4(
//                mod(fixedVPos.x, 1),
//                mod(fixedVPos.y, 1),
//                mod(fixedVPos.z, 1),
//            returnColor.w);
//        }
    }

    // If "w" is just set to just 1. then it would crash
	fragColor = returnColor;
}



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