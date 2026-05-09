package com.petlife.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Employee;
import com.petlife.model.EmployeeRole;
import com.petlife.model.EmployeeRoleId;
import com.petlife.model.Role;
import com.petlife.repository.EmployeeListDto;
import com.petlife.repository.EmployeeRepository;
import com.petlife.repository.EmployeeRoleRepository;
import com.petlife.repository.RoleRepository;
import com.petlife.repository.UpdateEmployeeRequset;



@Service
public class AdminEmployeeService {
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private EmployeeRoleRepository employeeRoleRepository;
	
	@Autowired
	private RoleRepository roleRepository;
	
	
	//員工端尋找所有員工
	public Page<EmployeeListDto> getEmployees(int page , int size ,  String searchType, String keyword){
		
		Pageable pageable = PageRequest.of(page, size);
		
		Page<Employee> result ;
		
		if(searchType == null || searchType.isBlank()) {
			
			result = employeeRepository.findAll(pageable);
			
		}else {
			
			switch (searchType) {
			  case "empId":
				  	result = employeeRepository.findByEmpId(Integer.valueOf(keyword), pageable);
				  	break;
		      case "empName":
		    	  		result = employeeRepository.findByEmpNameContaining(keyword, pageable);
		    	  		break;
		      case "status" :
		    	  		result = employeeRepository.findByStatus(keyword, pageable);
		    	  		break;
		      case "phoneLast3":
		    	  		result = employeeRepository.findByPhoneLast3(keyword, pageable);
		    	  		break;
		    	  default :
		    	  	 result = employeeRepository.findAll(pageable);
		    	  	 break;
			}
			
		}
		
		return result.map(emp -> {
			EmployeeListDto dto = new EmployeeListDto();
			
			dto.setEmpId(emp.getEmpId());

	        dto.setUsername(emp.getUsername());

	        dto.setEmpName(emp.getEmpName());

	        dto.setEmpPhone(emp.getEmpPhone());

	        dto.setStatus(emp.getStatus());

	        dto.setLastLoginAt(emp.getLastLoginAt());
	        
	        List<String> roles = employeeRoleRepository.findRolesByEmployee(emp)
	        						.stream()
	        						.map(Role::getRoleName)
	        						.toList();
	        
	        dto.setRoles(roles);
	        
	        return dto;
		});
		
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
		emp.setEmergencyContact(request.getEmergencyContact());
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
	
	//查詢員工角色
	public List<Integer> getEmployeeRoleIds(Integer empId){
		
		Employee emp = employeeRepository.findById(empId)
				.orElseThrow(()-> new RuntimeException("找不到員工"));
		
		
		return employeeRoleRepository.findRolesByEmployee(emp)
				.stream()
				.map(Role::getRoleId)
				.toList();
		
	}
	
	//更新員工角色
	@Transactional
	public void updateEmployeeRoles(Integer empId,List<Integer> roleIds) {
		
		Employee emp = employeeRepository.findById(empId)
				.orElseThrow(() -> new RuntimeException("找不到員工"));
		
		//先刪除舊角色
		employeeRoleRepository.deleteByEmployee(emp);
		
		//強制執行刪除，避免複合主鍵重複
		employeeRepository.flush();
		
		//重建角色
		for(Integer roleId : roleIds) {
			Role role = roleRepository.findById(roleId)
					.orElseThrow(() -> new RuntimeException("找不到角色"));
			
			EmployeeRoleId id = new EmployeeRoleId(
			        emp.getEmpId(),
			        role.getRoleId()
			);
			
			EmployeeRole employeeRole = new EmployeeRole();
			employeeRole.setId(id);
			employeeRole.setEmployee(emp);
			employeeRole.setRole(role);
			employeeRole.setAssignedAt(new Timestamp(System.currentTimeMillis()));
			
			
			employeeRoleRepository.save(employeeRole);
			
		}
		
	}
	
	
	
	
	
	
	
}
