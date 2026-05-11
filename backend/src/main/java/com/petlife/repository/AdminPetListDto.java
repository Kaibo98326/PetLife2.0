package com.petlife.repository;

import lombok.Data;

@Data
public class AdminPetListDto {
	
	 	private Integer petId;

	    private Integer memberId;

	    private String memberName;

	    private String petName;

	    private String species;

	    private String breed;

	    private Integer age;

	    private Double weight;

	    private String petPhoto;

	    private String status;
	    
	    private String medicalHistory;
	
	
}
