package cat.jiu.charts.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class BaseMessage {
    public abstract void toBytes(FriendlyByteBuf buf);
    public abstract void fromBytes(FriendlyByteBuf buf);
    public abstract boolean handler(Supplier<NetworkEvent.Context> context);

    public static abstract class CallbackMessage<T extends BaseMessage> extends BaseMessage {
        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(()->{
                T callback = this.callback(context);
                if (callback != null) {
                    this.send(context, callback);
                }
            });
            return true;
        }

        protected abstract T callback(Supplier<NetworkEvent.Context> context);

        protected abstract void send(Supplier<NetworkEvent.Context> context, T msg);
    }
}
