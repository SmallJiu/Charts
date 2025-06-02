package cat.jiu.charts.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class ChartKey {
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    public ChartKey(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChartKey other = (ChartKey) o;
        return Objects.equals(dimension, other.dimension) && Objects.equals(pos, other.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension.registry(), pos);
    }
}
