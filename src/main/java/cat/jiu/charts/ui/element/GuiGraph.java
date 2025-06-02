package cat.jiu.charts.ui.element;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.ui.GuiCharts;
import cat.jiu.charts.utils.client.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 * @author Mekanism
 * @see mekanism.client.gui.element.graph.GuiGraph
 */
@OnlyIn(Dist.CLIENT)
public abstract class GuiGraph<T> extends AbstractWidget {
    protected final GuiCharts parent;
    protected final List<T> graphData;
    protected final Map<Integer, Style> colorStyles = new HashMap<>();
    protected GraphDataHandler handler; // arg: hoverIndex

    protected boolean fixedScale = false;
    protected T scaleData;
    protected boolean displayPoints;

    protected GuiGraph(GuiCharts parent, int x, int y, int width, int height, List<T> graphData, GraphDataHandler handler) {
        super(x,y,width,height,CommonComponents.EMPTY);
        this.parent = parent;
        this.graphData = graphData;
        this.setHandler(handler);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        //Draw the graph
        int size = graphData.size();
        int x = this.getX() + 1;
        int y = this.getY() + 1;
        int height = this.height - 2;

        for (int i = 0; i < size; i++) {
            int relativeHeight = getRelativeHeight(i, height);
            if (this.displayPoints) {
                graphics.vLine(x+i, y+height-relativeHeight-1,y+height-relativeHeight+1, this.getDataColor(this.graphData.get(i)));
            }else {
                graphics.vLine(x+i, y+height-relativeHeight-1,y+height-relativeHeight+1, Color.GREEN.getRGB());
                graphics.vLine(x+i, y+height-relativeHeight, y+height, this.getDataColor(this.graphData.get(i)));
            }
            int hoverIndex = mouseX - getX();
            if (hoverIndex == i && mouseY >= getY() && mouseY < getY() + height) {
                graphics.vLine(x+i, y, y+height, Color.LIGHT_GRAY.getRGB());
                graphics.vLine(x+i, y+height-relativeHeight-1,y+height-relativeHeight+1, Color.MAGENTA.getRGB());
            } else {
                //Note: We can skip resetting the color if we enter the above if as it will reset it already
                graphics.setColor(1, 1, 1, 1);
            }
            RenderSystem.disableBlend();
        }
        RenderUtils.hLine(graphics, x, y+height, size-1, Color.DARK_GRAY.getRGB());
        if (this.isMouseOver(mouseX, mouseY)) {
            this.renderToolTip(mouseX);
        }
    }

    protected abstract int getDataColor(T data);
    protected abstract List<ColorText> getColorText(T data);

    public void enableFixedScale(T scale) {
        this.fixedScale = true;
        this.scaleData = scale;
    }

    public void setScaleData(T scaleData) {
        this.scaleData = scaleData;
    }

    public void setDisplayPoints(boolean displayPoints) {
        this.displayPoints = displayPoints;
    }

    public boolean isDisplayPoints() {
        return displayPoints;
    }

    public T getData(int index) {
        return this.graphData.get(index);
    }

    protected abstract int compare(T data1, T data2);
    public void clearData(){
        this.graphData.clear();
    }

    public void addData(T data) {
        if (this.graphData.size() == width - 2) {
            this.graphData.remove(0);
        }
        this.graphData.add(data);
        for (ColorText colorText : this.getColorText(data)) {
            if(!this.colorStyles.containsKey(colorText.color)) {
                this.colorStyles.put(colorText.color, Style.EMPTY.withColor(colorText.color));
            }
        }
        if (!this.fixedScale) {
            for (T graphData : this.graphData) {
                if (this.compare(graphData, this.scaleData) > 0) {
                    this.parent.setCurrentScale(graphData);
                }
            }
        }
    }

    public void addData(Collection<T> data) {
        for (T t : data) {
            this.addData(t);
        }
    }

    protected abstract int getRelativeHeight(int index, int height);

    public void setHandler(GraphDataHandler handler) {
        this.handler = handler;
    }

    public GraphDataHandler getHandler() {
        return handler;
    }

    protected List<ColorText> getDataDisplay(int hoverIndex) {
        if (this.handler!=null) {
            return this.handler.apply(hoverIndex);
        }
        return Collections.singletonList(new ColorText(Component.literal(String.valueOf(this.graphData.get(hoverIndex))), ChartsModMain.randomColor()));
    }

    protected void renderToolTip(int mouseX) {
        int hoverIndex = mouseX - getX();
        if (hoverIndex >= 0 && hoverIndex < graphData.size()) {
            for (ColorText colorText : this.getDataDisplay(hoverIndex)) {
                this.parent.addTooltip(Component.literal("■").withStyle(this.colorStyles.get(colorText.color)).append(" : ").append(colorText.text));
            }
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {

    }

    public static int clampToInt(double d) {
        if (d < Integer.MAX_VALUE) {
            return (int) d;
        }
        return Integer.MAX_VALUE;
    }

    public interface GraphDataHandler extends Function<Integer, List<ColorText>> {
        public static final GraphDataHandler EMPTY = index -> Collections.emptyList();

        @Override
        List<ColorText> apply(Integer index);
    }
    public static class ColorText {
        public final Component text;
        public final int color;

        public ColorText(Component text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}
