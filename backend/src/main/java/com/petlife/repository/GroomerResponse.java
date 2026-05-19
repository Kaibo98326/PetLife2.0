package com.petlife.repository;

public record GroomerResponse(Integer groomerId, String displayName, String intro, Integer seniorityYears,
        Boolean isBookable) {
}
