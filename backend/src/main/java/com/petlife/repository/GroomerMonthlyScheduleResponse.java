package com.petlife.repository;

import java.util.List;

public record GroomerMonthlyScheduleResponse(Integer groomerId, String yearMonth,
        List<GroomerMonthlyScheduleDayResponse> days) {
}
