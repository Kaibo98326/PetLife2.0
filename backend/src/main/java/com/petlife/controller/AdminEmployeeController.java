package com.petlife.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.model.Employee;
import com.petlife.repository.EmployeeRoleUpdateRequest;
import com.petlife.repository.UpdateEmployeePasswordRequest;
import com.petlife.repository.UpdateEmployeeRequset;
import com.petlife.service.AdminEmployeeService;

@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {
	
	@Autowired
	private AdminEmployeeService adminEmployeeService;
	
	@GetMapping
	public ResponseEntity<?> getEmployees(@RequestParam(defaultValue ="0") int page,
										 @RequestParam(defaultValue = "10") int size,
										 @RequestParam(required = false) String searchType,
										 @RequestParam(required = false) String keyword ){
		
		return ResponseEntity.ok(adminEmployeeService.getEmployees(page,size,searchType,keyword));
	}
	
	@PostMapping
	public ResponseEntity<?> addEmployee(@RequestBody Employee emp){
		return ResponseEntity.ok(adminEmployeeService.addEmployee(emp));
	}
	
	@PutMapping("/{empId}")
	public ResponseEntity<?> updateEmployee(@PathVariable Integer empId,
											@RequestBody UpdateEmployeeRequset request){
		return ResponseEntity.ok(adminEmployeeService.updateEmployee(empId, request));
	}
	@PutMapping("/{empId}/password")
	public ResponseEntity<?> updateEmployeePassword(@PathVariable Integer empId,
													@RequestBody UpdateEmployeePasswordRequest request){
		adminEmployeeService.updateEmployeePassword(empId,request.getNewPassword());
		
		return ResponseEntity.ok("修改密碼成功");
	}
	
	@PutMapping("/{empId}/status")
	public ResponseEntity<?> updateEmployeeStatus(@PathVariable Integer empId,
												  @RequestBody Map<String ,String> body){
		adminEmployeeService.updateEmployeeStatus(empId, body.get("status"));
		
		return ResponseEntity.ok("狀態更新成功");
	}
	
	@GetMapping("/{empId}/roles")
	public ResponseEntity<?> getEmployeeRoleIds(@PathVariable Integer empId){
		
		
		return ResponseEntity.ok(adminEmployeeService.getEmployeeRoleIds(empId));
		
	}
	
	@PutMapping("/{empId}/roles")
	public ResponseEntity<?> updateEmployeeRoles(@PathVariable Integer empId,
												@RequestBody EmployeeRoleUpdateRequest request){
		adminEmployeeService.updateEmployeeRoles(empId, request.getRoleIds());
		
		
		return ResponseEntity.ok("員工角色更新");
	}
	
	
}
