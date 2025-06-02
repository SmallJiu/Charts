package cat.jiu.charts.net.msg;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.api.ChartKey;
import cat.jiu.charts.utils.BaseMessage;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SearchDataOnServerMessage extends BaseMessage {
    protected ChartKey key;

    public SearchDataOnServerMessage() {
    }

    public SearchDataOnServerMessage(ChartKey key) {
        this.key = key;
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

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(()->{
            if (context.get().getSender()!=null) {
                CompoundTag nbt = SyncBlockEntityMessage.getDataNBT(context.get().getSender().level(), this.key.pos);
                if (!nbt.isEmpty()) {
                    ChartsModMain.NETWORK.sendMessageToPlayer(new SyncBlockEntityMessage.Call(this.key, nbt), context.get().getSender());
                }
            }
        });
        return true;
    }
}
