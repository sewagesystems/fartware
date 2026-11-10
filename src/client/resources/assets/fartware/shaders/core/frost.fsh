#version 150

// Draws a rounded rectangle showing a screen-sized, already blurred copy of the frame,
// darkened by Brightness: a frosted glass panel with no colour of its own.
uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec4 Rect;        // x0, y0, x1, y1 of the element in framebuffer pixels (origin bottom left)
uniform vec4 Radii;       // corner radii in pixels: top left, top right, bottom right, bottom left
uniform float Brightness; // 1.0 = unchanged, below 1.0 darkens

out vec4 fragColor;

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
    vec3 color = texture(Sampler0, gl_FragCoord.xy / ScreenSize).rgb * Brightness;
    fragColor = vec4(color, roundedMask());
}
