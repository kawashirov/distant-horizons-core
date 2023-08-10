
in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D gDepthMap;
// model view matrix and projection matrix
uniform mat4 gMvmProj;

uniform float fogScale;
uniform float fogVerticalScale;
uniform float nearFogStart;
uniform float nearFogLength;
uniform int fullFogMode;

uniform vec4 fogColor;


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

// method definitions
// ==== The below 5 methods will be run-time generated. ====
float getNearFogThickness(float dist);
float getFarFogThickness(float dist);
float getHeightFogThickness(float dist);
float calculateFarFogDepth(float horizontal, float dist, float nearFogStart);
float calculateHeightFogDepth(float vertical, float realY);
float mixFogThickness(float near, float far, float height);
// =========================================================


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


vec3 calcViewPosition(vec2 coords) {
    float fragmentDepth = texture(gDepthMap, coords).r;

    vec4 ndc = vec4(
        coords.x * 2.0 - 1.0,
        coords.y * 2.0 - 1.0,
        fragmentDepth * 2.0 - 1.0,
        1.0
    );

    vec4 eyeCoord = inverse(gMvmProj) * ndc;
    vec3 cameraPos = eyeCoord.xyz / eyeCoord.w;
    return cameraPos;
}

/**
 * Fragment shader for fog.
 * This should be passed last so it applies above other affects like AO
 *
 * version: 2023-6-21
 */
void main() 
{
    float vertexYPos = 100.0f;
    vec3 vertexWorldPos = calcViewPosition(TexCoord);
    
    if (fullFogMode == 1) 
    {
        // render everything with the fog color
        fragColor = vec4(fogColor.r, fogColor.g, fogColor.b, 1.0);
    } 
    else if (fullFogMode == 0)
    {
        // render fog based on distance from the camera
        
        float horizontalDist = length(vertexWorldPos.xz) * fogScale;
        float heightDist = calculateHeightFogDepth(vertexWorldPos.y, vertexYPos) * fogVerticalScale;
        float farDist = calculateFarFogDepth(horizontalDist, length(vertexWorldPos.xyz) * fogScale, nearFogStart);

        float nearFogThickness = getNearFogThickness(horizontalDist);
        float farFogThickness = getFarFogThickness(farDist);
        float heightFogThickness = getHeightFogThickness(heightDist);
        float mixedFogThickness = 
            clamp(
                mixFogThickness(nearFogThickness, farFogThickness, heightFogThickness)
                , 0.0, 1.0);

        fragColor = vec4(fogColor.r, fogColor.g, fogColor.b, mixedFogThickness);
    }
    else
    {
        // test code.
        
        // this can be fired by manually changing the fullFogMode to a (normally)
        // invalid value (like 7). By having a separate if statement defined by
        // a uniform we don't have to worry about GLSL optimizing away different
        // options when testing, causing a bunch of headaches if we just want to render the screen red.
        
        float depthValue = texture(gDepthMap, TexCoord).r;
        fragColor = vec4(vec3(depthValue), 1.0); // Convert depth value to grayscale color
    }
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