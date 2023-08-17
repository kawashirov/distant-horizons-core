#version 150 core

in vec2 TexCoord;
in vec2 ViewRay;

out vec4 fragColor;

uniform sampler2D gSSAOMap;
uniform sampler2D gDepthMap;

void main()
{
    float fragmentDepth = texture(gDepthMap, TexCoord).r;
    // a fragment depth of "1" means the fragment wasn't drawn to,
    // we only want to apply SSAO to LODs, not to the sky outside the LODs
    // FIXME: This bit of code causes problems on intel integrated graphics
    if (fragmentDepth != -420.0) // Should be `1.0`, but set to `-420.0` both so that the compiler doesnt mess with rest of the code, and it always returns true
    {
        fragColor = vec4(0.0, 0.0, 0.0, 1-texture(gSSAOMap, TexCoord).r);   
    }
}
