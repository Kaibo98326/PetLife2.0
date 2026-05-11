package com.petlife.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.model.Member;
import com.petlife.model.Pet;
import com.petlife.repository.AdminPetListDto;
import com.petlife.repository.MemberRepository;
import com.petlife.repository.PetRepository;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPetService {
	
	
	private final PetRepository petRepository;
	private final MemberRepository memberRepository;
	
	public Page<AdminPetListDto> getPets(int page , int size , String searchType , String keyword){
		
		Pageable pageable = PageRequest.of(page,size);
		
		Page<Pet> result;
		
		if(searchType == null || searchType.isBlank() || keyword == null || keyword.isBlank()) {
			
			result =petRepository.findAll(pageable);
		}else {
			switch(searchType) {
			    case "petName":
			    			result = petRepository.findByPetNameContaining(keyword, pageable);
			    			break;
			    case "species":
			    			result = petRepository.findBySpecies(keyword, pageable);
			    			break;
			    case "status":
			    			result = petRepository.findByStatus(keyword, pageable);
			    			break;
			    case	 "memberId":
			    			result = petRepository.findByMember_MemberId(Integer.valueOf(keyword), pageable);
			    			break;
			    	default:
			    		result = petRepository.findAll(pageable);
			    		break;
			}
		}
		
		return result.map(this::toDto);
		
	}
	
	public Pet addPet(Integer memberId, Pet pet , MultipartFile file) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new RuntimeException("會員不存在"));
		
		pet.setMember(member);
		pet.setStatus("active");
		pet.setPetPhoto(savePetImage(memberId , file , null));
		
		
		return petRepository.save(pet);
		
	}
	public Pet updatePet(Integer petId, Integer memberId, Pet req, MultipartFile file) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("寵物不存在"));

        if (memberId != null) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("會員不存在"));
            pet.setMember(member);
        }
        

        pet.setPetName(req.getPetName());
        pet.setSpecies(req.getSpecies());
        pet.setBreed(req.getBreed());
        pet.setAge(req.getAge());
        pet.setWeight(req.getWeight());
        pet.setMedicalHistory(req.getMedicalHistory());
        pet.setStatus(req.getStatus());

        if (file != null && !file.isEmpty()) {
            Integer ownerId = pet.getMember().getMemberId();
            pet.setPetPhoto(savePetImage(ownerId, file, pet.getPetPhoto()));
        }

        return petRepository.save(pet);
    }
	
	public void updatePetStatus(Integer petId, String status) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("寵物不存在"));

        pet.setStatus(status);
        petRepository.save(pet);
    }
	private String savePetImage(Integer memberId, MultipartFile file, String oldPhoto) {
        try {
            String uploadDir = "C:/Petlife2.0/uploads/images/pet/";
            Files.createDirectories(Paths.get(uploadDir));

            if (oldPhoto != null && !oldPhoto.equals("/images/pet/default_pet.jpg")) {
                String oldFileName = Paths.get(oldPhoto).getFileName().toString();
                Files.deleteIfExists(Paths.get(uploadDir).resolve(oldFileName));
            }

            if (file == null || file.isEmpty()) {
                return "/images/pet/default_pet.jpg";
            }

            String fileName = memberId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir).resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/images/pet/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("寵物圖片處理失敗：" + e.getMessage());
        }
    }
	
	private AdminPetListDto toDto(Pet pet) {
		
		AdminPetListDto  dto = new AdminPetListDto();
		
		dto.setPetId(pet.getPetId());
        dto.setPetName(pet.getPetName());
        dto.setSpecies(pet.getSpecies());
        dto.setBreed(pet.getBreed());
        dto.setAge(pet.getAge());
        dto.setWeight(pet.getWeight());
        dto.setPetPhoto(pet.getPetPhoto());
        dto.setStatus(pet.getStatus());
        
        if (pet.getMember() != null) {
            dto.setMemberId(pet.getMember().getMemberId());
            dto.setMemberName(pet.getMember().getMemberName());
        }
		
		return dto;
	}
	
}
