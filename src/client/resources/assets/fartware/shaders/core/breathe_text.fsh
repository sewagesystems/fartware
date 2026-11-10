#version 150

// Same colour field as breathe.fsh, used for glyphs: the font atlas supplies the shape.
uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform float Time;
uniform vec3 Color0;
uniform vec3 Color1;
uniform vec3 Color2;

in vec2 texCoord0;
in vec4 vertexColor;

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
    float alpha = texture(Sampler0, texCoord0).a * vertexColor.a;
    if (alpha < 0.003) {
        discard;
    }

    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float aspect = ScreenSize.x / ScreenSize.y;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0) * 2.0;
    float r = length(p);

    float flow = dot(p, vec2(0.55, 0.35))
               + 0.12 * sin(p.y * 2.5 + Time * 0.45)
               + 0.12 * sin(p.x * 1.8 - Time * 0.35)
               + Time * 0.04;
    vec3 color = palette(flow);

    float breath = 0.5 + 0.5 * sin(Time * 0.9 - r * 2.2);
    color *= mix(0.70, 1.15, breath);

    fragColor = vec4(min(color, vec3(1.0)), alpha);
}
