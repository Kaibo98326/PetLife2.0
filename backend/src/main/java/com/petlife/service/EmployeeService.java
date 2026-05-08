package com.petlife.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Employee;
import com.petlife.model.Role;
import com.petlife.repository.EmployeeRepository;
import com.petlife.repository.EmployeeRoleRepository;

@Service
public class EmployeeService {
	
	@Autowired
	private EmployeeRepository employeeRepos;
	
	@Autowired
    private EmployeeRoleRepository employeeRoleRepository;

    @Autowired
    private EmpJwtUtil empJwtUtil;

    // 員工登入並生成 JWT
    public String login(String username, String password) {
    	
        Optional<Employee> empOpt = employeeRepos.findByUsername(username);
        
        
        if (empOpt.isEmpty()) {
        	throw new IllegalArgumentException("帳號或密碼錯誤");
        }
        Employee emp = empOpt.get();
        
        if(!PasswordUtils.checkPassword(password, emp.getPasswordHash())) {
        	throw new IllegalArgumentException("帳號或密碼錯誤");
        }
        
        if("disable".equals(emp.getStatus())) {
        	throw new IllegalArgumentException("此員工帳號已停權");
        }
        if("delete".equals(emp.getStatus())) {
        	throw new IllegalArgumentException("此員工帳號已刪除");
        }
        if(!"active".equals(emp.getStatus())) {
        	throw new IllegalArgumentException("此員工帳號狀態異常");
        }
        emp.setLastLoginAt(new Timestamp(System.currentTimeMillis()));
        employeeRepos.save(emp);
        
        List<Role> roles = employeeRoleRepository.findRolesByEmployee(emp);
        
        return empJwtUtil.generateToken(emp.getEmpId(), emp.getUsername(), emp.getEmpName(), roles);
    }

    // 查詢所有員工
    public List<Employee> getAllEmployees() {
        return employeeRepos.findAll();
    }

    // 查詢單一員工
    public Employee getEmployeeById(Integer empId) {
        return employeeRepos.findById(empId).orElse(null);
    }

    // 新增員工
    public Employee addEmployee(Employee employee) {
    	
    	employee.setPasswordHash(PasswordUtils.hashPassword(employee.getPasswordHash()));
    	employee.setStatus("active");
    	
        return employeeRepos.save(employee);
    }

    // 更新員工
    public Employee updateEmployee(Employee employee) {
        return employeeRepos.save(employee);
    }

    // 軟刪除員工
    public boolean softDeleteEmployee(Integer empId) {
        return employeeRepos.findById(empId).map(emp -> {
            emp.setStatus("delete"); // ✅ 改成軟刪除
            employeeRepos.save(emp);
            return true;
        }).orElse(false);
    }
	
	
}
