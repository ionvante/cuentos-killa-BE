package com.forjix.cuentoskilla.model.DTOs;

import java.time.LocalDateTime;

public class TimeCount {
    private LocalDateTime time;
    private long count;

    public TimeCount(LocalDateTime time, long count) {
        this.time = time;
        this.count = count;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
