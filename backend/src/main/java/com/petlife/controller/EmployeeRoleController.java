package com.petlife.controller;

import com.petlife.model.Employee;
import com.petlife.model.Role;
import com.petlife.model.EmployeeRole;
import com.petlife.model.EmployeeRoleId;
import com.petlife.repository.EmployeeRepository;
import com.petlife.repository.RoleRepository;
import com.petlife.service.EmployeeRoleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/api/employeeRole")
public class EmployeeRoleController {

    @Autowired
    private EmployeeRoleService employeeRoleService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    // 取得某員工的所有角色
    @GetMapping("/{empId}/roles")
    public ResponseEntity<List<Role>> getRolesByEmployee(@PathVariable Integer empId) {
        List<Role> roles = employeeRoleService.getRolesByEmployee(empId);
        return ResponseEntity.ok(roles);
    }

    // 分派角色給員工
    @PostMapping("/assign")
    public ResponseEntity<EmployeeRole> assignRole(@RequestParam Integer empId, @RequestParam Integer roleId) {
        Employee employee = employeeRepository.findById(empId).orElseThrow();
        Role role = roleRepository.findById(roleId).orElseThrow();

        EmployeeRole assigned = employeeRoleService.assignRole(employee, role);
        return ResponseEntity.ok(assigned);
    }

    // 移除員工的角色
    @DeleteMapping("/remove")
    public ResponseEntity<String> removeRole(@RequestParam Integer empId, @RequestParam Integer roleId) {
        Employee employee = employeeRepository.findById(empId).orElseThrow();
        Role role = roleRepository.findById(roleId).orElseThrow();

        employeeRoleService.removeRole(employee, role);
        return ResponseEntity.ok("角色已移除");
    }

    // 查詢所有員工角色關聯
    @GetMapping("/all")
    public ResponseEntity<List<EmployeeRole>> getAllEmployeeRoles() {
        return ResponseEntity.ok(employeeRoleService.getAllEmployeeRoles());
    }
}
