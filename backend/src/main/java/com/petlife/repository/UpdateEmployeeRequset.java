package com.petlife.repository;

import lombok.Data;

@Data
public class UpdateEmployeeRequset {
	
	private String empName;
    private String empPhone;
    private String passwordHash;
    private String empAddress;
    private String emergencyContact;
    private String emergencyPhone;
    private String status;
}
