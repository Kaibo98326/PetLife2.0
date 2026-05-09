package com.petlife.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Role;
import com.petlife.repository.EmployeeRoleRepository;
import com.petlife.repository.RoleRepository;

@Service
public class AdminRoleService {
	@Autowired
	private RoleRepository roleRepository ;
	
	@Autowired
	private EmployeeRoleRepository employeeRoleRepository;
	
	//查全部角色
	public List<Role> getAllRoles(){
		
		return roleRepository.findAll();
	}
	//新增角色
	public Role addRole(Role role) {
		
		Role exist = roleRepository.findByRoleName(role.getRoleName());
		
		if(exist != null) {
			throw new RuntimeException("角色已存在");
		}
		
		return roleRepository.save(role);
		
	}
	
	//修改角色
	public Role updateRole(Integer roleId,Role request) {
		
		Role role = roleRepository.findById(roleId).orElseThrow(()-> new RuntimeException("找不到角色"));
		
		role.setRoleName(request.getRoleName());
		
		role.setDescription(request.getDescription());
		
		return roleRepository.save(role);
		
	}
	
	//刪除角色
	public void deleteRole(Integer roleId) {
		
		Role role = roleRepository.findById(roleId).
				orElseThrow(() -> new RuntimeException("找不到角色"));
		
		
		boolean isUsed = employeeRoleRepository.existsByRole(role);
		
		if(isUsed) {
			throw new RuntimeException("此類角色已有員工綁定，無法刪除");
		}
		
		
		roleRepository.deleteById(roleId);
		
	}
	
	
	
}
