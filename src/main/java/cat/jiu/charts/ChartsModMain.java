package cat.jiu.charts;

import cat.jiu.charts.api.Data;
import cat.jiu.charts.api.IChartsDataHandler;
import cat.jiu.charts.api.data.FluidDataHandler;
import cat.jiu.charts.api.data.ItemDataHandler;
import cat.jiu.charts.api.data.energy.EUDataHandler;
import cat.jiu.charts.api.data.energy.FEDataHandler;
import cat.jiu.charts.configs.ChartsConfigs;
import cat.jiu.charts.net.ChartsNetwork;
import cat.jiu.charts.ui.Handlers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod(ChartsModMain.MODID)
public class ChartsModMain {
    public static final String MODID = "charts";
    public static final String NAME = "Charts";
    public static final String VERSION = "1.20.1-1.0.0";
    public static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(NAME);
    public static final ChartsNetwork NETWORK = ChartsNetwork.getInstance();

    private static final ArrayList<IChartsDataHandler> HANDLERS = new ArrayList<>();
    public static void register(IChartsDataHandler handler){
        HANDLERS.add(handler);
    }
    public static List<IChartsDataHandler> getHandlers(){
        return Collections.unmodifiableList(HANDLERS);
    }
    public static List<Data> getData(Level level, BlockPos pos) {
        List<Data> data = new ArrayList<>();
        for (IChartsDataHandler handler : ChartsModMain.HANDLERS) {
            if (handler.hasData(level, pos)) {
                data.add(new Data(handler));
            }
        }
        return data;
    }
    public static boolean hasData(Level level, BlockPos pos) {
        for (IChartsDataHandler handler : ChartsModMain.HANDLERS) {
            if (handler.hasData(level, pos)) {
                return true;
            }
        }
        return false;
    }

    public ChartsModMain() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        if (FMLLoader.getDist().isClient()) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterBindings);
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ChartsConfigs.CONFIG_MAIN, "jiu/charts/configs.toml");
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event){
        register(ItemDataHandler.INSTANCE);
        register(FEDataHandler.INSTANCE);
        register(EUDataHandler.INSTANCE);
        register(FluidDataHandler.INSTANCE);
    }

    @OnlyIn(Dist.CLIENT)
    private void onClientSetup(final FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, ()->new ConfigScreenHandler.ConfigScreenFactory((mc, parent)->
                new cat.jiu.charts.ui.config.GuiConfig("/config/jiu/charts/configs.toml", parent, ChartsConfigs.CONFIG_MAIN)
        ));
        Handlers.registerItemGetter();
    }

    @OnlyIn(Dist.CLIENT)
    private void onRegisterBindings(RegisterKeyMappingsEvent event) {
        Handlers.SHOW_CHART_KEY.register(event);
    }

    public static final RandomSource RANDOM = RandomSource.create();
    public static int randomColor() {
        int r = RANDOM.nextInt(256);
        int g = RANDOM.nextInt(256);
        int b = RANDOM.nextInt(256);
        return FastColor.ARGB32.color(255, r, g, b);
    }
}
