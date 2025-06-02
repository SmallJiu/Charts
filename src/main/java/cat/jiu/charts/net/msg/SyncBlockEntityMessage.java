package cat.jiu.charts.net.msg;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.api.ChartKey;
import cat.jiu.charts.api.IChartsDataHandler;
import cat.jiu.charts.utils.BaseMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncBlockEntityMessage extends BaseMessage {
    protected ChartKey key;

    public SyncBlockEntityMessage() {
    }

    public SyncBlockEntityMessage(ChartKey key) {
        this.key = key;
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(()->{
            if (context.get().getSender()!=null) {
                CompoundTag nbt = getDataNBT(context.get().getSender().level(), this.key.pos);
                if (!nbt.isEmpty()) {
                    ChartsModMain.NETWORK.sendMessageToPlayer(new Call(this.key, nbt), context.get().getSender());
                }
            }
        });
        return true;
    }

    public static CompoundTag getDataNBT(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
            if (handler.hasData(level, pos)) {
                handler.writeData(nbt, handler.getData(level, pos));
            }
        }
        return nbt;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceKey(this.key.dimension);
        buf.writeBlockPos(this.key.pos);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf) {
        this.key = new ChartKey(buf.readResourceKey(Registries.DIMENSION), buf.readBlockPos());
    }

    public static class Call extends BaseMessage {
        protected ChartKey key;
        protected CompoundTag nbt;

        public Call() {
        }

        public Call(ChartKey key, CompoundTag nbt) {
            this.key = key;
            this.nbt = nbt;
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {
            buf.writeResourceKey(this.key.dimension);
            buf.writeBlockPos(this.key.pos);
            buf.writeNbt(this.nbt);
        }

        @Override
        public void fromBytes(FriendlyByteBuf buf) {
            this.key = new ChartKey(buf.readResourceKey(Registries.DIMENSION), buf.readBlockPos());
            this.nbt = buf.readNbt();
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            if (FMLLoader.getDist().isClient()) {
                Map<IChartsDataHandler, List<IChartsDataHandler.Data>> data = new HashMap<>();
                for (IChartsDataHandler handler : ChartsModMain.getHandlers()) {
                    List<IChartsDataHandler.Data> handlerData = new ArrayList<>();
                    handler.readData(this.nbt, handlerData);
                    if (!handlerData.isEmpty()) {
                        data.put(handler, handlerData);
                    }
                }
                if (!data.isEmpty()) {
                    cat.jiu.charts.ui.GuiCharts.updata(this.key, data);
                }
            }
            return true;
        }
    }
}
