package com.petlife.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.model.Pet;
import com.petlife.service.AdminPetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/pets")
@RequiredArgsConstructor
public class AdminPetController {

	private final AdminPetService adminPetService;
	
	
	//分頁查詢
	@GetMapping
	public ResponseEntity<?> getPets(@RequestParam(defaultValue = "0") int page,
									@RequestParam(defaultValue = "10") int size,
									@RequestParam(required = false) String searchType,
									@RequestParam(required = false) String keyword){
		
		return ResponseEntity.ok(adminPetService.getPets(page, size, searchType, keyword));	
			
	}
	
	//新增寵物
	@PostMapping( consumes = "multipart/form-data")
	public ResponseEntity<?> addPet(@RequestParam Integer memberId,
            							@RequestParam String petName,
            							@RequestParam(required = false) String species,
            							@RequestParam(required = false) String breed,
            							@RequestParam(required = false) Integer age,           						
            							@RequestParam(required = false) Double weight,
            							@RequestParam(required = false) String medicalHistory,
            							@RequestPart(required = false)  MultipartFile file){
		
		Pet pet = new Pet();
		
		pet.setPetName(petName);
	    pet.setSpecies(species);
	    pet.setBreed(breed);
	    pet.setAge(age);
	    pet.setWeight(weight);
	    pet.setMedicalHistory(medicalHistory);
		
		return ResponseEntity.ok(adminPetService.addPet(memberId, pet, file));
	    
	}
	//修改寵物
	@PutMapping(value = "/{petId}", consumes = "multipart/form-data")
	public ResponseEntity<?> updatePet(@PathVariable Integer petId,
									  @RequestParam(required = false)Integer memberId,
									  @RequestParam String petName,
									  @RequestParam(required = false)String species,
									  @RequestParam(required = false)String breed,
									  @RequestParam(required = false)Integer age,
									  @RequestParam(required = false)Double weight,
									  @RequestParam(required = false)String medicalHistory,
									  @RequestParam(required = false)String status,
									  @RequestPart(required = false)MultipartFile file){

	    Pet pet = new Pet();

	    pet.setPetName(petName);
	    pet.setSpecies(species);
	    pet.setBreed(breed);
	    pet.setAge(age);
	    pet.setWeight(weight);
	    pet.setMedicalHistory(medicalHistory);
	    pet.setStatus(status);

	    return ResponseEntity.ok(adminPetService.updatePet(petId,memberId,pet,file));
	}
	
	
	// 修改狀態
    @PutMapping("/{petId}/status")
    public ResponseEntity<?> updatePetStatus(
            @PathVariable Integer petId,
            @RequestBody Map<String, String> body
    ) {

        adminPetService.updatePetStatus(
                petId,
                body.get("status")
        );

        return ResponseEntity.ok("狀態更新成功");
    }
	
	
}
