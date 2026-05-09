package com.petlife.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.model.Role;
import com.petlife.service.AdminRoleService;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {
	
	 	@Autowired
	    private AdminRoleService adminRoleService;

	    // 查全部
	    @GetMapping
	    public ResponseEntity<?> getAllRoles() {

	        return ResponseEntity.ok(adminRoleService.getAllRoles());
	    }

	    // 新增
	    @PostMapping
	    public ResponseEntity<?> addRole(@RequestBody Role role) {

	        return ResponseEntity.ok(adminRoleService.addRole(role));
	    }

	    // 修改
	    @PutMapping("/{roleId}")
	    public ResponseEntity<?> updateRole(@PathVariable Integer roleId,
	             						   @RequestBody Role role) {

	        return ResponseEntity.ok(adminRoleService.updateRole(roleId,role));
	    }
	    
	    // 刪除
	    @DeleteMapping("/{roleId}")
	    public ResponseEntity<?> deleteRole(
	            @PathVariable Integer roleId
	    ) {

	        adminRoleService.deleteRole(roleId);

	        return ResponseEntity.ok("角色刪除成功");
	    }
	    
}
