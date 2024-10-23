package org.example;

import com.renomad.minum.database.DbData;

import java.time.LocalTime;
import java.util.Objects;

import static com.renomad.minum.utils.SerializationUtils.deserializeHelper;
import static com.renomad.minum.utils.SerializationUtils.serializeHelper;

public class Schedule extends DbData<Schedule> {

    public static Schedule EMPTY = new Schedule(0, LocalTime.MIN);
    private long index;
    private final LocalTime time;

    public Schedule(long index, LocalTime time) {
        super();

        this.index = index;
        this.time = time;
    }

    @Override
    protected String serialize() {
        return serializeHelper(index, time);
    }

    @Override
    protected Schedule deserialize(String serializedText) {
        final var tokens = deserializeHelper(serializedText);

        return new Schedule(
                Long.parseLong(tokens.get(0)),
                LocalTime.parse(tokens.get(1)));
    }

    @Override
    protected long getIndex() {
        return index;
    }

    @Override
    protected void setIndex(long index) {
        this.index = index;
    }

    public LocalTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return index == schedule.index && Objects.equals(time, schedule.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, time);
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "index=" + index +
                ", time=" + time +
                '}';
    }
}
