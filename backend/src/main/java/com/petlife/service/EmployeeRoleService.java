package com.petlife.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Employee;
import com.petlife.model.EmployeeRole;
import com.petlife.model.EmployeeRoleId;
import com.petlife.model.Role;
import com.petlife.repository.EmployeeRoleRepository;

@Service
public class EmployeeRoleService {
		
	@Autowired
	private EmployeeRoleRepository employeeRoleRepository;
	
	 // 分派角色
    public EmployeeRole assignRole(Employee employee, Role role) {
        EmployeeRoleId id = new EmployeeRoleId(employee.getEmpId(), role.getRoleId());
        EmployeeRole employeeRole = new EmployeeRole();
        employeeRole.setId(id);
        employeeRole.setEmployee(employee);
        employeeRole.setRole(role);
        employeeRole.setAssignedAt(new Timestamp(System.currentTimeMillis()));
        return employeeRoleRepository.save(employeeRole);
    }

    // 移除角色
    public void removeRole(Employee employee, Role role) {
        EmployeeRoleId id = new EmployeeRoleId(employee.getEmpId(), role.getRoleId());
        employeeRoleRepository.deleteById(id);
    }

    // 查詢某員工的所有角色
    public List<Role> getRolesByEmployee(Integer empId) {
        return employeeRoleRepository.findAll().stream()
                .filter(er -> er.getEmployee().getEmpId().equals(empId))
                .map(EmployeeRole::getRole)
                .toList();
    }

    // 查詢所有員工角色關聯
    public List<EmployeeRole> getAllEmployeeRoles() {
        return employeeRoleRepository.findAll();
    }
	
	
	
	
	
	
}
