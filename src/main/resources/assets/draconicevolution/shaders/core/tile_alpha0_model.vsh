#version 330

// Copy of brandonscore:position_color_tex_alpha0.vsh plus ModelMat.
// ExtractedModel submits shader-parameter render types with raw (block-local) vertices and passes the
// block pose as ModelMat (ShaderDraw.withPose). BC's shader ignores ModelMat, so the energy core's
// stabilizer orbs, outer shell and the pylon shell were drawn relative to the camera instead of the block.

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <brandonscore:bc_params.glsl>

#moj_import <minecraft:projection.glsl>


in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * ModelMat * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
}
