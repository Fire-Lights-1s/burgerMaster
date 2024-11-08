package com.itwillbs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itwillbs.domain.dashboard.DefectiveDTO;
import com.itwillbs.domain.dashboard.IncomingItemDTO;
import com.itwillbs.domain.dashboard.InventoryItemDTO;
import com.itwillbs.domain.dashboard.ItemDashDTO;
import com.itwillbs.entity.Item;


@Repository
public interface MainDashBoardItemRepository extends JpaRepository<Item, String> {

	

	

	//반품 폐기
	 @Query("SELECT new com.itwillbs.domain.dashboard.DefectiveDTO(d.quantity, i.itemName, d.status,d.note,d.defectiveId,i.itemCode) " +
		       "FROM DefectiveDash d JOIN Item i ON i.itemCode = d.itemCode.itemCode " +  
		       "WHERE d.status = :status")
	    List<DefectiveDTO> findByStatus(@Param("status") String status);
	 
	 //입고량
	 @Query("SELECT new com.itwillbs.domain.dashboard.IncomingItemDTO(c.quantity, i.itemName, i.itemCode,c.incomingItemId) " +
		       "FROM IncommingItemDash c JOIN Item i ON i.itemCode = c.itemCode.itemCode " +  
		       "WHERE i.itemType = :itemType")
	    List<IncomingItemDTO> findByItemType(@Param("itemType") String itemType);
	 
	 //재고량
	 @Query("SELECT new com.itwillbs.domain.dashboard.InventoryItemDTO(v.quantity, i.itemName,v.minReqQuantity) " +
		       "FROM InventoryItem v JOIN Item i ON i.itemCode = v.itemCode " +  
		       "WHERE i.itemType = :itemType")
	    List<InventoryItemDTO> findByInventoryItemType(@Param("itemType") String itemType);
	
//	 @Query("SELECT new com.itwillbs.domain.dashboard.SaleDTO(v.quantity, i.itemName,v.minReqQuantity) " +
//		       "FROM Sale s JOIN Item i ON i.itemCode = v.itemCode " +  
//		       "WHERE s.status = :status")
//	 List<SaleDTO> findByStatus(@Param("status") String status);
	 
	 
	 
	 
	 
}//
