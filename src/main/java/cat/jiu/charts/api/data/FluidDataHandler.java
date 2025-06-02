package cat.jiu.charts.api.data;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.api.IChartsDataHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class FluidDataHandler implements IChartsDataHandler {
    public static final FluidDataHandler INSTANCE = new FluidDataHandler();
    public static final Icon ICON = new Icon(new ResourceLocation(ChartsModMain.MODID, "textures/gui/fluid.png"), 18, 18);
    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public boolean hasData(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te!=null) {
            for (Direction side : VALUES) {
                if (te.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(EmptyFluidHandler.INSTANCE) != EmptyFluidHandler.INSTANCE){
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
            for (Direction side : VALUES) {
                IFluidHandler handler = te.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(EmptyFluidHandler.INSTANCE);
                if (handler != EmptyFluidHandler.INSTANCE) {
                    for (int tank = 0; tank < handler.getTanks(); tank++) {
                        FluidStack fluid = handler.getFluidInTank(tank);
                        if (!fluid.isEmpty()) {
                            data.add(new Data(fluid, fluid.getAmount()));
                        }
                    }
                }
            }
        }
        for (Data datum : data) {
            datum.handler = this;
        }
        return data;
    }

    @Override
    public void writeData(CompoundTag nbt, Collection<Data> data) {
        ListTag dataTag = new ListTag();
        for (Data datum : data) {
            if (datum.object instanceof FluidStack) {
                dataTag.add(((FluidStack) datum.object).writeToNBT(new CompoundTag()));
            }
        }
        nbt.put("fluid", dataTag);
    }

    @Override
    public void readData(CompoundTag nbt, Collection<Data> data) {
        for (Tag tag : nbt.getList("fluid", 10)) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT((CompoundTag) tag);
            Data d = new Data(stack, stack.getAmount());
            d.handler = this;
            data.add(d);
        }
    }

    @Override
    public int getID(Object object) {
        if (object instanceof IFluidHandler) {
            IFluidHandler handler = (IFluidHandler) object;
            ArrayList<Object> objs = new ArrayList<>();
            objs.add(handler.getTanks());
            for (int i = 0; i < handler.getTanks(); i++) {
                objs.add(handler.getFluidInTank(i).getFluid());
            }

        }
        if (object instanceof FluidStack) {
            return Objects.hashCode(object);
        }
        return 0;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Component getDataName(Data data, Component failBack) {
        if (data.object instanceof FluidStack) {
            return Component.literal(String.valueOf(data.data)).append(" x ").append(((FluidStack)data.object).getDisplayName());
        }
        return failBack == CommonComponents.EMPTY ? null : Component.literal(String.valueOf(data.data)).append(" x ").append(String.valueOf(data.object));
    }
}
