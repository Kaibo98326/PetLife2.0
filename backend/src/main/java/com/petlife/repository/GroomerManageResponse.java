package com.petlife.repository;

import java.util.List;

public record GroomerManageResponse(Integer groomerId, String displayName, String intro, Integer seniorityYears,
        Boolean isBookable, List<GroomerServiceResponse> services) {
}
