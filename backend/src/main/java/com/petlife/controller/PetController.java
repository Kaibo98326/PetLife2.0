package com.petlife.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.model.Pet;
import com.petlife.service.PetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {
	
	private final PetService petService;
	
	//會員端查詢寵物
	@GetMapping("/member/{memberId}")
	public ResponseEntity<List<Pet>> getPetByMemberId(@PathVariable Integer memberId){
		
		return ResponseEntity.ok(petService.getPetsByMemberId(memberId));
	}
	
	//會員端新增寵物
	@PostMapping
	public ResponseEntity<Pet> addPet(@ModelAttribute Pet pet,
									 @RequestParam(value = "file",required = false) MultipartFile file){
		Pet saved = petService.addPet(pet,file);
		
		return ResponseEntity.ok(saved);
	}
	
	//會員端修改寵物
	@PutMapping("/{petId}")
	public ResponseEntity<Pet> updatePet(@PathVariable Integer petId,@ModelAttribute Pet pet,
	        								@RequestParam(value = "file", required = false)MultipartFile file){
		return ResponseEntity.ok(petService.updatePet(petId, pet, file));
	}
	
	//會員端軟刪除寵物
	@DeleteMapping("/{petId}")
	public ResponseEntity<String> softDeletePet(@PathVariable Integer petId){
		
		petService.softDeletePet(petId);
		
		return ResponseEntity.ok("寵物已刪除");
	}
	
	
	
}
