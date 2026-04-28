package com.petlife.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petlife.model.Employee;
import com.petlife.model.EmployeeRole;
import com.petlife.model.EmployeeRoleId;
import com.petlife.model.Role;

public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, EmployeeRoleId> {

	// 用 Employee 物件查角色
    @Query("SELECT er.role FROM EmployeeRole er WHERE er.employee = :employee")
    List<Role> findRolesByEmployee(@Param("employee") Employee employee);

    // 或者用 empId 查角色
    @Query("SELECT er.role FROM EmployeeRole er WHERE er.employee.empId = :empId")
    List<Role> findRolesByEmployeeId(@Param("empId") Integer empId);
	
}
