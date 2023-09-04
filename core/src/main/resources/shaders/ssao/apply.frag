#version 150 core

#define ENABLE_SSAO_BLUR

in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D gSSAOMap;
uniform sampler2D gDepthMap;
uniform vec2 gViewSize;
uniform float gNear;
uniform float gFar;


float linearizeDepth(const in float depth) {
    return (gNear * gFar) / (depth * (gNear - gFar) + gFar);
}

float Gaussian(const in float sigma, const in float x) {
    return exp(-(x*x) / (2.0 * (sigma*sigma)));
}

float BilateralGaussianBlur(const in vec2 texcoord, const in float linearDepth, const in float g_sigmaV) {
    float g_sigmaX = 1.6;
    float g_sigmaY = 1.6;

    const float c_halfSamplesX = 2.0;
    const float c_halfSamplesY = 2.0;
    
    vec2 pixelSize = 1.0 / gViewSize;

    float accum = 0.0;
    float total = 0.0;
    for (float iy = -c_halfSamplesY; iy <= c_halfSamplesY; iy++) {
        float fy = Gaussian(g_sigmaY, iy);

        for (float ix = -c_halfSamplesX; ix <= c_halfSamplesX; ix++) {
            float fx = Gaussian(g_sigmaX, ix);

            vec2 sampleTex = texcoord + vec2(ix, iy) * pixelSize;
            float sampleValue = textureLod(gSSAOMap, sampleTex, 0).r;
            float sampleDepth = textureLod(gDepthMap, sampleTex, 0).r;
            float sampleLinearDepth = linearizeDepth(sampleDepth);

            float depthDiff = abs(sampleLinearDepth - linearDepth);
            float fv = Gaussian(g_sigmaV, depthDiff);

            float weight = fx*fy*fv;
            accum += weight * sampleValue;
            total += weight;
        }
    }

    if (total <= 1.e-4) return 1.0;
    return accum / total;
}


void main()
{
    fragColor = vec4(1.0);
    
    float fragmentDepth = textureLod(gDepthMap, TexCoord, 0).r;

    // a fragment depth of "1" means the fragment wasn't drawn to,
    // we only want to apply SSAO to LODs, not to the sky outside the LODs
    if (fragmentDepth < 1) {
        #ifdef ENABLE_SSAO_BLUR
            float fragmentDepthLinear = linearizeDepth(fragmentDepth);
            fragColor.a = BilateralGaussianBlur(TexCoord, fragmentDepthLinear, 1.6);
        #else
            fragColor.a = textureLod(gSSAOMap, TexCoord, 0).r;
        #endif
    }
}
