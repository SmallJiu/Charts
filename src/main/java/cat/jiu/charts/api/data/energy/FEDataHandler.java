package cat.jiu.charts.api.data.energy;

import cat.jiu.charts.api.data.EnergyDataHandler;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.EmptyEnergyStorage;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Collection;
import java.util.Objects;

public class FEDataHandler extends EnergyDataHandler {
    public static final FEDataHandler INSTANCE = new FEDataHandler();
    public static final Component NAME = Component.literal("RF");

    @Override
    public boolean hasEnergy(BlockEntity tile) {
        if (tile.getCapability(ForgeCapabilities.ENERGY).orElse(EmptyEnergyStorage.INSTANCE) != EmptyEnergyStorage.INSTANCE){
            return true;
        }
        for (Direction side : VALUES) {
            if (tile.getCapability(ForgeCapabilities.ENERGY, side).orElse(EmptyEnergyStorage.INSTANCE) != EmptyEnergyStorage.INSTANCE){
                return true;
            }
        }
        return false;
    }

    @Override
    public void getEnergy(BlockEntity tile, Collection<Data> data) {
        IEnergyStorage handler = tile.getCapability(ForgeCapabilities.ENERGY).orElse(EmptyEnergyStorage.INSTANCE);
        if (handler.getMaxEnergyStored() > 0) {
            data.add(new Data(handler, handler.getEnergyStored()));
        }
        if (data.isEmpty()) {
            for (Direction side : VALUES) {
                IEnergyStorage handler1 = tile.getCapability(ForgeCapabilities.ENERGY, side).orElse(EmptyEnergyStorage.INSTANCE);
                if (handler1.getMaxEnergyStored() > 0) {
                    data.add(new Data(handler1, handler1.getEnergyStored()));
                }
            }
        }
    }

    @Override
    public CompoundTag writeEnergy(Data data) {
        if (data.object instanceof IEnergyStorage) {
            IEnergyStorage storage = (IEnergyStorage) data.object;
            CompoundTag tag = new CompoundTag();
            tag.putLong("data", data.data);
            tag.putInt("energy", storage.getEnergyStored());
            tag.putInt("maxEnergy", storage.getMaxEnergyStored());
            return tag;
        }
        return null;
    }

    @Override
    public Data readEnergy(CompoundTag nbt) {
        return new Data(new EnergyStorage(nbt.getInt("maxEnergy"), 0, 0, nbt.getInt("energy")), nbt.getLong("data"));
    }

    @Override
    public int getID(Object object) {
        if (object instanceof IEnergyStorage) {
            return Objects.hashCode(((IEnergyStorage) object).getMaxEnergyStored());
        }
        return 0;
    }

    @Override
    public Component getName(Data data) {
        return Component.literal(String.valueOf(data.data)).append(CommonComponents.SPACE).append(NAME);
    }
}
