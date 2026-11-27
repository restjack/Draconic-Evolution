#version 330

#moj_import <brandonscore:math.glsl>

#moj_import <brandonscore:bc_params.glsl>
#define Colour BaseColor.rgb
#define Mipmap Decay
//uniform vec2 Angle;

in vec2 texCoord0;
in vec3 fPos;
in float flowH;

out vec4 fragColor;

void main() {
    float intensity = 1;
    vec3 coord = vec3(texCoord0, 0);
    float timeOffset = Time / 15.0;

    coord.x += timeOffset + (fPos.x / 10);// - (angle.x * 2);
    coord.y += (fPos.y / 10);

    float density = 15;//float(int(15.0 * mipmap));
    float brightness = 1;
    for(int i = 1; i <= 7; i++)
    {
        float power = pow(2, float(i));
        brightness += (1.5 / power) * max(snoise(coord + vec3(0, 0, Time * 0.01), power * density), -1);
    }

    brightness = ((1 - Mipmap) * brightness) + (Mipmap * 1.2) ;//max(brightness, mipmap);

    vec4 colour = vec4(brightness * Colour.r, pow(brightness, 2.0) * Colour.g, pow(brightness ,3.0) * Colour.b, 1.0);

    // I/O crystal mode (Transition = +1 Input, -1 Output, 0 none): bright bands travel away from the block on Input
    // and into it on Output, tinted aqua / gold like the wrench message; brighter toward the tip (Input) or the base
    // (Output) so a still frame still shows the way. Fades out with distance like the noise.
    float dir = Transition;
    if (dir != 0.0) {
        float amt = 1.0 - Mipmap;
        float h = clamp(flowH, 0.0, 1.0);
        float band = pow(0.5 + 0.5 * sin(6.2832 * (h * 1.6 - dir * Time * 0.55)), 6.0);
        float grad = dir > 0.0 ? h : 1.0 - h;
        vec3 tint = dir > 0.0 ? vec3(0.15, 0.8, 0.85) : vec3(1.0, 0.72, 0.2);
        colour.rgb *= mix(1.0, 0.55 + 0.9 * grad, amt);
        colour.rgb += amt * band * tint * (0.6 + 0.9 * grad);
    }
    fragColor = colour;
}