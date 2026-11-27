#version 330

in vec3 Position;
in vec2 UV0;

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <brandonscore:bc_params.glsl>

out vec3 fPos;
out vec2 texCoord0;
out float flowH;

void main() {
    gl_Position = ProjMat * ModelViewMat * ModelMat * vec4(Position, 1.0);
    fPos = (ModelViewMat * ModelMat * vec4(Position, 1.0)).xyz;
    texCoord0 = UV0;
    // I/O crystals: 0 at the face of the block it sits on, 1 at the tip. Position is block-local for BC shader draws.
    vec3 toBlock = vec3(Hue, Yaw, Pitch);
    flowH = (dot(Position - (vec3(0.5) + 0.5 * toBlock), -toBlock) - 0.04) / 0.8;
}
