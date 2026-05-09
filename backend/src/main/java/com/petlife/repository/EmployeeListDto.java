package com.petlife.repository;

import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

@Data
public class EmployeeListDto {
	
	private Integer empId;
	
	private String username;
	
	private String empName;
	private String empPhone;
	private String empAddress;
	private String emergencyContact;
	private String emergencyPhone;
	private String status;
	private Timestamp lastLoginAt;
	private List<String> roles;
	
	
}
