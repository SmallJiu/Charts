package cat.jiu.charts.utils.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class HighlightBlock extends RenderType {
    protected static HighlightBlock INSTANCE = new HighlightBlock();
    public static final ColorData COLOR_DATA = new ColorData(255, 0, 0);
    protected static final HashMap<BlockPos, HeightLight> HEIGHT_LIGHTS = new HashMap<>();
    protected static final ArrayList<BlockPos> HEIGHT_LIGHTS_KEYS = new ArrayList<>();
    public static void highlight(BlockPos pos, int m, int s, int tick) {
        if (!HEIGHT_LIGHTS.containsKey(pos)) {
            HEIGHT_LIGHTS_KEYS.add(pos);
            HEIGHT_LIGHTS.put(pos, new HeightLight((((m * 60L) + s) * 20) + tick, pos));
        }
    }
    protected static final RenderType CUBE_RENDER = create(
            "color_cube",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false, false,
            CompositeState.builder()
                    .setTransparencyState(
                            new RenderStateShard.TransparencyStateShard(
                                    "sto",
                                    () -> {
                                        RenderSystem.enableBlend();
                                        RenderSystem.blendFunc(
                                                GlStateManager.SourceFactor.SRC_ALPHA,
                                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                                        );
                                    },
                                    () -> {
                                        RenderSystem.disableBlend();
                                        RenderSystem.defaultBlendFunc();
                                    }
                            )
                    )
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setTextureState(NO_TEXTURE)
                    .createCompositeState(true)
    );

    protected VertexBuffer vertex;
    public HighlightBlock() {
        super("", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 0, false, false, () -> {}, () -> {});
    }

    public ColorData getColor() {
        return COLOR_DATA;
    }

    @SubscribeEvent
    public static void onRenderLevelStageEvent(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && !HEIGHT_LIGHTS.isEmpty()) {
            for (int i = 0; i < HEIGHT_LIGHTS_KEYS.size(); i++) {
                HeightLight light = HEIGHT_LIGHTS.get(HEIGHT_LIGHTS_KEYS.get(i));
                long gt = Minecraft.getInstance().level.getGameTime();
                if (!light.init) {
                    light.init = true;
                    light.endTime = gt + light.lightTime;
                }
                if (gt >= light.endTime) {
                    HEIGHT_LIGHTS_KEYS.remove(i);
                    HEIGHT_LIGHTS.remove(light.pos);
                }else {
                    light.canRender = !light.canRender;
                    if (light.canRender) {
                        INSTANCE.render(light, event.getPoseStack(), event.getProjectionMatrix(), event.getCamera());
                    }
                }
            }
        }
    }

    public void render(HeightLight light, PoseStack stack, Matrix4f pro, Camera camera) {
        if (GameRenderer.getPositionColorShader() == null || RenderSystem.getModelViewMatrix() == null) {
            return;
        }
        if (camera.isInitialized()) {
            if (this.vertex == null) {
                this.vertex = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            }
            BufferBuilder buffer = new BufferBuilder(CUBE_RENDER.bufferSize() * 8);
            stack.pushPose();
            Vec3 offset = camera.getPosition().reverse();
            stack.translate(offset.x, offset.y, offset.z);
            buffer.begin(CUBE_RENDER.mode(), CUBE_RENDER.format());

            this.drawCube(0.8f, light, stack, buffer);

            this.vertex.bind();
            this.vertex.upload(buffer.end());
            VertexBuffer.unbind();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            this.vertex.bind();
            this.vertex.drawWithShader(
                    RenderSystem.getModelViewMatrix(),
                    pro,
                    GameRenderer.getPositionColorShader()
            );
            VertexBuffer.unbind();
            stack.popPose();
            RenderSystem.enableCull();
        }
    }

    public void drawCube(float size, HeightLight light, PoseStack stack, BufferBuilder buf) {
        float half = size / 2f;
        Vec3 c = light.pos.getCenter();
        AABB box = new AABB(c.x - half, c.y - half, c.z - half, c.x + half, c.y + half, c.z + half);

        Vec3 topRight = new Vec3(box.maxX, box.maxY, box.maxZ);
        Vec3 bottomRight = new Vec3(box.maxX, box.minY, box.maxZ);
        Vec3 bottomLeft = new Vec3(box.minX, box.minY, box.maxZ);
        Vec3 topLeft = new Vec3(box.minX, box.maxY, box.maxZ);
        Vec3 topRight2 = new Vec3(box.maxX, box.maxY, box.minZ);
        Vec3 bottomRight2 = new Vec3(box.maxX, box.minY, box.minZ);
        Vec3 bottomLeft2 = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 topLeft2 = new Vec3(box.minX, box.maxY, box.minZ);
        drawSide(topRight, topLeft, bottomRight, bottomLeft, light.color, buf, stack);
        drawSide(topRight2, topRight, bottomRight2, bottomRight, light.color, buf, stack);
        drawSide(topLeft2, topRight2, bottomLeft2, bottomRight2, light.color, buf, stack);
        drawSide(topLeft, topLeft2, bottomLeft, bottomLeft2, light.color, buf, stack);
        drawSide(topLeft2, topRight2, topLeft, topRight, light.color, buf, stack);
        drawSide(bottomLeft2, bottomRight2, bottomLeft, bottomRight, light.color, buf, stack);
    }

    protected void drawSide(Vec3 tr, Vec3 tl, Vec3 br, Vec3 bl, ColorData color, VertexConsumer buf, PoseStack pose) {
        Matrix4f mat = pose.last().pose();
        buf.vertex(mat, (float) tr.x, (float) tr.y, (float) tr.z).color(color.getRf(), color.getGf(), color.getBf(), color.getAf()).endVertex();
        buf.vertex(mat, (float) br.x, (float) br.y, (float) br.z).color(color.getRf(), color.getGf(), color.getBf(), color.getAf()).endVertex();
        buf.vertex(mat, (float) bl.x, (float) bl.y, (float) bl.z).color(color.getRf(), color.getGf(), color.getBf(), color.getAf()).endVertex();
        buf.vertex(mat, (float) tl.x, (float) tl.y, (float) tl.z).color(color.getRf(), color.getGf(), color.getBf(), color.getAf()).endVertex();
    }

    public static class HeightLight {
        public final long lightTime;
        public final BlockPos pos;
        protected final ColorData color = INSTANCE.getColor().copy();
        protected boolean
                init,
                canRender;
        protected long endTime;

        public HeightLight(long lightTime, BlockPos pos) {
            this.lightTime = lightTime;
            this.pos = pos;
        }
    }

    public static class ColorData {
        public float a;
        public float r;
        public float g;
        public float b;

        public ColorData(float a, float r, float g, float b) {
            this.a = a;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public ColorData(float r, float g, float b) {
            this(1, r, g, b);
        }

        public ColorData(int a, int r, int g, int b) {
            this(a / 255f, r / 255f, g / 255f, b / 255f);
        }

        public ColorData(int r, int g, int b) {
            this(255, r, g, b);
        }

        public ColorData(int argb) {
            this((argb >>> 24) & 0xFF, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
        }

        public float getAf() {
            return this.a;
        }

        public float getRf() {
            return this.r;
        }

        public float getGf() {
            return this.g;
        }

        public float getBf() {
            return this.b;
        }

        public int getAi() {
            return (int) (this.a * 255);
        }

        public int getRi() {
            return (int) (this.r * 255);
        }

        public int getGi() {
            return (int) (this.g * 255);
        }

        public int getBi() {
            return (int) (this.b * 255);
        }

        public int toARGB() {
            return (getAi() << 24) | (getRi() << 16) | (getGi() << 8) | getBi();
        }

        public int toRGBA() {
            return (getRi() << 24) | (getGi() << 16) | (getBi() << 8) | getAi();
        }

        public int toRGB() {
            return (getRi() << 16) | (getGi() << 8) | getBi();
        }

        @Override
        public String toString() {
            return "[a=%s, r=%s, g=%s, b=%s]".formatted(this.a, this.r, this.g, this.b);
        }

        @Override
        public int hashCode() {
            return toARGB();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ColorData cd) {
                return cd.toARGB() == toARGB();
            }
            return false;
        }

        public ColorData copy() {
            return new ColorData(a, r, g, b);
        }
    }
}
