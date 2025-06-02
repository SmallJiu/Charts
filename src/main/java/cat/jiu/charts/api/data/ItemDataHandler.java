package cat.jiu.charts.api.data;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.api.IChartsDataHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemDataHandler implements IChartsDataHandler {
    public static final ItemDataHandler INSTANCE = new ItemDataHandler();
    public static final Icon ICON = new Icon(new ResourceLocation(ChartsModMain.MODID, "textures/gui/item.png"), 18, 18);
    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public boolean hasData(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te!=null) {
            if (te.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(EmptyHandler.INSTANCE) != EmptyHandler.INSTANCE){
                return true;
            }
            for (Direction side : VALUES) {
                if (te.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(EmptyHandler.INSTANCE) != EmptyHandler.INSTANCE){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Collection<Data> getData(Level world, BlockPos pos) {
        Collection<Data> data = new ArrayList<>();
        BlockEntity te = world.getBlockEntity(pos);
        if (te!=null) {
            IItemHandler handler = te.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(EmptyHandler.INSTANCE);
            if (handler != EmptyHandler.INSTANCE) {
                getData(handler, data);
            }
            if (data.isEmpty()) {
                for (Direction side : VALUES) {
                    IItemHandler handler1 = te.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(EmptyHandler.INSTANCE);
                    if (handler1 != EmptyHandler.INSTANCE) {
                        getData(handler1, data);
                    }
                }
            }
        }
        for (Data datum : data) {
            datum.handler = this;
        }
        return data;
    }
    public static void getData(IItemHandler handler, Collection<Data> data) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                boolean has = false;
                for (ItemStack itemStack : stacks) {
                    if (itemStack.is(stack.getItem())) {
                        itemStack.grow(stack.getCount());
                        has = true;
                    }
                }
                if (!has) {
                    stacks.add(stack.copy());
                }
            }
        }
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                data.add(new Data(stack, stack.getCount()));
            }
        }
    }

    @Override
    public void writeData(CompoundTag nbt, Collection<Data> data) {
        ListTag dataTag = new ListTag();
        for (Data datum : data) {
            if (datum.object instanceof ItemStack && !((ItemStack) datum.object).isEmpty()) {
                CompoundTag tag = new CompoundTag();
                ((ItemStack) datum.object).save(tag);
//                tag.putLong("count", datum.data);
                tag.putInt("Count", ((ItemStack) datum.object).getCount());
                dataTag.add(tag);
            }
        }
        nbt.put("item", dataTag);
    }

    @Override
    public void readData(CompoundTag nbt, Collection<Data> data) {
        for (Tag tag : nbt.getList("item", 10)) {
            ItemStack stack = ItemStack.of((CompoundTag) tag);
            stack.setCount(((CompoundTag) tag).getInt("Count"));
            if (!stack.isEmpty()){
                Data d = new Data(stack, stack.getCount());
                d.handler = this;
                data.add(d);
            }
        }
    }

    @Override
    public int getID(Object object) {
        if (object instanceof ItemStack) {
            return ((ItemStack) object).getItem().hashCode();
        }
        return 0;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Component getDataName(Data data, Component failBack) {
        if (data.object instanceof ItemStack) {
            return Component.literal(String.valueOf(data.data)).append(" x ").append(((ItemStack)data.object).getDisplayName());
        }
        if (data.object instanceof Data) {
            return this.getDataName((Data) data.object, failBack);
        }
        return failBack == CommonComponents.EMPTY ? null : Component.literal(String.valueOf(data.data)).append(" x ").append(String.valueOf(data.object));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderObject(GuiGraphics graphics, Object object, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver) {
        if (object instanceof ItemStack) {
            ItemStack stack = (ItemStack) object;
            if (x != -1 || y != -1 || width != -1 || height != -1) {
                graphics.renderItem(stack, x, y);
            }
            if (isMouseOver) {
                graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
            }
            return;
        }
        if (object instanceof Data) {
            this.renderObject(graphics, ((Data) object).object, x, y, width, height, mouseX, mouseY, isMouseOver);
            return;
        }
        IChartsDataHandler.super.renderObject(graphics, null, x, y, width, height, mouseX, mouseY, isMouseOver);
    }
}
