package cat.jiu.charts.api.data.energy;

import cat.jiu.charts.api.data.EnergyDataHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EUDataHandler extends EnergyDataHandler {
    public static final EUDataHandler INSTANCE = new EUDataHandler();
    public static final Component EU_NAME = Component.literal("EU");
    private static final Map<String, EUHandler> HANDLER_MAP = new HashMap<>();

    /**
     * 没有计划支持EU，如果你需要，你得自己写</p>
     * There are no plans to support the EU. If you want it, you have to write it for yourself.
     */
    public static void registerHandler(String modid, EUHandler handler) {
        HANDLER_MAP.put(modid, handler);
    }

    @Override
    public boolean hasEnergy(BlockEntity tile) {
        for (Map.Entry<String, EUHandler> entry : HANDLER_MAP.entrySet()) {
            if (ModList.get().isLoaded(entry.getKey())) {
                if (entry.getValue().checker.apply(tile)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void getEnergy(BlockEntity tile, Collection<Data> data) {
        for (Map.Entry<String, EUHandler> entry : HANDLER_MAP.entrySet()) {
            if (ModList.get().isLoaded(entry.getKey())) {
                if (entry.getValue().checker.apply(tile)) {
                    entry.getValue().getter.accept(tile, data);
                }
            }
        }
    }

    @Override
    public CompoundTag writeEnergy(Data data) {
        for (Map.Entry<String, EUHandler> entry : HANDLER_MAP.entrySet()) {
            if (ModList.get().isLoaded(entry.getKey())) {
                CompoundTag tag = entry.getValue().write.apply(data);
                if (tag!=null && tag.size()>0) {
                    return tag;
                }
            }
        }
        return null;
    }

    @Override
    public Data readEnergy(CompoundTag nbt) {
        for (Map.Entry<String, EUHandler> entry : HANDLER_MAP.entrySet()) {
            if (ModList.get().isLoaded(entry.getKey())) {
                Data data = entry.getValue().read.apply(nbt);
                if (data != null) {
                    return data;
                }
            }
        }
        return null;
    }

    @Override
    public int getID(Object object) {
        for (Map.Entry<String, EUHandler> entry : HANDLER_MAP.entrySet()) {
            if (ModList.get().isLoaded(entry.getKey())) {
                int ret = entry.getValue().id.apply(object);
                if (ret != 0) {
                    return ret;
                }
            }
        }
        return 0;
    }

    @Override
    public Component getName(Data data) {
        for (Map.Entry<String, EUHandler> entry : HANDLER_MAP.entrySet()) {
            if (ModList.get().isLoaded(entry.getKey())) {
                Component name = entry.getValue().name.apply(data);
                if (name!=null && name.getContents() != ComponentContents.EMPTY) {
                    return name;
                }
            }
        }
        return null;
    }

    public static class EUHandler {
        public final Function<BlockEntity, Boolean> checker;
        public final BiConsumer<BlockEntity, Collection<Data>> getter;
        public final Function<Data, CompoundTag> write;
        public final Function<CompoundTag, Data> read;
        public final Function<Data, Component> name;
        public final Function<Object, Integer> id;

        public EUHandler(
                Function<BlockEntity, Boolean> checker,
                BiConsumer<BlockEntity, Collection<Data>> getter,
                Function<Data, CompoundTag> write,
                Function<CompoundTag, Data> read,
                Function<Data, Component> name,
                Function<Object, Integer> id)
        {
            this.checker = checker;
            this.getter = getter;
            this.write = write;
            this.read = read;
            this.name = name;
            this.id = id;
        }
    }
}
