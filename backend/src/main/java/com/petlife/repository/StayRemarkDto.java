package com.petlife.repository;

import java.util.List;
import lombok.Data;

@Data
// 專門記錄 客人回傳的備註 與 多隻寵物時的寵物資訊
public class StayRemarkDto {
	private String customerNote;
	
	 private List<PetInfoDto> pets;
	 
	 	@Data
	 	// 因為只有 StayRemarkDto 會用到此Dto 
	 	// 所以寫在裡面 並用 static 宣告 讓他在IDE run之前就執行
	    public static class PetInfoDto {
	        private Integer petId;
	        private String petName;
	        private String species;
	        private String breed;
	    }
}
