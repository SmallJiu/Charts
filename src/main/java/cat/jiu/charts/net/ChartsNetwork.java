package cat.jiu.charts.net;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.net.msg.*;
import cat.jiu.charts.utils.BaseNetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;

public class ChartsNetwork extends BaseNetworkHandler {
    private static ChartsNetwork INSTANCE;

    public static ChartsNetwork getInstance() {
        if (INSTANCE==null) INSTANCE = new ChartsNetwork();
        return INSTANCE;
    }

    private ChartsNetwork() {
        super(new ResourceLocation(ChartsModMain.MODID, "main_network"), ChartsModMain.VERSION);

		this.register(SyncBlockEntityMessage.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(SyncBlockEntityMessage.Call.class, NetworkDirection.PLAY_TO_CLIENT);

		this.register(SearchDataOnServerMessage.class, NetworkDirection.PLAY_TO_SERVER);
    }
}
