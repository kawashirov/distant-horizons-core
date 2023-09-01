#version 150 core
#extension GL_ARB_derivative_control : enable

in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D gDepthMap;
uniform float gSampleRad;
uniform float gFactor;
uniform float gPower;
uniform mat4 gProj;
uniform mat4 gInvProj;

const float MIN_LIGHT = 0.6;
const float SAMPLE_BIAS = 0.6;
const int MAX_KERNEL_SIZE = 32;
const float INV_MAX_KERNEL_SIZE_F = 1.0 / float(MAX_KERNEL_SIZE);
uniform vec3 gKernel[MAX_KERNEL_SIZE];

const vec3 MAGIC = vec3(0.06711056, 0.00583715, 52.9829189);
const float PI = 3.1415926538;


float InterleavedGradientNoise(const in vec2 pixel) {
    float x = dot(pixel, MAGIC.xy);
    return fract(MAGIC.z * fract(x));
}

vec3 calcViewPosition(const in vec3 clipPos) {
    vec4 viewPos = gInvProj * vec4(clipPos * 2.0 - 1.0, 1.0);
    return viewPos.xyz / viewPos.w;
}

void main() {
    float fragmentDepth = textureLod(gDepthMap, TexCoord, 0).r;
    float occlusion = 1.0;
    
    if (fragmentDepth < 1.0) {
        float dither = InterleavedGradientNoise(gl_FragCoord.xy);
        vec3 viewPos = calcViewPosition(vec3(TexCoord, fragmentDepth));
        
        #ifdef GL_ARB_derivative_control
            vec3 viewNormal = cross(dFdxFine(viewPos.xyz), dFdyFine(viewPos.xyz));
        #else
            vec3 viewNormal = cross(dFdx(viewPos.xyz), dFdy(viewPos.xyz));
        #endif

        viewNormal = normalize(viewNormal);
        //const vec3 upVec = vec3(0.0, -1.0, 0.0);

        float angle = dither * (PI * 2.0);
        vec3 rotation = vec3(sin(angle), cos(angle), 0.0);
        vec3 tangent = normalize(cross(viewNormal, rotation));
        
        vec3 bitangent = normalize(cross(viewNormal, tangent));
        mat3 TBN = mat3(tangent, bitangent, viewNormal);

        float maxWeight = 0.0;
        float occlusion_factor = 0.0;
        for (int i = 0; i < MAX_KERNEL_SIZE; i++) {
            vec3 samplePos = TBN * gKernel[i];
            //samplePos *= sign(dot(samplePos, viewNormal));
            samplePos = viewPos + samplePos * gSampleRad;

            vec4 sampleNdcPos = gProj * vec4(samplePos + viewPos, 1.0);
            sampleNdcPos = sampleNdcPos / sampleNdcPos.w;
            if (any(greaterThanEqual(abs(sampleNdcPos.xy), vec2(1.0)))) continue;
            
            maxWeight += 1.0;
            
            vec2 sampleTexPos = sampleNdcPos.xy * 0.5 + 0.5;
            float sampleDepth = textureLod(gDepthMap, sampleTexPos, 0).r;
            float geometryDepth = calcViewPosition(vec3(sampleTexPos, sampleDepth)).z;

            float rangeCheck = smoothstep(0.0, 1.0, gSampleRad / abs(viewPos.z - geometryDepth));
            // the number added to the samplePos.z can be used to reduce noise in the SSAO application at the cost of reducing the overall affect
            occlusion_factor += step(samplePos.z + SAMPLE_BIAS, geometryDepth) * rangeCheck;
        }

        float visibility_factor = 1.0 - (occlusion_factor / maxWeight);
        occlusion = (1.0 - pow(visibility_factor, gFactor)) * gPower;
        occlusion = clamp(1.0 - occlusion, MIN_LIGHT, 1.0);
    }
    
    fragColor = vec4(vec3(1.0), occlusion);
}
