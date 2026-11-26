package com.brandon3055.draconicevolution.client.handler;

import com.brandon3055.brandonscore.client.ProcessHandlerClient;
import com.brandon3055.brandonscore.lib.DelayedExecutor;
import com.brandon3055.draconicevolution.api.energy.ICrystalBinder;
import com.brandon3055.draconicevolution.client.shader.ExplosionFlashRenderer;
import com.mojang.blaze3d.platform.Window;
import java.nio.FloatBuffer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.joml.Vector4f;

import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;






/**
 * Created by Brandon on 28/10/2014.
 */
@Deprecated //I eventually want to move all this out into proper dedicated handlers
public class ClientEventHandler {
    //    public static ObfMapping splashTextMapping = new ObfMapping("net/minecraft/client/gui/GuiMainMenu", "field_110353_x");
    public static FloatBuffer winPos = FloatBuffer.allocate(3);
    public static volatile int elapsedTicks;
    public static boolean playerHoldingWrench = false;

    private static Random rand = new Random();
    public static BlockPos explosionPos = null;
    public static double explosionAnimation = 0;
    public static int explosionTime = 0;
    public static boolean explosionRetreating = false;

    /** Fusion recipes received from the server (eli.7); read by the JEI plugin. */
    public static java.util.List<net.minecraft.world.item.crafting.RecipeHolder<com.brandon3055.draconicevolution.api.crafting.IFusionRecipe>> fusionRecipes = java.util.List.of();

