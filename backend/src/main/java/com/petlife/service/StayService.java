package com.petlife.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.petlife.model.Pet;
import com.petlife.model.Stay;
import com.petlife.model.StayRoom;
import com.petlife.model.StayRoomType;
import com.petlife.repository.CalendarDayDto;
import com.petlife.repository.PetRepository;
import com.petlife.repository.RoomTypeDto;
import com.petlife.repository.StayRemarkDto;
import com.petlife.repository.StayRepository;
import com.petlife.repository.StayRequestDto;
import com.petlife.repository.StayResponseDto;
import com.petlife.repository.StayRoomRepository;
import com.petlife.repository.StayRoomTypeRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class StayService implements IStayService {

	private final StayRoomTypeRepository stayRoomTypeRepository;
	private final StayRoomRepository stayRoomRepository;
	private final StayRepository stayRepository;
	private final PetRepository petRepository;
	

	//計價邏輯
	@Override
	public Double calculatePrice (Integer roomTypeId,LocalDate startDate,LocalDate endDate) {
		
		StayRoomType stayRoomType = stayRoomTypeRepository.findById(roomTypeId).orElseThrow();
		
		long stayDay = ChronoUnit.DAYS.between(startDate, endDate);
	
		Double sumPrice = stayRoomType.getRoomPrice() * stayDay;
	
		return sumPrice;
	}
	
	// 空房查詢
	@Override
	public RoomTypeDto checkAvailability(Integer roomTypeId,LocalDate startDate,LocalDate endDate) {
		
		StayRoomType roomType = stayRoomTypeRepository.findById(roomTypeId).orElseThrow(() -> new RuntimeException("找不到房型") );
	
		int totalRooms = stayRoomRepository.findByStayRoomType_RoomTypeId(roomTypeId).size();
		
		int bookedRooms = stayRepository.findOverlappingStays(roomTypeId, startDate, endDate).size();
		
		int availableCount = totalRooms - bookedRooms;
		
		RoomTypeDto dto = new RoomTypeDto();
		dto.setRoomTypeId(roomTypeId);
		dto.setRoomName(roomType.getRoomName());
		dto.setRoomPrice(roomType.getRoomPrice());
		dto.setRoomDescription(roomType.getRoomDescription());
		dto.setCapacity(roomType.getCapacity());
		dto.setAvailableCount(availableCount);
		
		
		return dto;
	}

	// 建立預約
	@Override
	public StayResponseDto createStay(StayRequestDto  request) {

		// 確認還有空房
		List<StayRoom> availableRooms = stayRoomRepository
	            .findByStayRoomType_RoomTypeIdAndRoomStatus(
	                request.getStayRoomTypeId(), "可預約");
		
		int bookedRooms = stayRepository.findOverlappingStays(request.getStayRoomTypeId(), request.getStayStartDate(), request.getStayEndDate()).size();
		
		if(availableRooms.size() - bookedRooms < 1) {
			throw new RuntimeException("此時段已無空房間");
		}
		
		// 找一間空房分配
		StayRoom availableRoom = availableRooms.get(0);
		
		// 確認寵物存在
		Pet pet = petRepository.findById(request.getPetId()).orElseThrow(() -> new RuntimeException("找不到寵物"));
		
		// 計算價格
		Double sumPrice = calculatePrice(request.getStayRoomTypeId(), request.getStayStartDate(), request.getStayEndDate());
		
		// 計算天數
		long stayDays = ChronoUnit.DAYS.between(request.getStayStartDate(), request.getStayEndDate());
		
		// 建立訂單
		Stay stay = new Stay();
		stay.setPet(pet);
		stay.setStayRoom(availableRoom);
		stay.setStayStartDate(request.getStayStartDate());
	    stay.setStayEndDate(request.getStayEndDate());
	    stay.setPetCount(request.getPetCount());
	    stay.setStayDay((int)stayDays);
	    stay.setSumPrice(sumPrice);
	    stay.setStayStatus("active");
	    
	    // 5/6 新增 --建立備註 JSON--
	    StayRemarkDto remark = new StayRemarkDto();
	    remark.setCustomerNote(request.getCustomerNote());
	    
	    // 組裝寵物清單
	    List<StayRemarkDto.PetInfoDto> petInfoList = new ArrayList<>();
	    
	    // 第一隻寵物
	    StayRemarkDto.PetInfoDto firstPet = new StayRemarkDto.PetInfoDto();
	    firstPet.setPetId(pet.getPetId());
	    firstPet.setPetName(pet.getPetName());
	    firstPet.setSpecies(pet.getSpecies());
	    firstPet.setBreed(pet.getBreed());
	    petInfoList.add(firstPet);
	    
	    // 第2隻以後的寵物
	    if (request.getExtraPetIds() != null && !request.getExtraPetIds().isEmpty()) {
	    	for (Integer extraId : request.getExtraPetIds()) {
	    		Pet extraPet = petRepository.findById(extraId)
	                    .orElseThrow(() -> new RuntimeException("找不到寵物 ID: " + extraId));
	    		 StayRemarkDto.PetInfoDto info = new StayRemarkDto.PetInfoDto();
	    	        info.setPetId(extraPet.getPetId());
	    	        info.setPetName(extraPet.getPetName());
	    	        info.setSpecies(extraPet.getSpecies());
	    	        info.setBreed(extraPet.getBreed());
	    	        petInfoList.add(info);
	    	}
	    }
	    
	    remark.setPets(petInfoList);
	    
	    
	    // 轉成 JSON 字串存入 Stay
	    ObjectMapper mapper = new ObjectMapper();
	    try {
	        stay.setStayRemark(mapper.writeValueAsString(remark));
	    } catch (Exception e) {
	        throw new RuntimeException("備註序列化失敗");
	    }
	    
	    // --  5/6 新增 至此 -- 
		
	    // 存入資料庫
	    Stay saved = stayRepository.save(stay);

	    // 組裝 Dto 並 回傳給 
	    StayResponseDto dto = new StayResponseDto();
		dto.setStayId(saved.getStayId());
		dto.setPetName(pet.getPetName());
	    dto.setStayStartDate(saved.getStayStartDate());
	    dto.setStayEndDate(saved.getStayEndDate());
	    dto.setStayDay(saved.getStayDay());
	    dto.setPetCount(saved.getPetCount());
	    dto.setSumPrice(saved.getSumPrice());
	    dto.setStayStatus(saved.getStayStatus());
	    dto.setRoomTypeName(availableRoom.getStayRoomType().getRoomName());
	    dto.setStayRemark(saved.getStayRemark());
		
		return dto;
	}

	// 取消預約
	@Override
	public void cancelStay(Integer stayId) {
		
		// 查看有無這筆訂單
		Stay stay = stayRepository.findById(stayId).orElseThrow(() -> new RuntimeException("找不到此訂單"));
	
		// 確認狀態是否可以取消(狗都入住怎取消)
		if (stay.getStayStatus().equals("CHECKED_IN")) {
			throw new RuntimeException("已入住的訂單無法取消");
		}
		
		// 更新狀態
		stay.setStayStatus("CANCELLED");
		
		// 4. 存回資料庫
	    stayRepository.save(stay);
	}

	// 根據ID取得會員訂單
	@Override
	public List<StayResponseDto> getMyStays(Integer memberId) {
		
		List<Stay> stays = stayRepository.findByPet_Member_MemberId(memberId);
		
		//Create an empty list to hold the results.
		List<StayResponseDto> result = new ArrayList<>();
		
		for (Stay stay:stays) {
			// Create a new DTO object for each stay record.
			StayResponseDto dto = new StayResponseDto();
			dto.setStayId(stay.getStayId());
			dto.setPetName(stay.getPet().getPetName());
			dto.setRoomTypeName(stay.getStayRoom().getStayRoomType().getRoomName());
			dto.setRoomNo(stay.getStayRoom().getRoomNo());
			dto.setStayStartDate(stay.getStayStartDate());
			dto.setStayEndDate(stay.getStayEndDate());
			dto.setStayDay(stay.getStayDay());
			dto.setPetCount(stay.getPetCount());
			dto.setSumPrice(stay.getSumPrice());
			dto.setStayStatus(stay.getStayStatus());
			dto.setStayRemark(stay.getStayRemark());
			result.add(dto);
		}
		return result;
	}

	// 從前端得到 ? 年 ? 月 回傳 此月每一號可用空房
	@Override
	public List<CalendarDayDto> getCalendar(Integer roomTypeId, int year, int month) {
		
		// 取得這個月的第一天和最後一天
	    LocalDate startOfMonth = LocalDate.of(year, month, 1);
	     	// 自動取得這個月有幾天，不用自己判斷
	    LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
	    
	    // 查這個房型總共幾間可預約的房間
	    int totalRooms = stayRoomRepository
	            .findByStayRoomType_RoomTypeIdAndRoomStatus(roomTypeId, "可預約").size();
		
	    // 建立結果清單
	    List<CalendarDayDto> result = new ArrayList<>();
	    
	    // 逐天計算
	    LocalDate current = startOfMonth;
	    	//從月初逐天跑到月底
	    while (!current.isAfter(endOfMonth)) {
	    	// 查這天有幾筆重疊的預約
	        int bookedRooms = stayRepository
	                .findOverlappingStays(roomTypeId, current, current.plusDays(1)).size();

	        int availableCount = totalRooms - bookedRooms;

	        CalendarDayDto dto = new CalendarDayDto();
	        dto.setStayRoomTypeId(roomTypeId);
	        dto.setDate(current);
	        dto.setAvailableCount(availableCount);
	        dto.setIsAvailable(availableCount > 0);

	        result.add(dto);
	         // 跟迴圈 i++ 同理 
	        current = current.plusDays(1);
	    }
	    return result;
	}

	// 取得所有房型
	@Override
	public List<RoomTypeDto> getAllRoomTypes() {

		List<StayRoomType> roomTypes = stayRoomTypeRepository.findAll();
		
		List<RoomTypeDto> result = new ArrayList<>();
		
		for (StayRoomType roomType : roomTypes) {
			// 查這個房型總共幾間可預約的房間
			int totalRooms = stayRoomRepository.findByStayRoomType_RoomTypeIdAndRoomStatus(roomType.getRoomTypeId(),"可預約").size();
			
			//  存入
			RoomTypeDto dto = new RoomTypeDto();
			dto.setRoomTypeId(roomType.getRoomTypeId());
			dto.setRoomName(roomType.getRoomName());
			dto.setRoomPrice(roomType.getRoomPrice());
			dto.setRoomDescription(roomType.getRoomDescription());
			dto.setCapacity(roomType.getCapacity());
			dto.setAvailableCount(totalRooms);
			
			result.add(dto);
		}
		return result;
	}

	// 取得房型ID
	@Override
	public RoomTypeDto getRoomTypeById(Integer roomTypeId) {
		StayRoomType roomType = stayRoomTypeRepository.findById(roomTypeId)
				.orElseThrow(() -> new RuntimeException("找不到此房型!!"));
		
		int totalRooms = stayRoomRepository
	            .findByStayRoomType_RoomTypeIdAndRoomStatus(roomTypeId, "可預約").size();
		
		 RoomTypeDto dto = new RoomTypeDto();
		    dto.setRoomTypeId(roomType.getRoomTypeId());
		    dto.setRoomName(roomType.getRoomName());
		    dto.setRoomPrice(roomType.getRoomPrice());
		    dto.setRoomDescription(roomType.getRoomDescription());
		    dto.setCapacity(roomType.getCapacity());
		    dto.setAvailableCount(totalRooms);
		return dto;
	}
	
	
	
}
