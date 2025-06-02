package cat.jiu.charts.ui;

import cat.jiu.charts.utils.client.KeyUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import org.lwjgl.glfw.GLFW;

public class Handlers {
	@OnlyIn(Dist.CLIENT)
	public static final KeyUtil SHOW_CHART_KEY = KeyUtil.of("chart_key", GLFW.GLFW_KEY_P, KeyMapping.CATEGORY_INTERFACE);

	@OnlyIn(Dist.CLIENT)
	public static void registerItemGetter() {
		GuiCharts.registerItemGetter("sophisticatedbackpacks", key -> {
			BlockEntity entity = Minecraft.getInstance().level.getBlockEntity(key.pos);
			if(entity instanceof BackpackBlockEntity) {
				return ((BackpackBlockEntity) entity).getBackpackWrapper().getBackpack();
			}
			return ItemStack.EMPTY;
		});
	}
}