    public static void init(IEventBus modBus) {
        modBus.addListener(EventPriority.LOW, ClientEventHandler::registerOverlays);
        modBus.addListener(ClientEventHandler::addPackFinders);
        NeoForge.EVENT_BUS.addListener(ClientEventHandler::tickEnd);
        // eli.7: keep the fusion recipes the server sent, for the JEI category. HIGHEST so it runs before JEI reloads.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (net.neoforged.neoforge.client.event.RecipesReceivedEvent e) ->
                fusionRecipes = java.util.List.copyOf(e.getRecipeMap().byType(com.brandon3055.draconicevolution.api.DraconicAPI.FUSION_RECIPE_TYPE.get())));
    }

    public static void addPackFinders(AddPackFindersEvent event) {
        event.addPackFinders(Identifier.fromNamespaceAndPath(MODID, "2d_item_models"), PackType.CLIENT_RESOURCES,
                Component.literal("Draconic Evolution Classic 16×16"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    private static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerBelowAll(Identifier.fromNamespaceAndPath(MODID, "explosion_overlay"), (graphics, deltaTracker) -> {
            if (explosionPos != null) {
                Minecraft mc = Minecraft.getInstance();
                updateExplosionAnimation(mc, graphics, mc.getWindow(), deltaTracker.getGameTimeDeltaPartialTick(false));
            }
        });
    }

    public static void tickEnd(ClientTickEvent.Post event) {
        elapsedTicks++;

        if (explosionPos != null) {
            updateExplosion();
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            playerHoldingWrench = (!player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem() instanceof ICrystalBinder) || (!player.getOffhandItem().isEmpty() && player.getOffhandItem().getItem() instanceof ICrystalBinder);
        }
    }



//    @SubscribeEvent
//    public void renderWorldEvent(RenderLevelLastEvent event) {
//        if (event.isCanceled()) {
//            return;
//        }
//        MODELVIEW.set(event.getPoseStack().last().pose());
//        PROJECTION.set(event.getProjectionMatrix());
//
//        LocalPlayer player = Minecraft.getInstance().player;
//        Level world = player.getCommandSenderWorld();
//        ItemStack stack = player.getMainHandItem();
//        ItemStack offStack = player.getOffhandItem();
//        Minecraft mc = Minecraft.getInstance();
//        float partialTicks = event.getPartialTick();
//
//        try {
//            if (!stack.isEmpty() && stack.getItem() instanceof ICrystalBinder) {
//                BinderHandler.renderWorldOverlay(player, event.getPoseStack(), world, stack, mc, partialTicks);
//                return;
//            } else if (!stack.isEmpty() && offStack.getItem() instanceof ICrystalBinder) {
//                BinderHandler.renderWorldOverlay(player, event.getPoseStack(), world, offStack, mc, partialTicks);
//                return;
//            }
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//
//
//        if (!(mc.hitResult instanceof BlockHitResult)) {
//            return;
//        }
//
////        if (!stack.isEmpty() && stack.getItem() == DEContent.creative_exchanger) {
////
////            List<BlockPos> blocks = CreativeExchanger.getBlocksToReplace(stack, ((BlockRayTraceResult) mc.hitResult).getBlockPos(), world, ((BlockRayTraceResult) mc.hitResult).getDirection());
////
////            Tessellator tessellator = Tessellator.getInstance();
////            BufferBuilder buffer = tessellator.getBuilder();
////
////            double offsetX = player.xo + (player.getX() - player.xo) * (double) partialTicks;
////            double offsetY = player.yo + (player.getY() - player.yo) * (double) partialTicks;
////            double offsetZ = player.zo + (player.getZ() - player.zo) * (double) partialTicks;
////
////            RenderSystem.enableBlend();
////            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
////            RenderSystem.color4f(1F, 1F, 1F, 1F);
////            RenderSystem.lineWidth(2.0F);
////            RenderSystem.disableTexture();
////
////            for (BlockPos block : blocks) {
////                if (world.isEmptyBlock(block)) {
////                    continue;
////                }
////
////                double renderX = block.getX() - offsetX;
////                double renderY = block.getY() - offsetY;
////                double renderZ = block.getZ() - offsetZ;
////
////                Cuboid6 box = new Cuboid6(renderX, renderY, renderZ, renderX + 1, renderY + 1, renderZ + 1).expand(0.001, 0.001, 0.001);
////                float colour = 1F;
////                if (!world.getBlockState(block.relative(((BlockRayTraceResult) mc.hitResult).getDirection())).getMaterial().isReplaceable()) {
////                    RenderSystem.disableDepthTest();
////                    colour = 0.2F;
////                }
////                GL11.glColor4f(colour, colour, colour, colour);
////
//////                RenderUtils.drawCuboidOutline(box);
////
////                if (!world.getBlockState(block.relative(((BlockRayTraceResult) mc.hitResult).getDirection())).getMaterial().isReplaceable()) {
////                    RenderSystem.enableDepthTest();
////                }
////            }
////
////            RenderSystem.enableTexture();
////            RenderSystem.disableBlend();
////        }
//
////        if (stack.isEmpty() || !(stack.getItem() instanceof MiningToolBase) || !ToolConfigHelper.getBooleanField("showDigAOE", stack)) {
////            return;
////        }
////
////        BlockPos pos = ((BlockRayTraceResult) mc.hitResult).getBlockPos();
////        BlockState state = world.getBlockState(pos);
////        MiningToolBase tool = (MiningToolBase) stack.getItem();
////
////        if (!tool.isToolEffective(stack, state)) {
////            return;
////        }
//
////        renderMiningAOE(world, stack, pos, player, partialTicks);
//    }

//    private void renderMiningAOE(World world, ItemStack stack, BlockPos pos, ClientPlayerEntity player, float partialTicks) {
//        MiningToolBase tool = (MiningToolBase) stack.getItem();
//        Pair<BlockPos, BlockPos> aoe = tool.getMiningArea(pos, player, tool.getDigAOE(stack), tool.getDigDepth(stack));
//        List<BlockPos> blocks = Lists.newArrayList(BlockPos.betweenClosed(aoe.key(), aoe.value()));
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder buffer = tessellator.getBuilder();
//
//        double offsetX = player.xo + (player.getX() - player.xo) * (double) partialTicks;
//        double offsetY = player.yo + (player.getY() - player.yo) * (double) partialTicks;
//        double offsetZ = player.zo + (player.getZ() - player.zo) * (double) partialTicks;
//
//        RenderSystem.enableBlend();
//        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
//        RenderSystem.color4f(1F, 1F, 1F, 1F);
//        RenderSystem.lineWidth(2.0F);
//        RenderSystem.disableTexture();
//        RenderSystem.disableDepthTest();
//
//
//        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
//
//        for (BlockPos block : blocks) {
//            BlockState state = world.getBlockState(block);
//
//            if (!tool.isToolEffective(stack, state)) {
//                continue;
//            }
//
//            double renderX = block.getX() - offsetX;
//            double renderY = block.getY() - offsetY;
//            double renderZ = block.getZ() - offsetZ;
//
//            AxisAlignedBB box = new AxisAlignedBB(renderX, renderY, renderZ, renderX + 1, renderY + 1, renderZ + 1).deflate(0.49D);
//
//            double rDist = Utils.getDistanceSq(pos.getX(), pos.getY(), pos.getZ(), block.getX(), block.getY(), block.getZ());
//
//
//            float colour = 1F - (float) rDist / 100F;
//            if (colour < 0.1F) {
//                colour = 0.1F;
//            }
//            float alpha = colour;
//            if (alpha < 0.15) {
//                alpha = 0.15F;
//            }
//
//            float r = 0F;
//            float g = 1F;
//            float b = 1F;
//
//
//            buffer.vertex(box.minX, box.minY, box.minZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//            buffer.vertex(box.maxX, box.maxY, box.maxZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//
//            buffer.vertex(box.maxX, box.minY, box.minZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//            buffer.vertex(box.minX, box.maxY, box.maxZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//
//            buffer.vertex(box.minX, box.minY, box.maxZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//            buffer.vertex(box.maxX, box.maxY, box.minZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//
//            buffer.vertex(box.maxX, box.minY, box.maxZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//            buffer.vertex(box.minX, box.maxY, box.minZ).color(r * colour, g * colour, b * colour, alpha).endVertex();
//
//        }
//
//        tessellator.end();
//
//        RenderSystem.enableDepthTest();
//        RenderSystem.enableTexture();
//        RenderSystem.disableBlend();
//    }

    public static void triggerExplosionEffect(BlockPos pos, boolean reload) {
        explosionPos = pos;
        explosionRetreating = false;
        explosionAnimation = 0;
        explosionTime = 0;

        if (reload) {
            ProcessHandlerClient.addProcess(new DelayedExecutor(13) {
                @Override
                public void execute(Object[] args) {
                    Minecraft.getInstance().levelRenderer.allChanged();
                }
            });
        }
    }

    private static void updateExplosion() {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        explosionTime++;
        if (!explosionRetreating) {
            explosionAnimation += 0.05;
            if (explosionAnimation >= 1) {
                explosionAnimation = 1;
                explosionRetreating = true;
            }
        } else {
            if (explosionAnimation <= 0) {
                explosionAnimation = 0;
                explosionPos = null;
                return;
            }
            explosionAnimation -= 0.01;
        }
//        explosionTime = 10;
//        explosionAnimation = explosionTime * 0.05;
    }

    private static void updateExplosionAnimation(Minecraft mc, GuiGraphicsExtractor graphics, Window window, float partialTick) {
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        if (explosionRetreating) {
            float alpha = Mth.clamp((float) explosionAnimation - partialTick * 0.01F, 0F, 1F);
            graphics.fill(0, 0, width, height, ((int) (alpha * 255F) << 24) | 0xFFFFFF);
            return;
        }

        var camera = mc.gameRenderer.getMainCamera();
        Vec3 relative = Vec3.atCenterOf(explosionPos).subtract(camera.position());
        var point = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f())
                .transform(new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1F));
        float screenX = point.w > 0 ? point.x / point.w * 0.5F + 0.5F : -1F;
        float screenY = point.w > 0 ? point.y / point.w * 0.5F + 0.5F : -1F;
        graphics.submitPictureInPictureRenderState(new ExplosionFlashRenderer.State(0, 0, width, height,
                screenX, screenY, (float) explosionAnimation, new org.joml.Matrix3x2f(graphics.pose()),
                graphics.peekScissorStack()));
    }
}
