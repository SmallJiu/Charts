package cat.jiu.charts.ui.element;

import cat.jiu.charts.ui.GuiCharts;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiLongComponentGraph extends GuiGraph<GuiLongComponentGraph.Graph> {
    public static final Graph DEFAULT_SCALE = new Graph(32, 0 ,Collections.emptyList());
    public GuiLongComponentGraph(GuiCharts gui, int x, int y, int width, int height) {
        super(gui, x, y, width, height, new ArrayList<>(), null);
        this.setScaleData(DEFAULT_SCALE);
        this.setHandler(index -> this.getData(index).name);
    }

    @Override
    public void clearData() {
        super.clearData();
        this.setScaleData(DEFAULT_SCALE);
    }

    @Override
    protected int compare(Graph data1, Graph data2) {
        return Long.compare(data1.data, data2.data);
    }

    @Override
    protected int getDataColor(Graph data) {
        return data.color;
    }

    @Override
    protected List<ColorText> getColorText(Graph data) {
        return data.name;
    }

    @Override
    protected int getRelativeHeight(int index, int height) {
        long data = Math.min(this.scaleData.data, this.getData(index).data);
        return clampToInt(data * height / (double) scaleData.data);
    }

    public static class Graph {
        public static final Graph EMPTY = new Graph(0, 0, Collections.emptyList());
        public final long data;
        public final int color;
        public final List<ColorText> name;
        public Graph(long data, int color, ColorText name) {
            this(data, color, Collections.singletonList(name));
        }
        public Graph(long data, int color, List<ColorText> name) {
            this.data = data;
            this.color = color;
            this.name = name;
        }
    }
}
