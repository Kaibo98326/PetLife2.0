package com.petlife.repository;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petlife.model.Employee;




public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
	
	Optional<Employee> findByUsername(String username);
	
	Page<Employee> findByEmpNameContaining(String empName , Pageable pageable);
	
	
	Page<Employee> findByStatus(String status , Pageable pageable);
	
	
	Page<Employee> findByEmpId(Integer empId , Pageable pageable);
	
	@Query(value =  """
			SELECT * 
			FROM Employee 
			WHERE emp_phone IS NOT NULL
				AND RIGHT(emp_phone, 3) = :last3
			""" ,  
			countQuery = """
					SELECT COUNT(*)
					FROM Employee
					WHERE emp_phone IS NOT NULL
					AND RIGHT(emp_phone,3) = :last3
					""",nativeQuery = true)
	Page<Employee> findByPhoneLast3(@Param("last3") String last3 , Pageable pageable);
	
}
