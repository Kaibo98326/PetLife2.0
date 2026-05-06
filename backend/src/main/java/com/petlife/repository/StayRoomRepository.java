package com.petlife.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petlife.model.StayRoom;

@Repository
public interface StayRoomRepository extends JpaRepository<StayRoom, Integer>{

	// 查這個房型總共幾間房
	List<StayRoom> findByStayRoomType_StayRoomTypeId(Integer roomTypeId);
	
	// 查這個房型中 狀態為空閒的房間
	List<StayRoom> findByStayRoom_StayRoomTypeIdAndRoomStatus(Integer stayRoomTyeId,String roomStatus);
}
