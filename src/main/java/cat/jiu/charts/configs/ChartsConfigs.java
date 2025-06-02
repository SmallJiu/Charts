package cat.jiu.charts.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class ChartsConfigs {
    public static final ForgeConfigSpec.IntValue Default_Tick_Time;

    public static final ForgeConfigSpec CONFIG_MAIN;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        Default_Tick_Time = builder.translation("charts.config.default.tick_time")
                .comment("default tick time to display")
                .defineInRange("Default_Tick_Time", 2, 1, 20);

        CONFIG_MAIN = builder.build();
    }
}
