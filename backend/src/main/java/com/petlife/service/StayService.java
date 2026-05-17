package com.petlife.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.petlife.model.Pet;
import com.petlife.model.Stay;
import com.petlife.model.StayPayment;
import com.petlife.model.StayRoom;
import com.petlife.model.StayRoomType;
import com.petlife.repository.CalendarDayDto;
import com.petlife.repository.PetRepository;
import com.petlife.repository.RoomStatusDto;
import com.petlife.repository.RoomTypeDto;
import com.petlife.repository.StayPaymentRepository;
import com.petlife.repository.StayPaymentResponseDto;
import com.petlife.repository.StayRemarkDto;
import com.petlife.repository.StayRepository;
import com.petlife.repository.StayRequestDto;
import com.petlife.repository.StayResponseDto;
import com.petlife.repository.StayRoomDto;
import com.petlife.repository.StayRoomRepository;
import com.petlife.repository.StayRoomTypeRepository;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
@Service
@RequiredArgsConstructor
public class StayService implements IStayService {

	private final StayRoomTypeRepository stayRoomTypeRepository;
	private final StayPaymentRepository stayPaymentRepository;
	private final StayRoomRepository stayRoomRepository;
	private final StayRepository stayRepository;
	private final PetRepository petRepository;
	private final LinePayService linePayService;
	

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
	
