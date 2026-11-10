#version 150

uniform vec2 ScreenSize;  // framebuffer size in pixels (filled in by the game)
uniform float Time;       // animation clock in seconds
uniform float Alpha;      // opacity of the element being drawn
uniform vec3 Color0;
uniform vec3 Color1;
uniform vec3 Color2;
uniform vec4 Rect;        // x0, y0, x1, y1 of the element in framebuffer pixels (origin bottom left)
uniform vec4 Radii;       // corner radii in pixels: top left, top right, bottom right, bottom left
uniform float Flat;       // 1.0 = draw FlatColor instead of the breathing colours
uniform vec4 FlatColor;

out vec4 fragColor;

// Smoothly loops Color0 -> Color1 -> Color2 -> Color0 as x goes from 0 to 1.
vec3 palette(float x) {
    float t = fract(x) * 3.0;
    float f = smoothstep(0.0, 1.0, fract(t));
    int i = int(floor(t));
    if (i == 0) return mix(Color0, Color1, f);
    if (i == 1) return mix(Color1, Color2, f);
    return mix(Color2, Color0, f);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// Coverage of this pixel inside the rounded rectangle, antialiased over one pixel.
float roundedMask() {
    if (Radii.x + Radii.y + Radii.z + Radii.w <= 0.0) {
        return 1.0;
    }
    vec2 center = (Rect.xy + Rect.zw) * 0.5;
    vec2 halfSize = (Rect.zw - Rect.xy) * 0.5;
    vec2 p = gl_FragCoord.xy - center;
    float r = p.x < 0.0 ? (p.y > 0.0 ? Radii.x : Radii.w) : (p.y > 0.0 ? Radii.y : Radii.z);
    vec2 q = abs(p) - halfSize + r;
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    return clamp(0.5 - d, 0.0, 1.0);
}

void main() {
    float mask = roundedMask();
    if (Flat > 0.5) {
        fragColor = vec4(FlatColor.rgb, FlatColor.a * mask);
        return;
    }

    // The pattern is computed from the position on screen, so every element
    // shows the matching slice of one continuous colour field.
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float aspect = ScreenSize.x / ScreenSize.y;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0) * 2.0;
    float r = length(p);

    float flow = dot(p, vec2(0.55, 0.35))
               + 0.12 * sin(p.y * 2.5 + Time * 0.45)
               + 0.12 * sin(p.x * 1.8 - Time * 0.35)
               + Time * 0.04;
    vec3 color = palette(flow);

    // The breath: brightness swells and fades in a slow wave moving out from the centre.
    float breath = 0.5 + 0.5 * sin(Time * 0.9 - r * 2.2);
    color *= mix(0.70, 1.15, breath);

    float alpha = Alpha * mask + (hash(gl_FragCoord.xy + Time) - 0.5) / 255.0;
    fragColor = vec4(min(color, vec3(1.0)), clamp(alpha, 0.0, 1.0));
}
