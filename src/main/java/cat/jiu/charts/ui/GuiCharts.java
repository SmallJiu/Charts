package cat.jiu.charts.ui;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.api.ChartKey;
import cat.jiu.charts.api.Data;
import cat.jiu.charts.api.IChartsDataHandler;
import cat.jiu.charts.configs.ChartsConfigs;
import cat.jiu.charts.net.msg.SearchDataOnServerMessage;
import cat.jiu.charts.net.msg.SyncBlockEntityMessage;
import cat.jiu.charts.ui.element.ButtonNoBackground;
import cat.jiu.charts.ui.element.ChartList;
import cat.jiu.charts.ui.element.GuiGraph;
import cat.jiu.charts.ui.element.GuiLongComponentGraph;
import cat.jiu.charts.utils.client.RenderUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class GuiCharts extends Screen {
    public static final ResourceLocation BG = new ResourceLocation(ChartsModMain.MODID, "textures/gui/bg.png");
    public static final int
            BG_WIDTH = 200, BG_HEIGHT = 90,
            CHART_WIDTH = 190, CHART_HEIGHT = 80;
    protected static final ConcurrentHashMap<ChartKey, Values> DATA_MAP = new ConcurrentHashMap<>();
    protected static final List<ChartKey> DATA_POS_MAP = new ArrayList<>();
    protected static final Map<String, Function<ChartKey, ItemStack>> ITEM_GETTER = new HashMap<>();
    protected static Data.Time minUpdataTime;
    public static void resetUpdataTime() {
        minUpdataTime = null;
    }

    protected static class Values {
        public static final Values EMPTY = new Values(Collections.emptyList(), Collections.emptyList());
        public final List<Data>
                values,
                emptySelect;
        public Values(List<Data> values, List<Data> emptySelect) {
            this.values = values;
            this.emptySelect = emptySelect;
        }
    }
    public static void updata(ChartKey key, Map<IChartsDataHandler, List<IChartsDataHandler.Data>> data) {
        if (DATA_MAP.containsKey(key)) {
            if (DATA_MAP.get(key).values.isEmpty()) {
                DATA_MAP.remove(key);
                DATA_POS_MAP.remove(key);
                return;
            }
            for (Data value : DATA_MAP.get(key).values) {
                value.updata(Minecraft.getInstance().level.getGameTime(), data.get(value.handler), BG_WIDTH);
            }
        }else {
            Values values = new Values(new ArrayList<>(), new ArrayList<>());
            data.forEach((k,v)->{
                Data d = new Data(k);
                d.updata(Minecraft.getInstance().level.getGameTime(), v, BG_WIDTH);
                values.values.add(d);
            });
            DATA_MAP.put(key, values);
            DATA_POS_MAP.add(key);
            RenderSystem.recordRenderCall(()->
                    Minecraft.getInstance().setScreen(new GuiCharts(key))
            );
        }
    }
    public static void registerItemGetter(String modid, Function<ChartKey, ItemStack> getter) {
        ITEM_GETTER.put(modid, getter);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        minUpdataTime = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.level().isClientSide()) return;
        if (event.phase == TickEvent.Phase.START) {
            for (ChartKey key : GuiCharts.DATA_MAP.keySet()) {
                if (event.player.level().getBlockEntity(key.pos) == null
                || DATA_MAP.get(key).values.isEmpty()
                || DATA_MAP.get(key).values.get(0).times.get(0) == Data.EMPTY_TIME) {
                    GuiCharts.DATA_MAP.remove(key);
                    GuiCharts.DATA_POS_MAP.remove(key);
                }
            }
            if(minUpdataTime == null || minUpdataTime.ticks != ChartsConfigs.Default_Tick_Time.get()) {
                minUpdataTime = Data.tick();
            }
            long gt = event.player.level().getGameTime();
            if (gt >= minUpdataTime.getNextUpdataTicks()) {
                minUpdataTime.setNextUpdataTicks(gt + minUpdataTime.ticks);
                for (ChartKey key : GuiCharts.DATA_MAP.keySet()) {
                    ChartsModMain.NETWORK.sendMessageToServer(new SyncBlockEntityMessage(key));
                }
            }
        }
        while (Handlers.SHOW_CHART_KEY.isClicked()) {
            HitResult result = Minecraft.getInstance().player.pick(20.0D, 0.0F, false);
            AtomicBoolean flag = new AtomicBoolean();
            if (result.getType() == HitResult.Type.BLOCK) {
                ChartKey key = new ChartKey(Minecraft.getInstance().level.dimension(), ((BlockHitResult)result).getBlockPos());
                if (Minecraft.getInstance().level.getBlockEntity(key.pos) != null) {
                    ChartsModMain.LOGGER.debug("{} / {}: {}", key.dimension.location(), String.format("{%s,%s,%s}", key.pos.getX(), key.pos.getY(), key.pos.getZ()), event.player.level().getBlockEntity(((BlockHitResult)result).getBlockPos()));
                    RenderSystem.recordRenderCall(()-> {
                        if (ChartsModMain.hasData(event.player.level(), key.pos)) {
                            Minecraft.getInstance().setScreen(new GuiCharts(key));
                            flag.set(true);
                        }else {
                            ChartsModMain.NETWORK.sendMessageToServer(new SearchDataOnServerMessage(key));
                        }
                    });
                }
            }
            if (!flag.get() && DATA_POS_MAP.size() >= 1) {
                Minecraft.getInstance().setScreen(new GuiCharts(DATA_POS_MAP.get(0)));
            }
        }
    }

    protected final int pos_index;
    protected final ChartKey key;
    protected GuiLongComponentGraph graphs;
    protected ChartList chartList, timeDataList;
    protected List<Component> tooltips;
    protected int bgX, bgY, currentData = 0, currentTime = 0, currentTimeData;

    public GuiCharts(ResourceKey<Level> dimension, BlockPos pos) {
        this(new ChartKey(dimension, pos));
    }
    public GuiCharts(ChartKey key) {
        super(CommonComponents.EMPTY);
        this.key = key;
        this.minecraft = Minecraft.getInstance();
        if (!DATA_MAP.containsKey(key)){
            DATA_MAP.put(key, new Values(ChartsModMain.getData(Minecraft.getInstance().level, key.pos), new ArrayList<>()));
            DATA_POS_MAP.add(key);
        }
        this.pos_index = DATA_POS_MAP.indexOf(this.key);
    }

    public List<Data> getPosData() {
        if (!DATA_MAP.containsKey(this.key)) {
            return Collections.emptyList();
        }
        return DATA_MAP.get(this.key).values;
    }
    public Data getCurrentData() {
        List<Data> d = this.getPosData();
        return d.isEmpty() ? Data.EMPTY_DATA : d.get(this.currentData);
    }
    public Data.Time getCurrentTime() {
        Data d = this.getCurrentData();
        return d.times.isEmpty() ? Data.EMPTY_TIME : d.times.get(this.currentTime);
    }
    public List<IChartsDataHandler.Data> getCurrentTimeData(){
        return this.getCurrentTime().dataMap.get(this.currentTimeData);
    }

    @Override
    protected void init() {
        this.bgX = (this.width - BG_WIDTH) / 2;
        this.bgY = (this.height - BG_HEIGHT) / 2;

        this.graphs = this.addRenderableOnly(new GuiLongComponentGraph(this, this.bgX + 5, this.bgY + 5, CHART_WIDTH, CHART_HEIGHT));

        this.chartList = this.addRenderableWidget(new ChartList(this.bgX - 30, this.bgY, BG_HEIGHT - 1));
        this.timeDataList = this.addRenderableWidget(new ChartList(this.bgX + BG_WIDTH + 6, this.chartList.getTop(), this.chartList.getHeight()));

        this.addRenderableWidget(Button.builder(Component.literal("<"), btn->
            this.offsetSelectData(0, -1)
        ).pos(this.bgX, this.bgY - 25).size(20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(">"), btn->
            this.offsetSelectData(0, 1)
        ).pos(this.bgX + BG_WIDTH - 20, this.bgY - 25).size(20, 20).build());

        Button btn0 = this.addRenderableWidget(new Button.Builder(Component.translatable("charts.display_mode"), btn->
                this.graphs.setDisplayPoints(!this.graphs.isDisplayPoints())
        )
                .size(this.timeDataList.getWidth() + 6, Minecraft.getInstance().font.lineHeight + 2)
                .pos(this.timeDataList.getLeft()+2, this.timeDataList.getBottom() + 2)
                .tooltip(Tooltip.create(Component.translatable("charts.display_mode")))
                .build());
        btn0 = this.addRenderableWidget(Button.builder(Component.literal("R"), btn->{
            for (int i = 0; i < this.getCurrentData().times.size(); i++) {
                this.getCurrentData().times.get(i).dataMap.clear();
            }
            this.graphs.clearData();
        }).pos(btn0.getX(), btn0.getY()+btn0.getHeight()+2).size((this.timeDataList.getWidth() + 6) / 2-1, 10).tooltip(Tooltip.create(Component.translatable("info.config.reload"))).build());

        this.addRenderableWidget(Button.builder(Component.literal("<"), btn->{
            if (DATA_POS_MAP.size() > 1) {
                int pos_index = this.pos_index - 1;
                if (pos_index < 0) {
                    pos_index = GuiCharts.DATA_POS_MAP.size() - 1;
                }
                Minecraft.getInstance().setScreen(new GuiCharts(DATA_POS_MAP.get(pos_index)));
            }
        }).pos(this.bgX, this.bgY + BG_HEIGHT + 2).size(20, 20).build());

        Button btn1 = this.addRenderableWidget(Button.builder(Component.literal(">"), btn->{
            if(DATA_POS_MAP.size() > 1) {
                int pos_index = this.pos_index +1;
                if (pos_index >= GuiCharts.DATA_POS_MAP.size()) {
                    pos_index = 0;
                }
                Minecraft.getInstance().setScreen(new GuiCharts(DATA_POS_MAP.get(pos_index)));
            }
        }).pos(this.bgX + BG_WIDTH - 20, this.bgY + BG_HEIGHT + 2).size(20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("X"), btn->{
            if (DATA_POS_MAP.size()-1 > 0) {
                btn1.onClick(0,0);
            }else {
                Minecraft.getInstance().setScreen(null);
            }
            DATA_MAP.remove(this.key);
            DATA_POS_MAP.remove(this.key);
        }).pos(btn0.getX() + btn0.getWidth() + 2, btn0.getY()).size((this.timeDataList.getWidth() + 6) / 2-1, 10).tooltip(Tooltip.create(Component.translatable("info.config.clear"))).build());

        Component text = Component.literal(String.format("%s%s %s%s%s %s%s%s",
                ChatFormatting.RED, this.key.pos.getX(),
                ChatFormatting.RESET, ChatFormatting.GREEN, this.key.pos.getY(),
                ChatFormatting.RESET, ChatFormatting.AQUA, this.key.pos.getZ()
        ));
        this.addRenderableWidget(new ButtonNoBackground(Button.builder(text, btn->{

                    Minecraft.getInstance().setScreen(null);
                })
                .pos(this.bgX + BG_WIDTH/2 - Minecraft.getInstance().font.width(text)/2, this.bgY + BG_HEIGHT + Minecraft.getInstance().font.lineHeight + 2)
                .size(Minecraft.getInstance().font.width(text) + 4, Minecraft.getInstance().font.lineHeight)
                .tooltip(Tooltip.create(Component.translatable("charts.display_block.0", this.key.dimension.location(), Minecraft.getInstance().level.dimension().location()).append(CommonComponents.NEW_LINE).append(Component.translatable("charts.display_block.1"))))
        ));

        this.setSelectData(true, this.currentData, this.currentTime, this.currentTimeData);
    }

    protected void offsetSelectData(int dataIndex, int timeIndex) {
        this.setSelectData(false, this.currentData+dataIndex, this.currentTime+timeIndex, this.currentTimeData);
    }
    public void setSelectData(boolean reloadChartList, int dataIndex, int timeIndex, int timeDataID) {
        List<Data> dataList = this.getPosData();
        if (dataList!=null && !dataList.isEmpty()) {
            if (reloadChartList || this.chartList.children().isEmpty()) {
                this.loadChartList(dataList, dataIndex);
            }
            if (dataIndex < 0) {
                dataIndex = 0;
            }
            if (dataIndex >= dataList.size()) {
                dataIndex = dataList.size()-1;
            }
            Data data = this.getPosData().get(dataIndex);
            this.currentData = dataIndex;
            if (data != null && !data.times.isEmpty()) {
                if (timeIndex < 0) {
                    timeIndex = 0;
                }
                if (timeIndex >= data.times.size()) {
                    if (timeIndex >= Data.TIMES.size()) {
                        timeIndex = 0;
                    }else {
                        this.getCurrentData().addNewTime(Data.TIMES.get(timeIndex-1));
                        this.graphs.clearData();
                    }
                }
                Data.Time time = data.times.get(timeIndex);
                this.currentTime = timeIndex;
                if (time!=null) {
                    this.setVisibleTime(dataList, dataIndex, timeIndex, true);
                    this.currentTick = time.ticks;
                    this.nextUpdataTick = this.gameTime + this.currentTick;
                    if (time.dataMap.isEmpty()) {
                        return;
                    }
                    this.timeDataList.children().clear();
                    this.timeDataList.setSelected(null);
                    if (time.dataID.size() > 1) {
                        time.dataMap.forEach((k,v)->{
                                ChartList.Chart chart = this.timeDataList.addChart((graphics, x, y, width, height, mouseX, mouseY, isMouseOver, partialTick) -> {
                                    if (this.getCurrentTime().getHandler() != null) {
                                        this.getCurrentTime().getHandler().renderObject(graphics, v.get(0).object, x, y, width, height, mouseX, mouseY, isMouseOver);
                                    }
                                }, chart1 ->
                                    this.setSelectData(false, this.currentData, this.currentTime, (Integer) chart1.object)
                                );
                                chart.object = k;
                                chart.unselect = chart1 -> this.displayAllTimeInfo(time);
                        });
                        for (ChartList.Chart child : this.timeDataList.children()) {
                            if (Objects.equals(child.object, timeDataID)) {
                                this.timeDataList.setSelected(child);
                                break;
                            }
                        }
                    }

                    if (time.dataMap.size() == 1) {
                        timeDataID = time.dataID.get(0);
                    }
                    if (time.dataMap.containsKey(timeDataID)) {
                        List<IChartsDataHandler.Data> timeDataList = time.dataMap.get(timeDataID);
                        this.currentTimeData = timeDataID;
                        this.graphs.clearData();
                        for (IChartsDataHandler.Data value : timeDataList) {
                            this.graphs.addData(new GuiLongComponentGraph.Graph(value.data, data.getColor(timeDataID), new GuiGraph.ColorText(value.getName(), data.getColor(timeDataID))));
                        }
                        time.setCurrentDataID(timeDataID);
                        if (!time.hasListener()) {
                            this.setTimeListener(time);
                        }
                    }else {
                        this.displayAllTimeInfo(time);
                    }
                }
            }
        }
    }

    public void displayAllDataInfo(List<Data> dataList) {
        this.currentData = 0;
        this.graphs.clearData();
        long ticks = this.getCurrentTime().ticks;
        for (int i = 0; i < CHART_WIDTH; i++) {
            List<ColorData> data = new ArrayList<>();
            int finalI = i;
            for (Data data1 : dataList) {
                for (Data.Time time : data1.times) {
                    if (time.ticks == ticks) {
                        try {
                            time.dataMap.forEach((k, v)->
                                    data.add(new ColorData(v.get(finalI), time.data.getColor(k)))
                            );
                        }catch (Exception ignored){}
                    }
                }
            }

            if (!data.isEmpty()) {
                this.graphs.addData(this.getGraph(data));
            }
        }
    }
    public void displayAllTimeInfo(Data.Time tickTime) {
        this.currentTimeData = 0;
        this.graphs.clearData();
        for (int i = 0; i < CHART_WIDTH; i++) {
            List<ColorData> data = new ArrayList<>();
            int finalI = i;
            try {
                tickTime.dataMap.forEach((k, v)->
                        data.add(new ColorData(v.get(finalI), tickTime.data.getColor(k)))
                );
            }catch (Exception ignored){}

            if (!data.isEmpty()) {
                this.graphs.addData(this.getGraph(data));
            }
        }
    }

    protected void loadChartList(List<Data> dataList, int select) {
        ChartList.Chart selectChart = null;
        for (int dataIndex = 0; dataIndex < dataList.size(); dataIndex++) {
            Data data = dataList.get(dataIndex);
            int finalDataIndex = dataIndex;
            ChartList.Chart chart = this.chartList.addChart((graphics, x, y, width, height, mouseX, mouseY, isMouseOver, partialTick)->
                    data.handler.renderObject(graphics, null, x, y, width, height, mouseX, mouseY, isMouseOver),
                    chart1 -> this.setSelectData(false, finalDataIndex, this.currentTime, this.currentTimeData)
            );
            chart.unselect = chart1 -> this.displayAllDataInfo(dataList);
            for (Data.Time time : data.times) {
                this.setTimeListener(time);
            }
            if (select == dataIndex){
                selectChart = chart;
            }
        }
        this.chartList.setSelected(selectChart);
    }

    protected void setTimeListener(Data.Time time) {
        time.setListener((handler, id, data1)->{
            if (this.timeDataList.getSelected() != null && !Screen.hasShiftDown()) {
                this.graphs.addData(new GuiLongComponentGraph.Graph(data1.data, time.data.getColor(id), new GuiGraph.ColorText(handler.getDataName(data1, null), time.data.getColor(id))));
            }
        });
    }

    long gameTime, currentTick, nextUpdataTick;
    @Override
    public void tick() {
        long gt = this.minecraft.level.getGameTime();
        if (gt >= this.nextUpdataTick && this.timeDataList.getSelected() == null) {
            this.gameTime = gt;
            this.nextUpdataTick = this.gameTime + this.currentTick;
            if (Screen.hasShiftDown()) return;
            if (this.chartList.getSelected()!=null) {
                List<ColorData> data = new ArrayList<>();
                Data.Time time = this.getCurrentTime();
                time.dataMap.forEach((k,v)->
                    data.add(new ColorData(v.get(v.size()-1), time.data.getColor(k)))
                );
                if (!data.isEmpty()) {
                    this.graphs.addData(this.getGraph(data));
                }
            }else {
                List<ColorData> data = new ArrayList<>();
                long currentTimeTicks = this.getCurrentTime().ticks;
                List<Data> dataList = this.getPosData();
                for (Data posDatum : dataList) {
                    for (Data.Time time : posDatum.times) {
                        if(time.ticks == currentTimeTicks){
                            time.dataMap.forEach((k,v)->
                                data.add(new ColorData(v.get(v.size()-1), posDatum.getColor(k)))
                            );
                        }
                    }
                }
                if (!data.isEmpty()) {
                    this.graphs.addData(this.getGraph(data));
                }
            }
        }
    }

    public GuiLongComponentGraph.Graph getGraph(List<ColorData> datas) {
        long count = 0;
        List<GuiGraph.ColorText> name = new ArrayList<>();
        for (ColorData data : datas) {
            name.add(new GuiGraph.ColorText(data.data.getName(), data.color));
            count += data.data.data;
        }
        return new GuiLongComponentGraph.Graph(count, Color.GRAY.getRGB(), name);
    }

    public static class ColorData {
        public final IChartsDataHandler.Data data;
        public final int color;
        public ColorData(IChartsDataHandler.Data data, int color) {
            this.data = data;
            this.color = color;
        }
    }

    public void setVisibleTime(){
        this.setVisibleTime(this.getPosData(), this.currentData, this.currentTime, true);
    }
    public void setVisibleTime(List<Data> dataList, int dataIndex, int timeIndex, boolean enable){
        for (int i = 0; i < dataList.size(); i++) {
            Data data = dataList.get(i);
            for (int j = 0; j < data.times.size(); j++) {
                boolean flag = i == dataIndex
                        && j == timeIndex;
                data.times.get(j).setVisible(flag && enable);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics);
        graphics.blit(BG, this.bgX, this.bgY, 0, 0, BG_WIDTH, BG_HEIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        graphics.drawCenteredString(mc.font, (this.currentTime+1)+"/"+this.getCurrentData().times.size(), this.bgX + BG_WIDTH/2, this.bgY-20, Color.WHITE.getRGB());
        graphics.drawCenteredString(mc.font, this.getCurrentTime().getName(), this.bgX + BG_WIDTH/2, this.bgY-20+mc.font.lineHeight+2, Color.WHITE.getRGB());

        graphics.drawCenteredString(mc.font, (DATA_POS_MAP.indexOf(this.key)+1)+"/"+DATA_POS_MAP.size(), this.bgX + BG_WIDTH/2, this.bgY + BG_HEIGHT + 2, Color.WHITE.getRGB());

        ItemStack stack = mc.level.getBlockState(this.key.pos).getCloneItemStack(
                mc.hitResult, mc.level,
                this.key.pos, mc.player
        );
        if (stack==null || stack.isEmpty()) {
            for (Map.Entry<String, Function<ChartKey, ItemStack>> entry : ITEM_GETTER.entrySet()) {
                if(ModList.get().isLoaded(entry.getKey())) {
                    ItemStack result = entry.getValue().apply(this.key);
                    if (result!=null && !result.isEmpty()) {
                        stack = result;
                        break;
                    }
                }
            }
        }
        if (stack != null && !stack.isEmpty()) {
            graphics.renderItem(stack, this.bgX + BG_WIDTH/2 - 8, this.bgY + BG_HEIGHT + 3 + mc.font.lineHeight*2);
            if (RenderUtils.inRange(mouseX, mouseY, this.bgX + BG_WIDTH/2 - 8, this.bgY + BG_HEIGHT + 3 + mc.font.lineHeight*2, 16, 16)) {
                graphics.renderTooltip(mc.font, stack, mouseX, mouseY);
            }
        }

        if (this.tooltips!=null && !this.tooltips.isEmpty()) {
            graphics.renderComponentTooltip(mc.font, this.tooltips, mouseX, mouseY);
            this.tooltips.clear();
        }

        ChartList.Chart timeData = this.timeDataList.getEntryAtPos(mouseX, mouseY);
        if (timeData != null && timeData.isMouseOver) {
            timeData.render.render(graphics, -1, -1, -1, -1, mouseX, mouseY, true, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        for (GuiEventListener child : this.children()) {
            child.setFocused(false);
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        boolean ret = this.getFocused() != null && this.isDragging() && pButton == 0 && this.getFocused().mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (Minecraft.getInstance().options.keyInventory.isActiveAndMatches(InputConstants.getKey(pKeyCode, pScanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @SubscribeEvent
    public static void onExitWorld(ClientPlayerNetworkEvent.LoggingOut event) {
        DATA_MAP.clear();
        DATA_POS_MAP.clear();
    }

    public void addTooltip(Component tooltip) {
        if (this.tooltips==null) {
            this.tooltips = new ArrayList<>();
        }
        this.tooltips.add(tooltip);

        for (int i = 0; i < this.tooltips.size(); i++) {
            if (this.tooltips.get(i) == null) {
                this.tooltips.remove(i);
            }
        }
    }

    public <T> void setCurrentScale(T scale) {
        if (scale instanceof GuiLongComponentGraph.Graph) {
            this.graphs.setScaleData((GuiLongComponentGraph.Graph) scale);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
