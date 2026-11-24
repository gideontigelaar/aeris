#version 330 core

in vec2 vUV;
out vec4 fragColor;

uniform float iTime;
uniform float iRippleTime;
uniform vec2 iResolution;
uniform int palette;
uniform int previousPalette;
uniform float startTime;

const float PI = 3.14159265;

const vec3 PALETTES[9] = vec3[9](
    vec3(0.086, 0.090, 0.102), vec3(0, 0.157, 0.349), vec3(0.408, 0.682, 0.831),
    vec3(0.137, 0.286, 0.459), vec3(0.408, 0.682, 0.831), vec3(0.980, 0.992, 1),
    vec3(0, 0.157, 0.349), vec3(0.137, 0.286, 0.459), vec3(0.408, 0.682, 0.831)
);

void main()
{
    vec2 fragCoord = vec2(vUV.x * iResolution.x, vUV.y * iResolution.y);
    float blockPx = 4.0;
    float cellSize = 20.0;
    float discR = 8.0;
    float rimWidth = 1.0;
    float rippleSpeed = 0.5;
    float ringGap = 0.15;

    float waveSpeed = 1400.0;
    float flipDuration = 0.3;

    vec2 center = iResolution * 0.5;
    float t = iTime - startTime;
    float duration = length(iResolution) * 0.5 / waveSpeed + flipDuration;
    bool switching = palette != previousPalette && t < duration;

    vec2 cell = floor(fragCoord / blockPx);

    vec2 snapped = (floor(fragCoord / (blockPx * 2.0)) + 0.5) * (blockPx * 2.0);

    vec2 group = floor(cell / cellSize);
    vec2 local = mod(cell, cellSize);
    vec2 p = local + 0.5 - cellSize * 0.5;

    vec2 discCenter = (group + 0.5) * cellSize * blockPx;
    float discDist = distance(discCenter, center);

    float phase = iRippleTime * rippleSpeed - discDist / (cellSize * blockPx) * ringGap;
    float n = floor(phase);
    float f = fract(phase);

    float flipT = floor(clamp(f * 2.5, 0.0, 1.0) * 6.0) / 6.0;
    float c = cos(flipT * PI);

    float switchT = floor(clamp((t - discDist / waveSpeed) / flipDuration, 0.0, 1.0) * 8.0) / 8.0;
    float sx = max(abs(c * (switching ? cos(switchT * PI) : 1.0)), 0.15);

    bool lightSide = mod(n + (c < 0.0 ? 1.0 : 0.0) + group.x + group.y, 2.0) > 0.5;

    float d = length(vec2(p.x / sx, p.y));

    int shade = 1;
    if (d < discR)
    {
        bool rim = d >= discR - rimWidth;
        if (lightSide) shade = rim ? 0 : 2;
        else           shade = rim ? 2 : 0;
    }

    int discPalette = switchT >= 0.5 ? palette : previousPalette;
    int bgPalette = distance(snapped, center) < (t - flipDuration * 0.5) * waveSpeed ? palette : previousPalette;
    int currentPalette = shade == 1 ? bgPalette : discPalette;

    fragColor = vec4(PALETTES[currentPalette * 3 + shade], 1.0);
}