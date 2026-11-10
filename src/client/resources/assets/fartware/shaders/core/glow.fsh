#version 150

// Turns a blurred player silhouette into a glow around the players, in the theme colours.
uniform sampler2D Sampler0; // blurred silhouette (coverage in the red channel)
uniform sampler2D Sampler1; // sharp silhouette, so the glow stays outside the model
uniform vec2 ScreenSize;
uniform float Time;
uniform float Strength;
uniform vec3 Color0;
uniform vec3 Color1;
uniform vec3 Color2;

out vec4 fragColor;

vec3 palette(float x) {
    float t = fract(x) * 3.0;
    float f = smoothstep(0.0, 1.0, fract(t));
    int i = int(floor(t));
    if (i == 0) return mix(Color0, Color1, f);
    if (i == 1) return mix(Color1, Color2, f);
    return mix(Color2, Color0, f);
}

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float glow = texture(Sampler0, uv).r;
    float body = texture(Sampler1, uv).r;

    // The same colour field as the UI, so the glow breathes with the theme.
    float aspect = ScreenSize.x / ScreenSize.y;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0) * 2.0;
    float flow = dot(p, vec2(0.55, 0.35))
               + 0.12 * sin(p.y * 2.5 + Time * 0.45)
               + 0.12 * sin(p.x * 1.8 - Time * 0.35)
               + Time * 0.04;
    vec3 color = palette(flow);
    float breath = 0.5 + 0.5 * sin(Time * 0.9 - length(p) * 2.2);
    color *= mix(0.80, 1.15, breath);

    // Blurring spreads the coverage thin, so boost it; fade it out over the model itself.
    float alpha = clamp(glow * 3.0, 0.0, 1.0) * Strength * (1.0 - body);
    fragColor = vec4(min(color, vec3(1.0)), alpha);
}
