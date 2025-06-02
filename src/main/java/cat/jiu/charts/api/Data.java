package cat.jiu.charts.api;

import cat.jiu.charts.ChartsModMain;
import cat.jiu.charts.configs.ChartsConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;

import java.util.*;

public class Data {
    public static final ArrayList<Time> TIMES = new ArrayList<>();
    public static Time tick() {
        int index = Data.TIMES.size()-1;
        if (Data.TIMES.get(index).ticks != Data.DAYS_1.ticks
         && Data.TIMES.get(index).ticks != ChartsConfigs.Default_Tick_Time.get()) {
            Data.TIMES.remove(index);
        }
        return new Time(ChartsConfigs.Default_Tick_Time.get(), "charts.time.tick", ChartsConfigs.Default_Tick_Time.get());
    }
    public static final Time
            SECONDS_1 = new Time(20, "charts.time.sec", 1), // 1 sec
            SECONDS_10 = new Time(SECONDS_1.ticks * 10, "charts.time.sec", 10), // 10 sec
            SECONDS_30 = new Time(SECONDS_1.ticks * 30, "charts.time.sec", 30), // 30 sec
            MINUTES_1 = new Time(SECONDS_1.ticks * 60, "charts.time.minute", 1), // 1m
            MINUTES_10 = new Time(MINUTES_1.ticks * 10, "charts.time.minute", 10), // 10m
            MINUTES_30 = new Time(MINUTES_1.ticks * 30, "charts.time.minute", 30), // 30m
            HOURS_1 = new Time(MINUTES_1.ticks * 60, "charts.time.hour", 1), // 1 hour
            HOURS_12 = new Time(HOURS_1.ticks * 12, "charts.time.hour", 12), // 10 hour
            DAYS_1 = new Time(HOURS_1.ticks * 24, "charts.time.day", 1); // 1 day

    public static final Data EMPTY_DATA = new Data(null);
    public static final Time EMPTY_TIME = new Time(0, "empty");

    public final IChartsDataHandler handler;
    public final ArrayList<Time> times = new ArrayList<>();
    public final HashMap<Integer, Integer> dataColor = new HashMap<>();
    public final List<Integer> colorMap = new ArrayList<>();

    public Data(IChartsDataHandler handler) {
        this.handler = handler;
        this.addNewTime(tick());
    }

    public void addNewTime(Time time) {
        if (this.handler!=null) {
            Time newTime = time.newTime().setHandler(this.handler);
            newTime.data = this;
            this.times.add(newTime);
            this.times.sort(Comparator.comparingLong(t -> t.ticks));
        }
    }

    public void updata(long gameTime, Collection<IChartsDataHandler.Data> newData, int maxSize) {
        Time pre = null;
        for (Time time : this.times) {
            if (gameTime >= time.getNextUpdataTicks()) {
                time.updata(this.handler, gameTime, pre, pre!=null ? null : newData, maxSize);
                pre = time;
            }
        }
    }
    public void updata(Level world, BlockPos pos, int maxSize) {
        this.updata(world.getGameTime(), this.handler.getData(world, pos), maxSize);
    }

    public void setVisibleTime(int index) {
        for (int i = 0; i < times.size(); i++) {
            times.get(i).setVisible(i == index);
        }
    }
    protected int color() {
        int color = ChartsModMain.randomColor();
        return this.colorMap.contains(color) ? this.color() : color;
    }

    public int getColor(int dataID) {
        if (!this.dataColor.containsKey(dataID)) {
            this.dataColor.put(dataID, this.color());
        }
        return this.dataColor.get(dataID);
    }

    public static class Time {
        public Data data;
        public final long ticks;
        public final String name;
        public final Object[] nameArgs;
        public final HashMap<Integer, List<IChartsDataHandler.Data>> dataMap = new HashMap<>();
        public final List<Integer> dataID = new ArrayList<>();
        protected long nextUpdataTicks;
        protected ChangeListener listener;
        protected boolean visible;
        protected int currentDataID;
        protected IChartsDataHandler handler;

