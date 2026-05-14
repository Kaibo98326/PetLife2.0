package com.petlife.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import com.petlife.repository.AdminStayQueryDto;
import com.petlife.repository.AdminStayResponseDto;
import com.petlife.repository.CalendarDayDto;
import com.petlife.repository.PetRepository;
import com.petlife.repository.RoomCalendarDto;
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

	// 根據ID取得會員訂單
	@Override
	public List<StayResponseDto> getMyStays(Integer memberId) {

	    List<Stay> stays = stayRepository.findByPet_Member_MemberId(memberId);
	    List<StayResponseDto> result = new ArrayList<>();

	    for (Stay stay : stays) {
	        StayResponseDto dto = new StayResponseDto();
	        dto.setMemberName(stay.getPet().getMember().getMemberName());
	        dto.setMemberPhone(stay.getPet().getMember().getPhone());
	        dto.setMemberEmail(stay.getPet().getMember().getEmail());
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

	        stayPaymentRepository.findByStay_StayId(stay.getStayId())
	        .ifPresent(p -> {
	            dto.setPaymentStatus(p.getPaymentStatus());
	            dto.setCreatedAt(p.getCreatedAt());
	            dto.setPaidAt(p.getPaidAt());
	        });
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
	
	// 查單筆訂單
	@Override
	public StayResponseDto getStayById(Integer stayId) {
	    Stay stay = stayRepository.findById(stayId)
	            .orElseThrow(() -> new RuntimeException("找不到此訂單"));

	    StayResponseDto dto = new StayResponseDto();
	    dto.setStayId(stay.getStayId());
	    dto.setPetName(stay.getPet().getPetName());
	    dto.setRoomTypeName(stay.getStayRoom().getStayRoomType().getRoomName());
	    dto.setRoomNo(stay.getStayRoom().getRoomNo());
	    dto.setStayStartDate(stay.getStayStartDate());
	    dto.setStayEndDate(stay.getStayEndDate());
	    dto.setStayDay(stay.getStayDay());
	    dto.setSumPrice(stay.getSumPrice());
	    dto.setStayStatus(stay.getStayStatus());
	    return dto;
	}

	//付款紀錄
	@Override
	public Integer getStayIdByMerchantTradeNo(String merchantTradeNo) {
	    StayPayment payment = stayPaymentRepository
	            .findByMerchantTradeNo(merchantTradeNo)
	            .orElseThrow(() -> new RuntimeException("找不到付款紀錄"));
	    return payment.getStay().getStayId();
	}
	
	// ========== 後台訂單管理 ==========
	
		/**
		 * 後台查詢訂單列表（支援分頁、搜尋）
		 */
		@Override
		public Page<AdminStayResponseDto> getAllStaysForAdmin(AdminStayQueryDto query) {
		    Pageable pageable = PageRequest.of(
		        query.getPage(), 
		        query.getSize()
		    );
		    
		    Integer stayIdParam = null;
		    if (query.getStayId() != null && !query.getStayId().isEmpty()) {
		        try {
		            stayIdParam = Integer.parseInt(query.getStayId());
		        } catch (NumberFormatException e) {
		            throw new RuntimeException("訂單編號格式錯誤");
		        }
		    }
		    
		    Page<Stay> stays = stayRepository.searchStays(
		        stayIdParam,
		        query.getStayStatus(),
		        query.getMemberName(),
		        query.getMemberPhone(),
		        query.getStartDate(),
		        query.getEndDate(),
		        pageable
		    );
		    
		    return stays.map(stay -> getStayByIdForAdmin(stay.getStayId()));
		}
		
		/**
		 * 後台查詢單筆訂單詳情
		 */
		@Override
		public AdminStayResponseDto getStayByIdForAdmin(Integer stayId) {
			Stay stay = stayRepository.findById(stayId)
					.orElseThrow(() -> new RuntimeException("找不到此訂單"));
			
			AdminStayResponseDto dto = new AdminStayResponseDto();
			dto.setStayId(stay.getStayId());
			dto.setMemberId(stay.getPet().getMember().getMemberId());
			dto.setMemberName(stay.getPet().getMember().getMemberName());
			dto.setMemberPhone(stay.getPet().getMember().getPhone());
			dto.setMemberEmail(stay.getPet().getMember().getEmail());
			
			// 主要寵物
			dto.setMainPetName(stay.getPet().getPetName());
			dto.setMainPetSpecies(stay.getPet().getSpecies());
			dto.setMainPetBreed(stay.getPet().getBreed());
			
			// 其他寵物（從 JSON 備註中解析）
			if (stay.getStayRemark() != null) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					StayRemarkDto remark = mapper.readValue(stay.getStayRemark(), StayRemarkDto.class);
					if (remark.getPets() != null && remark.getPets().size() > 1) {
						dto.setOtherPetNames(
							remark.getPets().stream()
								.skip(1) // 跳過第一隻
								.map(StayRemarkDto.PetInfoDto::getPetName)
								.collect(Collectors.toList())
						);
					}
				} catch (Exception e) {
					// 解析失敗就忽略
				}
			}
			
			// 房間資訊
			dto.setRoomNo(stay.getStayRoom().getRoomNo());
			dto.setRoomTypeName(stay.getStayRoom().getStayRoomType().getRoomName());
			
			// 訂單資訊
			dto.setStayStartDate(stay.getStayStartDate());
			dto.setStayEndDate(stay.getStayEndDate());
			dto.setStayDay(stay.getStayDay());
			dto.setPetCount(stay.getPetCount());
			dto.setSumPrice(stay.getSumPrice());
			dto.setStayStatus(stay.getStayStatus());
			dto.setStayRemark(stay.getStayRemark());
			
			// 支付資訊
			stayPaymentRepository.findByStay_StayId(stayId).ifPresent(payment -> {
				dto.setPaymentMethod(payment.getPaymentMethod());
				dto.setPaymentStatus(payment.getPaymentStatus());
				dto.setPaidAt(payment.getPaidAt());
				dto.setCreatedAt(payment.getCreatedAt());
			});
			
			return dto;
		}
		
		/**
		 * 修改訂單狀態
		 * PENDING_PAYMENT -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT
		 */
		@Override
		public AdminStayResponseDto updateStayStatus(Integer stayId, String newStatus) {
			Stay stay = stayRepository.findById(stayId)
					.orElseThrow(() -> new RuntimeException("找不到此訂單"));
			
			String currentStatus = stay.getStayStatus();
			
			// ✅ 狀態轉移驗證
			validateStatusTransition(currentStatus, newStatus);
			
			// 更新狀態
			stay.setStayStatus(newStatus);
			stayRepository.save(stay);
			
			// 如果改成「已入住」，房間狀態改成「已預約」
			if ("CHECKED_IN".equals(newStatus)) {
				StayRoom room = stay.getStayRoom();
				room.setRoomStatus("已預約");
				stayRoomRepository.save(room);
			}
			
			// 如果改成「已退房」，房間狀態改回「可預約」
			if ("CHECKED_OUT".equals(newStatus)) {
				StayRoom room = stay.getStayRoom();
				room.setRoomStatus("可預約");
				stayRoomRepository.save(room);
			}
			
			return getStayByIdForAdmin(stayId);
		}
		
		/**
		 * 狀態轉移驗證
		 */
		private void validateStatusTransition(String currentStatus, String newStatus) {
			if (currentStatus.equals(newStatus)) {
				throw new RuntimeException("新狀態與現在狀態相同");
			}
			
			if ("CANCELLED".equals(currentStatus)) {
				throw new RuntimeException("已取消的訂單無法修改");
			}
			
			if ("CHECKED_OUT".equals(currentStatus)) {
				throw new RuntimeException("已退房的訂單無法修改");
			}
			
			// 其他狀態轉移邏輯可自行調整
		}
		
		/**
		 * 後台取消訂單（會同時更新房間狀態為可預約）
		 */
		@Override
		public AdminStayResponseDto cancelStayByAdmin(Integer stayId) {
			Stay stay = stayRepository.findById(stayId)
					.orElseThrow(() -> new RuntimeException("找不到此訂單"));
			
			// 檢查是否可以取消
			if ("CHECKED_IN".equals(stay.getStayStatus())) {
				throw new RuntimeException("已入住的訂單無法取消");
			}
			
			if ("CHECKED_OUT".equals(stay.getStayStatus())) {
				throw new RuntimeException("已退房的訂單無法取消");
			}
			
			if ("CANCELLED".equals(stay.getStayStatus())) {
				throw new RuntimeException("訂單已取消");
			}
			
			// 取消訂單
			stay.setStayStatus("CANCELLED");
			stayRepository.save(stay);
			
			// 房間改回可預約
			StayRoom room = stay.getStayRoom();
			room.setRoomStatus("可預約");
			stayRoomRepository.save(room);
			
			return getStayByIdForAdmin(stayId);
		}
		
		// ========== 房間管理 ==========
		
		/**
		 * 查詢所有房間
		 */
		@Override
		public List<StayRoomDto> getAllRooms() {
			List<StayRoom> rooms = stayRoomRepository.findAll();
			
			return rooms.stream().map(room -> {
				StayRoomDto dto = new StayRoomDto();
				dto.setRoomId(room.getRoomId());
				dto.setRoomNo(room.getRoomNo());
				dto.setRoomTypeId(room.getStayRoomType().getRoomTypeId());
				dto.setRoomTypeName(room.getStayRoomType().getRoomName());
				dto.setRoomStatus(room.getRoomStatus());
				return dto;
			}).collect(Collectors.toList());
		}
		
		/**
		 * 修改房間狀態（停權/啟用）
		 */
		@Override
		public StayRoomDto updateRoomStatus(Integer roomId, String status) {
			StayRoom room = stayRoomRepository.findById(roomId)
					.orElseThrow(() -> new RuntimeException("找不到此房間"));
			
			// ✅ 驗證狀態值
			if (!status.equals("可預約") && !status.equals("維護中")) {
				throw new RuntimeException("無效的房間狀態：" + status);
			}
			
			room.setRoomStatus(status);
			StayRoom updated = stayRoomRepository.save(room);
			
			StayRoomDto dto = new StayRoomDto();
			dto.setRoomId(updated.getRoomId());
			dto.setRoomNo(updated.getRoomNo());
			dto.setRoomTypeId(updated.getStayRoomType().getRoomTypeId());
			dto.setRoomTypeName(updated.getStayRoomType().getRoomName());
			dto.setRoomStatus(updated.getRoomStatus());
			return dto;
		}
		
		// ========== 房型管理 ==========
		
		/**
		 * 修改房型價格
		 */
		@Override
		public RoomTypeDto updateRoomTypePrice(Integer roomTypeId, Double newPrice) {
			StayRoomType roomType = stayRoomTypeRepository.findById(roomTypeId)
					.orElseThrow(() -> new RuntimeException("找不到此房型"));
			
			if (newPrice <= 0) {
				throw new RuntimeException("價格必須大於 0");
			}
			
			roomType.setRoomPrice(newPrice);
			StayRoomType updated = stayRoomTypeRepository.save(roomType);
			
			RoomTypeDto dto = new RoomTypeDto();
			dto.setRoomTypeId(updated.getRoomTypeId());
			dto.setRoomName(updated.getRoomName());
			dto.setRoomPrice(updated.getRoomPrice());
			dto.setRoomDescription(updated.getRoomDescription());
			dto.setCapacity(updated.getCapacity());
			dto.setAvailableCount(stayRoomRepository
				.findByStayRoomType_RoomTypeIdAndRoomStatus(roomTypeId, "可預約").size());
			
			return dto;
		}
		
		// ========== 日期查詢 ==========
		
		/**
		 * 查看日期範圍內所有房間狀態
		 */
		@Override
		public List<RoomCalendarDto> getRoomCalendar(LocalDate startDate, LocalDate endDate) {
			List<StayRoom> allRooms = stayRoomRepository.findAll();
			List<RoomCalendarDto> result = new ArrayList<>();
			
			// 逐日逐房查詢
			LocalDate current = startDate;
			while (!current.isAfter(endDate)) {
				for (StayRoom room : allRooms) {
					// 查這個房間在這天是否被預約
					List<Stay> overlappingStays = stayRepository
						.findOverlappingStays(
							room.getStayRoomType().getRoomTypeId(),
							current,
							current.plusDays(1)
						);
					
					// 篩選該房間的預約
					Stay booking = overlappingStays.stream()
						.filter(s -> s.getStayRoom().getRoomId().equals(room.getRoomId()))
						.findFirst()
						.orElse(null);
					
					RoomCalendarDto dto = new RoomCalendarDto();
					dto.setRoomId(room.getRoomId());
					dto.setRoomNo(room.getRoomNo());
					dto.setRoomTypeId(room.getStayRoomType().getRoomTypeId());
					dto.setRoomTypeName(room.getStayRoomType().getRoomName());
					dto.setDate(current);
					
					if ("維護中".equals(room.getRoomStatus())) {
						dto.setStatus("維護中");
					} else if (booking != null) {
						dto.setStatus("已預約");
						dto.setMemberName(booking.getPet().getMember().getMemberName());
						dto.setStayId(booking.getStayId().toString());
					} else {
						dto.setStatus("可用");
					}
					
					result.add(dto);
				}
				
				current = current.plusDays(1);
			}
			
			return result;
		}
	
	
	
}
