#version 150

// Draws an entity model as one flat colour. The texture is only used to cut out transparent pixels,
// so the silhouette matches the real model including skin overlays and armour.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    if (texture(Sampler0, texCoord0).a < 0.1) {
        discard;
    }
    fragColor = ColorModulator;
}