        public Time(long ticks, String name, Object... nameArgs) {
            this.ticks = ticks;
            this.name = name;
            this.nameArgs = nameArgs;

            if (!Data.TIMES.contains(this)) {
                Data.TIMES.add(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Time time = (Time) o;
            return ticks == time.ticks;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ticks);
        }

        public void setListener(ChangeListener listener) {
            this.listener = listener;
        }
        public boolean hasListener(){
            return this.listener!=null;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void setCurrentDataID(int currentDataID) {
            this.currentDataID = currentDataID;
        }

        public int getCurrentDataID() {
            return currentDataID;
        }

        public IChartsDataHandler getHandler() {
            return handler;
        }

        public Time setHandler(IChartsDataHandler handler) {
            this.handler = handler;
            return this;
        }

        public void updata(IChartsDataHandler handler, long gameTicks, Time pre, Collection<IChartsDataHandler.Data> newData, int maxSize) {
            this.nextUpdataTicks = gameTicks + this.ticks;
            if (pre==null && newData!=null) {
                for (IChartsDataHandler.Data datum : newData) {
                    this.addData(handler, datum, maxSize);
                }
                this.checkNotFound(handler, newData, maxSize);
            }else if (pre != null){
                for (Map.Entry<Integer, List<IChartsDataHandler.Data>> entry : pre.dataMap.entrySet()) {
                    IChartsDataHandler.Data averageData = pre.getAverageData(this.ticks, entry.getKey());
                    averageData.handler = handler;
                    this.addData(handler, averageData, maxSize);
                }
            }
        }
        public IChartsDataHandler.Data getAverageData(long count, int object){
            List<IChartsDataHandler.Data> dataList = this.dataMap.get(object);
            if (count >= dataList.size()) {
                count = dataList.size();
            }
            Object data_object = null;
            long all = 0;
            int j = 0;
            for (int i = dataList.size() - 1; i >= 0; i--) {
                if (data_object==null) data_object = dataList.get(i).object;
                if (j >= count) {
                    break;
                }
                j++;
                all += dataList.get(i).data;
            }
            return new IChartsDataHandler.Data(data_object, all / count);
        }

        protected void checkNotFound(IChartsDataHandler handler, Collection<IChartsDataHandler.Data> newData, int maxSize) {
            List<Integer> hash = new ArrayList<>();
            for (IChartsDataHandler.Data data : newData) {
                hash.add(handler.getID(data.object));
            }
            for (Integer id : this.dataMap.keySet()) {
                if (!hash.contains(id)) {
                    IChartsDataHandler.Data data = new IChartsDataHandler.Data(this.dataMap.get(id).get(0).object, 0);
                    data.handler = handler;
                    this.dataMap.get(id).add(data);
                    if (this.hasListener() && this.isVisible() && this.currentDataID == id) {
                        this.listener.changed(handler, id, data);
                    }
                }
            }
        }

        public void addData(IChartsDataHandler handler, IChartsDataHandler.Data data, int maxSize) {
            int id = handler.getID(data.object);
            if (id==0) return;
            if (!this.dataMap.containsKey(id)) {
                this.dataMap.put(id, new ArrayList<>());
                this.dataID.add(id);
                this.currentDataID = id;
            }
            if (this.dataMap.get(id).size() >= maxSize - 2) {
                this.dataMap.get(id).remove(0);
            }
            this.dataMap.get(id).add(data);

            if (this.hasListener() && this.isVisible() && this.currentDataID == id) {
                this.listener.changed(handler, id, data);
            }
        }

        public void removeData(int id) {
            this.dataMap.remove(id);
            this.dataID.remove((Integer) id);
        }

        public long getNextUpdataTicks() {
            return nextUpdataTicks;
        }

        public void setNextUpdataTicks(long nextUpdataTicks) {
            this.nextUpdataTicks = nextUpdataTicks;
        }

        public MutableComponent getName() {
            return Component.translatable(this.name, this.nameArgs);
        }

        public Time newTime(){
            return new Time(this.ticks, this.name, this.nameArgs);
        }

        public interface ChangeListener {
            void changed(IChartsDataHandler handler, int id, IChartsDataHandler.Data data);
        }
    }
}
