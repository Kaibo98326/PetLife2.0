package com.petlife.repository;

import lombok.Data;

@Data
public class PetRequest {
	
	private Integer memberId;

    private String petName;

    private String breed;

    private String species;

    private Integer age;

    private Double weight;

    private String medicalHistory;
}
