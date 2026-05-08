package com.petlife.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Employee;
import com.petlife.model.Role;
import com.petlife.repository.EmployeeListDto;
import com.petlife.repository.EmployeeRepository;
import com.petlife.repository.EmployeeRoleRepository;
import com.petlife.repository.UpdateEmployeeRequset;

@Service
public class AdminEmployeeService {
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private EmployeeRoleRepository employeeRoleRepository;
	
	
	//員工端尋找所有員工
	public List<EmployeeListDto> getAllEmployees(){
		
		return employeeRepository.findAll().stream()
				.map(emp -> {
					EmployeeListDto dto = new EmployeeListDto();
					
					dto.setEmpId(emp.getEmpId());
					dto.setUsername(emp.getUsername());
					dto.setEmpName(emp.getEmpName());
					dto.setEmpPhone(emp.getEmpPhone());
					dto.setEmpAddress(emp.getEmpAddress());
					dto.setEmergencyContact(emp.getEmergencyContact());
					dto.setEmergencyPhone(emp.getEmergencyPhone());
					dto.setStatus(emp.getStatus());
					dto.setLastLoginAt(emp.getLastLoginAt());
					
					List<String> roleNames = employeeRoleRepository.findRolesByEmployee(emp)
											.stream().map(Role::getRoleName).toList();
					
					
					dto.setRoles(roleNames);
					return dto;
				}).toList();
		
	}
	
	//員工端新增員工
	public Employee addEmployee(Employee employee) {
		employee.setPasswordHash(PasswordUtils.hashPassword(employee.getPasswordHash()));
		
		employee.setStatus("active");
		
		return employeeRepository.save(employee);
	}
	
	//員工端更新員工資料
	public Employee updateEmployee(Integer empId , UpdateEmployeeRequset request) {
		Employee emp = employeeRepository.findById(empId)
						.orElseThrow( () -> new RuntimeException("找不到員工") );
		
		emp.setEmpName(request.getEmpName());
		emp.setEmpPhone(request.getEmpPhone());
		emp.setEmpAddress(request.getEmpAddress());
		emp.setEmergencyContact(request.getEmpAddress());
		emp.setEmergencyPhone(request.getEmergencyPhone());
		emp.setStatus(request.getStatus());
		
		return employeeRepository.save(emp);	
	}
	//修改密碼
	public void updateEmployeePassword(
	        Integer empId,
	        String newPassword
	) {

	    Employee emp = employeeRepository.findById(empId)
	            .orElseThrow(() -> new RuntimeException("找不到員工"));

	    emp.setPasswordHash(
	            PasswordUtils.hashPassword(newPassword)
	    );

	    employeeRepository.save(emp);
	}
	//更新員工狀態
	public void updateEmployeeStatus(Integer empId, String status) {

	    Employee emp = employeeRepository.findById(empId)
	            .orElseThrow(() -> new RuntimeException("找不到員工"));

	    emp.setStatus(status);

	    employeeRepository.save(emp);
	}
	
	
	
	
	
	
	
}
