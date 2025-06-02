package cat.jiu.charts.api;

import cat.jiu.charts.ChartsModMain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface IChartsDataHandler {
    public static final Direction[] VALUES = Direction.values();

    boolean hasData(Level world, BlockPos pos);
    Collection<Data> getData(Level world, BlockPos pos);
    default void writeData(CompoundTag nbt, Collection<Data> data){

    }
    default void readData(CompoundTag nbt, Collection<Data> data){

    }

    @OnlyIn(Dist.CLIENT)
    Component getDataName(Data data, Component failBack);
    @OnlyIn(Dist.CLIENT)
    default void renderObject(GuiGraphics graphics, Object object, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver) {
        Icon icon = this.getIcon();
        graphics.blit(icon.img, x, y, width, height, 0, 0, icon.width, icon.height, icon.width, icon.height);
    }

    Icon getIcon();
    int getID(Object object);

    class Data {
        public IChartsDataHandler handler;
        public final Object object;
        public final long data;
        public Data(Object object, long data) {
            this.object = object;
            this.data = data;
        }
        public Component getName() {
            if (this.handler != null) {
                return this.handler.getDataName(this, null);
            }
            return Component.literal(String.valueOf(this.object));
        }
        public int getID() {
            if (this.handler != null) {
                return this.handler.getID(this);
            }
            return Objects.hashCode(this.object);
        }
    }

    class Icon {
        public final ResourceLocation img;
        public final int width, height;
        public Icon(ResourceLocation img, int width, int height) {
            this.img = img;
            this.width = width;
            this.height = height;
        }
    }

    class AllDataHandler implements IChartsDataHandler {
        public static final AllDataHandler INSTANCE = new AllDataHandler();

        @Override
        public boolean hasData(Level world, BlockPos pos) {
            for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                if (handler.hasData(world, pos)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Collection<Data> getData(Level world, BlockPos pos) {
            List<Data> data = new ArrayList<>();
            for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                data.addAll(handler.getData(world, pos));
            }
            data.forEach(d->d.handler=this);
            return data;
        }

        @Override
        public Component getDataName(Data data, Component failBack) {
            for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                Component name = handler.getDataName(data, CommonComponents.EMPTY);
                if (name != null) {
                    return name;
                }
            }
            return Component.literal(String.valueOf(data.data)).append(" x ").append(String.valueOf(data.object));
        }

        @Override
        public void renderObject(GuiGraphics graphics, Object object, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver) {
            for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                handler.renderObject(graphics, object, x, y, width, height, mouseX, mouseY, isMouseOver);
            }
        }

        @Override
        public void writeData(CompoundTag nbt, Collection<Data> data) {
            for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                handler.writeData(nbt, data);
            }
        }

        @Override
        public void readData(CompoundTag nbt, Collection<Data> data) {
            for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                handler.readData(nbt, data);
            }
            data.forEach(d->d.handler=this);
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public int getID(Object object) {
            for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                int hash = handler.getID(object);
                if (hash != 0) {
                    return hash;
                }
            }
            return 0;
        }
    }
}
