package com.petlife.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.model.Employee;
import com.petlife.service.EmployeeService;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {
	
	@Autowired
	private EmployeeService employeeService;
	
	//員工登入
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> payload){
		
		String username = payload.get("username");
		String password = payload.get("password");
		
		
		String token = employeeService.login(username, password);
		if(token == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤");
		}
		return ResponseEntity.ok(Map.of("token" ,token));
		
		
	}
	
	// 查詢所有員工
    @GetMapping("/all")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // 查詢單一員工
    @GetMapping("/{empId}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Integer empId) {
        Employee employee = employeeService.getEmployeeById(empId);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(employee);
    }

    // 新增員工
    @PostMapping("/add")
    public ResponseEntity<Employee> addEmployee(@RequestBody Employee employee) {
        Employee saved = employeeService.addEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 更新員工
    @PutMapping("/update")
    public ResponseEntity<Employee> updateEmployee(@RequestBody Employee employee) {
        Employee updated = employeeService.updateEmployee(employee);
        return ResponseEntity.ok(updated);
    }

    // 軟刪除員工
    @DeleteMapping("/{empId}")
    public ResponseEntity<String> softDeleteEmployee(@PathVariable Integer empId) {
        boolean deleted = employeeService.softDeleteEmployee(empId);
        if (deleted) {
            return ResponseEntity.ok("員工已軟刪除");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到員工");
        }
    }
	
}
