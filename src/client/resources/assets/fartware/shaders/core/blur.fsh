#version 150

// One direction of a separable Gaussian blur. Needs a texture with linear filtering:
// the five taps stand in for nine by sampling between texels.
uniform sampler2D Sampler0;
uniform vec2 InSize;      // size of the source texture in pixels
uniform vec2 Direction;   // (1, 0) for horizontal, (0, 1) for vertical, (0, 0) for a plain copy
uniform float Radius;     // blur spread in source pixels
uniform float Brightness; // 1.0 = unchanged, below 1.0 darkens

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 texelStep = Direction * Radius / InSize;

    vec3 sum = texture(Sampler0, texCoord).rgb * 0.2270270270;
    sum += (texture(Sampler0, texCoord + texelStep * 1.3846153846).rgb
          + texture(Sampler0, texCoord - texelStep * 1.3846153846).rgb) * 0.3162162162;
    sum += (texture(Sampler0, texCoord + texelStep * 3.2307692308).rgb
          + texture(Sampler0, texCoord - texelStep * 3.2307692308).rgb) * 0.0702702703;

    fragColor = vec4(sum * Brightness, 1.0);
}
