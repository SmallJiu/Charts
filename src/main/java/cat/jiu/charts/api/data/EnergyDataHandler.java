package cat.jiu.charts.api.data;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.api.IChartsDataHandler;
import cat.jiu.charts.api.data.energy.EUDataHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class EnergyDataHandler implements IChartsDataHandler {
    public static final Icon ICON = new Icon(new ResourceLocation(ChartsModMain.MODID, "textures/gui/energy.png"), 18, 18);
    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public boolean hasData(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        return te != null && this.hasEnergy(te);
    }

    public abstract boolean hasEnergy(BlockEntity tile);

    @Override
    public Collection<Data> getData(Level world, BlockPos pos) {
        Collection<Data> data = new ArrayList<>();
        BlockEntity te = world.getBlockEntity(pos);
        if (te!=null) {
            this.getEnergy(te, data);
        }
        for (Data datum : data) {
            datum.handler = this;
        }
        return data;
    }

    public abstract void getEnergy(BlockEntity tile, Collection<Data> data);

    @Override
    public void writeData(CompoundTag nbt, Collection<Data> data) {
        ListTag dataTag = new ListTag();
        for (Data datum : data) {
            CompoundTag tag = this.writeEnergy(datum);
            if (tag != null && tag.size() > 0) {
                dataTag.add(tag);
            }
        }
        nbt.put("energy", dataTag);
    }

    public abstract CompoundTag writeEnergy(Data data);

    @Override
    public void readData(CompoundTag nbt, Collection<Data> data) {
        for (Tag tag : nbt.getList("energy", 10)) {
            Data d = this.readEnergy((CompoundTag) tag);
            if (d != null) {
                d.handler = this;
                data.add(d);
            }
        }
    }

    public abstract Data readEnergy(CompoundTag nbt);

    @OnlyIn(Dist.CLIENT)
    @Override
    public Component getDataName(Data data, Component failBack) {
        Component name = this.getName(data);
        if (name!=null && name.getContents() != ComponentContents.EMPTY) {
            return name;
        }
        return failBack == CommonComponents.EMPTY ? null : Component.literal(String.valueOf(data.data)).append(" x ").append(String.valueOf(data.object));
    }

    public abstract Component getName(Data data);
}
