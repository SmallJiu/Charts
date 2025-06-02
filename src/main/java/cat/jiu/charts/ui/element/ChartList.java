package cat.jiu.charts.ui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class ChartList extends ObjectSelectionList<ChartList.Chart> {
    public ChartList(int x, int y, int height) {
        super(Minecraft.getInstance(), 24, height, y, y+height, 22);
        this.setLeftPos(x);
        this.setRenderBackground(false);
        this.setRenderHeader(false, 0);
        this.setRenderTopAndBottom(false);
        this.setRenderSelection(false);
    }

    @Override
    public boolean isSelectedItem(int pIndex) {
        return this.getSelected() != null && this.getSelected().index == pIndex;
    }

    public Chart addChart(Chart.Render render, Consumer<Chart> clicker) {
        Chart chart = new Chart(this, this.children().size(), render, clicker);
        this.addEntry(chart);
        return chart;
    }

    public Chart getEntryAtPos(double pMouseX, double pMouseY) {
        return this.getEntryAtPosition(pMouseX, pMouseY);
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.width + 2;
    }

    public static class Chart extends ObjectSelectionList.Entry<Chart> {
        public final ChartList list;
        public final int index;
        public final Render render;
        public final Consumer<Chart> select;
        public Consumer<Chart> unselect;
        public Object object;
        public boolean isMouseOver;
        public Chart(ChartList list, int index, Render render, Consumer<Chart> select) {
            this.list = list;
            this.index = index;
            this.render = render;
            this.select = select;
        }

        @Override
        public @NotNull Component getNarration() {
            return CommonComponents.EMPTY;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            this.render.render(graphics, x + 3, y+3, 16, 16, mouseX, mouseY, isMouseOver, partialTick);
        }

        @Override
        public void renderBack(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            this.isMouseOver = isMouseOver;
            int u = this.list.isSelectedItem(index) ? 1 : 24;
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(AbstractWidget.WIDGETS_LOCATION, x, y, u, 23, 22, 22);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Chart chart = (Chart) o;
            return index == chart.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index);
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if(this.list.isSelectedItem(this.index)) {
                this.list.setSelected(null);
                if(this.unselect != null) {
                    this.unselect.accept(this);
                }
                return false;
            }
            this.select.accept(this);
            return true;
        }

        public interface Render {
            void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick);
        }
    }
}