		int totalRooms = stayRoomRepository
		        .findByStayRoomType_RoomTypeIdAndRoomStatus(roomTypeId, "可預約").size();
		
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
		public StayResponseDto createStay(StayRequestDto request) {
	
	    // 確認還有空房
	    List<StayRoom> availableRooms = stayRoomRepository
	            .findByStayRoomType_RoomTypeIdAndRoomStatus(
	                request.getStayRoomTypeId(), "可預約");
	
	    // 查這段日期已被預約的房間 ID
	    List<Stay> overlappingStays = stayRepository
	            .findOverlappingStays(request.getStayRoomTypeId(),
	                    request.getStayStartDate(),
	                    request.getStayEndDate());
	    
	
	    List<Integer> bookedRoomIds = overlappingStays.stream()
	            .map(s -> s.getStayRoom().getRoomId())
	            .collect(Collectors.toList());
	
	    // 從可預約房間中排除已被預約的，找第一間真正空的
	    StayRoom availableRoom = availableRooms.stream()
	            .filter(r -> !bookedRoomIds.contains(r.getRoomId()))
	            .findFirst()
	            .orElseThrow(() -> new RuntimeException("此時段已無空房間"));
	
	    // 確認寵物存在
	    Pet pet = petRepository.findById(request.getPetId())
	            .orElseThrow(() -> new RuntimeException("找不到寵物"));
	
	    // 計算價格
	    Double sumPrice = calculatePrice(request.getStayRoomTypeId(),
	            request.getStayStartDate(),
	            request.getStayEndDate());
	
	    // 計算天數
	    long stayDays = ChronoUnit.DAYS.between(
	            request.getStayStartDate(),
	            request.getStayEndDate());
	
	    // 建立訂單
	    Stay stay = new Stay();
	    stay.setPet(pet);
	    stay.setStayRoom(availableRoom);
	    stay.setStayRoomType(availableRoom.getStayRoomType());
	    stay.setStayStartDate(request.getStayStartDate());
	    stay.setStayEndDate(request.getStayEndDate());
	    stay.setPetCount(request.getPetCount());
	    stay.setStayDay((int) stayDays);
	    stay.setSumPrice(sumPrice);
	    stay.setStayStatus("active");
	
	    // 建立備註 JSON
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
	
	    // 存入資料庫
	    Stay saved = stayRepository.save(stay);
	
	    // 組裝 DTO 並回傳
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
	    dto.setRoomNo(availableRoom.getRoomNo());
	
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
		
		 StayPayment payment = stayPaymentRepository.findByStay_StayId(stayId).orElse(null);
		    if (payment != null && payment.getPaymentStatus().equals("SUCCESS")) {
		        throw new RuntimeException("已付款訂單無法自行取消，請聯繫客服");
		    }
		
		// 更新狀態
		stay.setStayStatus("CANCELLED");
		
		// 4. 存回資料庫
	    stayRepository.save(stay);
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

	// 建立訂單 與 line pay
	@Override
	public StayPaymentResponseDto  createStayWithPayment(StayRequestDto request) {
		
		 // 1. 建立訂單（呼叫原本的 createStay）
	    StayResponseDto stayResponse = createStay(request);

	    // 2. 產生商家交易編號
	    String merchantTradeNo = "STAY" + stayResponse.getStayId() +
	                             System.currentTimeMillis();

	    // 3. 建立付款紀錄
	    StayPayment payment = new StayPayment();
	    payment.setStay(stayRepository.findById(stayResponse.getStayId()).orElseThrow());
	    payment.setPaymentMethod("LINE_PAY");
	    payment.setPaymentStatus("PENDING");
	    payment.setAmount(stayResponse.getSumPrice());
	    payment.setMerchantTradeNo(merchantTradeNo);
	    payment.setCreatedAt(LocalDateTime.now());
	    stayPaymentRepository.save(payment);

	    // 4. 呼叫 LINE Pay 拿付款網址
	    String paymentUrl = linePayService.requestPayment(
	        merchantTradeNo,
	        stayResponse.getSumPrice().intValue(),
	        "PetLife 寵物住宿 - " + stayResponse.getRoomTypeName()
	    );

	    // 5. 回傳結果
	    StayPaymentResponseDto result = new StayPaymentResponseDto();
	    result.setStayId(stayResponse.getStayId());
	    result.setPaymentUrl(paymentUrl);

	    return result;
	}

	// 確認預約
	@Override
	public String confirmPayment(String merchantTradeNo, String transactionId) {
		// 1. 查付款紀錄
	    StayPayment payment = stayPaymentRepository
	            .findByMerchantTradeNo(merchantTradeNo)
	            .orElseThrow(() -> new RuntimeException("找不到付款紀錄"));

	    // 2. 打 LINE Pay Confirm API
	    boolean success = linePayService.confirmPayment(
	        transactionId,
	        payment.getAmount().intValue()
	    );

	    // 3. 更新付款狀態
	    if (success) {
	        payment.setPaymentStatus("SUCCESS");
	        payment.setTradeNo(transactionId);
	        payment.setPaidAt(LocalDateTime.now());
	        stayPaymentRepository.save(payment);

	        // ✅ 付款成功後同時更新訂單狀態
	        Stay stay = payment.getStay();
	        stay.setStayStatus("CONFIRMED");
	        stayRepository.save(stay);

	        return "SUCCESS";
	    } else {
	        payment.setPaymentStatus("FAILED");
	        stayPaymentRepository.save(payment);
	        return "FAILED";
	    }
	}
	
	

	//付款紀錄
	@Override
	public Integer getStayIdByMerchantTradeNo(String merchantTradeNo) {
	    StayPayment payment = stayPaymentRepository
	            .findByMerchantTradeNo(merchantTradeNo)
	            .orElseThrow(() -> new RuntimeException("找不到付款紀錄"));
	    return payment.getStay().getStayId();
	}

	

	// 修改訂單狀態
	@Override
	public void updateStayStatus(Integer stayId, String status) {
		Stay stay = stayRepository.findById(stayId)
	            .orElseThrow(() -> new RuntimeException("找不到此訂單"));

	    stay.setStayStatus(status);
	    stayRepository.save(stay);		
	}
	
	// 查所有房間
	@Override
	public List<StayRoomDto> getAllRooms() {

	    List<StayRoom> rooms = stayRoomRepository.findAll();
	    List<StayRoomDto> result = new ArrayList<>();

	    for (StayRoom room : rooms) {
	        StayRoomDto dto = new StayRoomDto();
	        dto.setRoomId(room.getRoomId());
	        dto.setRoomNo(room.getRoomNo());
	        dto.setRoomStatus(room.getRoomStatus());
	        dto.setRoomTypeId(room.getStayRoomType().getRoomTypeId());
	        dto.setRoomTypeName(room.getStayRoomType().getRoomName());
	        result.add(dto);
	    }
	    return result;
	}

	// 修改房間狀態
	@Override
	public void updateRoomStatus(Integer roomId, String status) {
	    StayRoom room = stayRoomRepository.findById(roomId)
	            .orElseThrow(() -> new RuntimeException("找不到此房間"));
	    room.setRoomStatus(status);
	    stayRoomRepository.save(room);
	}
	
	// 修改房型
	@Override
	public RoomTypeDto updateRoomType(Integer roomTypeId, Double newPrice, 
	        String roomName, Integer capacity, String roomDescription) {
	    StayRoomType roomType = stayRoomTypeRepository.findById(roomTypeId)
	            .orElseThrow(() -> new RuntimeException("找不到此房型"));
	
	    roomType.setRoomPrice(newPrice);
	    roomType.setRoomName(roomName);
	    roomType.setCapacity(capacity);
	    roomType.setRoomDescription(roomDescription);
	    stayRoomTypeRepository.save(roomType);
	
	    RoomTypeDto dto = new RoomTypeDto();
	    dto.setRoomTypeId(roomType.getRoomTypeId());
	    dto.setRoomName(roomType.getRoomName());
	    dto.setRoomPrice(roomType.getRoomPrice());
	    dto.setRoomDescription(roomType.getRoomDescription());
	    dto.setCapacity(roomType.getCapacity());
	    return dto;
	}
	
	// 私有方法to dto
	private StayResponseDto toDto(Stay stay) {
    StayResponseDto dto = new StayResponseDto();
    dto.setMemberName(stay.getPet().getMember().getMemberName());
    dto.setMemberPhone(stay.getPet().getMember().getPhone());
    dto.setMemberEmail(stay.getPet().getMember().getEmail());
    dto.setStayId(stay.getStayId());
    dto.setPetName(stay.getPet().getPetName());
    dto.setRoomTypeName(stay.getStayRoom().getStayRoomType().getRoomName());
    dto.setStayStartDate(stay.getStayStartDate());
    dto.setStayEndDate(stay.getStayEndDate());
    dto.setStayDay(stay.getStayDay());
    dto.setPetCount(stay.getPetCount());
    dto.setSumPrice(stay.getSumPrice());
    dto.setStayStatus(stay.getStayStatus());
    dto.setStayRemark(stay.getStayRemark());

    stayPaymentRepository.findPaymentByStayId(stay.getStayId())
        .ifPresent(p -> {
            dto.setPaymentStatus(p.getPaymentStatus());
            dto.setCreatedAt(p.getCreatedAt());
            dto.setPaidAt(p.getPaidAt());
            // ✅ 只有付款成功才顯示房號
            if ("SUCCESS".equals(p.getPaymentStatus())) {
                dto.setRoomNo(stay.getStayRoom().getRoomNo());
            }
        });

    return dto;
}
	
		// 查單筆訂單
		@Override
		public StayResponseDto getStayById(Integer stayId) {
		    Stay stay = stayRepository.findById(stayId)
		            .orElseThrow(() -> new RuntimeException("找不到此訂單"));
		    return toDto(stay);  // ← 那一大串全部刪掉，改成這行
		}
		
		//依會員名字搜尋
		@Override
		public List<StayResponseDto> searchByMemberName(String name) {
		    return stayRepository.findByMemberName(name)
		            .stream().map(this::toDto).collect(Collectors.toList());
		}

		//依訂單編號搜尋
		@Override
		public List<StayResponseDto> searchByStayId(Integer stayId) {
		    return stayRepository.findByStayId(stayId)
		            .stream().map(this::toDto).collect(Collectors.toList());
		}

		//依手機末三碼搜尋
		@Override
		public List<StayResponseDto> searchByPhone(String phone) {
		    return stayRepository.findByPet_Member_PhoneEndingWith(phone)
		            .stream().map(this::toDto).collect(Collectors.toList());
		}
		
		// 查所有訂單
		@Override
		public List<StayResponseDto> getAllStays() {
		    return stayRepository.findAll()
		            .stream().map(this::toDto).collect(Collectors.toList());
		}
		
		// 根據ID取得會員訂單
		@Override
		public List<StayResponseDto> getMyStays(Integer memberId) {
		    return stayRepository.findByPet_Member_MemberId(memberId)
		            .stream().map(this::toDto).collect(Collectors.toList());
		}

		
		@Override
		public List<RoomStatusDto> getRoomStatusByDate(LocalDate date) {
		    // 拿所有房間
		    List<StayRoom> allRooms = stayRoomRepository.findAll();
		    // 查這天有重疊預約的 Stay
		    List<Stay> occupiedStays = stayRepository.findAllOverlappingStays(date, date.plusDays(1));
		
		    Map<Integer, Stay> roomStayMap = new HashMap<>();
		    for (Stay stay : occupiedStays) {
		
		        // 只有付款成功的訂單才算佔位
		        boolean isPaid = stayPaymentRepository.findByStay_StayId(stay.getStayId())
		                .map(p -> "SUCCESS".equals(p.getPaymentStatus()))
		                .orElse(false);
		
		        if (!isPaid) continue; // 未付款跳過
		
		        if ("CHECKED_IN".equals(stay.getStayStatus())) {
		            // 已入住
		            roomStayMap.put(stay.getStayRoom().getRoomId(), stay);
		        } else if ("CONFIRMED".equals(stay.getStayStatus())) {
		            // 已預約但未入住
		            roomStayMap.put(stay.getStayRoom().getRoomId(), stay);
		        }
		    }
		
		    List<RoomStatusDto> result = new ArrayList<>();
		    for (StayRoom room : allRooms) {
		        RoomStatusDto dto = new RoomStatusDto();
		        dto.setRoomId(room.getRoomId());
		        dto.setRoomNo(room.getRoomNo());
		        dto.setRoomTypeName(room.getStayRoomType().getRoomName());
		        dto.setRoomStatus(room.getRoomStatus());
		
		        Stay stay = roomStayMap.get(room.getRoomId());
		        if (stay != null) {
		            dto.setStayId(stay.getStayId());
		            dto.setMemberName(stay.getPet().getMember().getMemberName());
		            dto.setPetName(stay.getPet().getPetName());
		            dto.setStayStartDate(stay.getStayStartDate());
		            dto.setStayEndDate(stay.getStayEndDate());
		            dto.setStayStatus(stay.getStayStatus());
		            // 只有 CHECKED_IN 才是已入住
		            dto.setIsOccupied("CHECKED_IN".equals(stay.getStayStatus()));
		        } else {
		            dto.setIsOccupied(false);
		        }
		        result.add(dto);
		    }
		    return result;
		}

		// 今日預約
		@Override
		public List<StayResponseDto> searchByCheckInDate(LocalDate date) {
		    List<Stay> stays = stayRepository.findByStayStartDate(date);
		    return stays.stream().map(this::toDto).collect(Collectors.toList());
		}

		// 退款
		@Override
		public void refundStay(Integer stayId) {

		    // 1. 查訂單
		    Stay stay = stayRepository.findById(stayId)
		            .orElseThrow(() -> new RuntimeException("找不到此訂單"));

		    // 2. 查付款紀錄
		    StayPayment payment = stayPaymentRepository.findPaymentByStayId(stayId)
		            .orElseThrow(() -> new RuntimeException("找不到付款紀錄"));

		    // 3. 確認可以退款
		    if (!"SUCCESS".equals(payment.getPaymentStatus())) {
		        throw new RuntimeException("此訂單尚未付款，無法退款");
		    }

		    // 4. 打 LINE Pay 退款 API
		    boolean success = linePayService.refundPayment(
		        payment.getTradeNo(),
		        payment.getAmount().intValue()
		    );

		    // 5. 更新狀態
		    if (success) {
		        payment.setPaymentStatus("REFUNDED");
		        stayPaymentRepository.save(payment);
		        stay.setStayStatus("CANCELLED");
		        stayRepository.save(stay);
		    } else {
		        throw new RuntimeException("退款失敗，請稍後再試");
		    }
		}
}
