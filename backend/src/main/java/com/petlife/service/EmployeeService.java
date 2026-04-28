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
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            // 假設你有 PasswordUtils 來驗證雜湊密碼
            if (PasswordUtils.checkPassword(password, emp.getPasswordHash())) {
                // 更新最後登入時間
                emp.setLastLoginAt(new Timestamp(System.currentTimeMillis()));
                employeeRepos.save(emp);

                // 查詢員工角色清單
                List<Role> roles = employeeRoleRepository.findRolesByEmployee(emp);

                // 生成 JWT，包含 empId, username, empName, roles
                return empJwtUtil.generateToken(emp.getEmpId(), emp.getUsername(), emp.getEmpName(), roles);
            }
        }
        return null;
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
        return employeeRepos.save(employee);
    }

    // 更新員工
    public Employee updateEmployee(Employee employee) {
        return employeeRepos.save(employee);
    }

    // 軟刪除員工
    public boolean softDeleteEmployee(Integer empId) {
        return employeeRepos.findById(empId).map(emp -> {
            emp.setStatus("deleted"); // ✅ 改成軟刪除
            employeeRepos.save(emp);
            return true;
        }).orElse(false);
    }
	
	
}
