package com.petlife.service;



import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.model.Pet;
import com.petlife.repository.PetRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetService {
	
	
	private final PetRepository petRepository;
	
	//查會員所有寵物
	public List<Pet> getPetsByMemberId(Integer memberId){
		
		return petRepository.findByMemberIdAndStatus(memberId, "active");
	}
	
	//新增寵物
	public Pet addPet(Pet pet,MultipartFile file) {
		
		pet.setStatus("active");
		
		if(file != null && !file.isEmpty()) {
			try {
				
				String uploadDir = "C:/Petlife2.0/uploads/images/pet/";
				
				Files.createDirectories(Paths.get(uploadDir));
				
				String fileName = pet.getMemberId()+ "_" + System.currentTimeMillis()
								 + "_" + file.getOriginalFilename();
				
				Path filePath = Paths.get(uploadDir).resolve(fileName);
				Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
				
				pet.setPetPhoto("/images/pet/" + fileName);
				
				
			} catch (Exception e) {
				throw new RuntimeException("寵物圖片上傳失敗:" + e.getMessage());
			}
		}else {
			pet.setPetPhoto("/images/pet/default_pet.jpg");
		}
			
			
		return petRepository.save(pet);
	}
	
	//修改寵物
	public Pet updatePet(Integer petId, Pet req ,MultipartFile file) {
		
		Pet pet = petRepository.findById(petId)
				.orElseThrow(() -> new IllegalArgumentException("寵物不存在"));
		
		pet.setPetName(req.getPetName());
		pet.setBreed(req.getBreed());
		pet.setSpecies(req.getSpecies());
		pet.setAge(req.getAge());
		pet.setWeight(req.getWeight());
		pet.setMedicalHistory(req.getMedicalHistory());
		
		
		if(file != null && !file.isEmpty()) {
			
			try {
				//刪除舊圖(不是預覽圖才刪)
				if(pet.getPetPhoto() != null && !pet.getPetPhoto().equals("/images/pet/default_pet.jpg")) {
					
					String oldFileName = Paths.get(pet.getPetPhoto())
											  .getFileName()
											  .toString();
					Path oldPath = Paths.get("C://Petlife2.0/uploads/images/pet/");
					
					Files.deleteIfExists(oldPath);
				}
				
				String uploadDir = "C:/Petlife2.0/uploads/images/pet/";
				
				Files.createDirectories(Paths.get(uploadDir));
				
				String fileName = pet.getMemberId()+"_"+System.currentTimeMillis()+"_"+file.getOriginalFilename();
				
				Path filePath = Paths.get(uploadDir).resolve(fileName);
				
				Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
				
				pet.setPetPhoto("/images/pet/" + fileName);
				
			} catch (Exception e) {
				throw new RuntimeException("寵物圖片更新失敗: " + e.getMessage());
			}
		}
		
		
		return petRepository.save(pet);
		
	}
	
	//軟刪除寵物(會員端)
	@Transactional
	public void softDeletePet(Integer petId) {
		Pet pet = petRepository.findById(petId)
				.orElseThrow(() -> new IllegalArgumentException("寵物不存在"));
		
		pet.setStatus("delete");
		petRepository.save(pet);
	}
	
	
	
	
	
	
	
}
