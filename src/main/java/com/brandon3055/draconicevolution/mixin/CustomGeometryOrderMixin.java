package com.brandon3055.draconicevolution.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Stops translucent custom geometry (energy core shell, stabilizer and pylon beams, crystal effects, reactor) from
 * flickering. Vanilla groups custom geometry submits in a HashMap keyed by RenderType and draws the groups in map
 * order. Brandon's Core gives every shader draw its own RenderType each frame (ShaderDraw.capture), so that order
 * followed identity hashes and changed every frame: translucent layers swapped between in front of and behind each
 * other. A LinkedHashMap draws them in submission order, which is the same from one frame to the next.
 */
@Mixin(targets = "net.minecraft.client.renderer.feature.CustomFeatureRenderer$Storage")
public abstract class CustomGeometryOrderMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/HashMap;"))
    private HashMap<?, ?> draconicevolution$keepSubmitOrder() {
        return new LinkedHashMap<>();
    }
}
