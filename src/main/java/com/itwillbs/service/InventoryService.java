package com.itwillbs.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.itwillbs.domain.inventory.IncomingDTO;
import com.itwillbs.domain.inventory.IncomingInsertDTO;
import com.itwillbs.domain.inventory.IncomingItemsDTO;

import com.itwillbs.domain.inventory.InventoryItemDTO;
import com.itwillbs.domain.inventory.OutgoingDTO;
import com.itwillbs.domain.inventory.OutgoingInsertDTO;
import com.itwillbs.domain.inventory.OutgoingItemsDTO;

import com.itwillbs.entity.InventoryItem;
import com.itwillbs.repository.IncomingItemsRepository;
import com.itwillbs.repository.IncomingRepository;
import com.itwillbs.repository.InventoryRepository;
import com.itwillbs.repository.OutgoingItemsRepository;
import com.itwillbs.repository.OutgoingRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
	private final InventoryRepository inventoryRepository;
	private final IncomingRepository incomingRepository;
	private final IncomingItemsRepository incomingItemsRepository;
	private final OutgoingRepository outgoingRepository;
	private final OutgoingItemsRepository outgoingItemsRepository;

	// 재고 전체 조회 (페이지네이션 지원)
	public Page<InventoryItemDTO> getInventoryItems(Pageable pageable) {
		log.info("getInventoryItems()");
		return inventoryRepository.getAllInventoryItems(pageable);
	}

	// 재고 부족 품목만 조회 (검색 조건 포함)
	public Page<InventoryItemDTO> findInventoryItemsByOutOfStock(String itemCodeOrName, String itemType,
			Pageable pageable) {
		log.info("findInventoryItemsByOutOfStock()");
		return inventoryRepository.findInventoryItemsByOutOfStock(itemCodeOrName, itemType, pageable);
	}

	// 재고 검색 (검색 조건과 페이지네이션)
	public Page<InventoryItemDTO> findInventoryItemsBySearch(String itemCodeOrName, String itemType,
			Pageable pageable) {
		log.info("findInventoryItems()");
		return inventoryRepository.findInventoryItems(itemCodeOrName, itemType, pageable);
	}

	//재고량, 최소필요재고량 업데이트
	@Transactional
	public void updateInventoryItems(List<InventoryItemDTO> InventoryItemDTOList) throws Exception {
		log.info("InventoryService.updateInventoryItems() - InventoryItemDTOList: {}", InventoryItemDTOList);

		for (InventoryItemDTO itemData : InventoryItemDTOList) {
			String itemCode = itemData.getItemCode();
			Integer quantity = itemData.getQuantity();
			Integer minReqQuantity = itemData.getMinReqQuantity();

			InventoryItem item = inventoryRepository.findById(itemCode)
					.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 품목 코드입니다: " + itemCode));
			item.setQuantity(quantity); //재고량 입력
			item.setMinReqQuantity(minReqQuantity); //최소필요 재고량 입력
			inventoryRepository.save(item); //품목 업데이트
		}
	}
	
	// 입고 페이지 진입시 조회
	public Page<IncomingDTO> getIncomingLists(Pageable pageable) {
		log.info("getIncomingLists()");

		// 페이지 사이즈에 맞는 입고 테이블 데이터 조회
		Page<IncomingDTO> incomingByPage = incomingRepository.getIncomingLists(pageable);

		// 각 입고데이터마다 품목의 이름과 갯수를 구하기 위한 반복문
		incomingByPage.forEach(dto -> {

			String incomingId = dto.getIncomingId();

			// incoming_items테이블에서 품목의 이름과 갯수를 구한다.
			List<IncomingItemsDTO> itemNames = incomingItemsRepository.findIncomingItemsListById(incomingId);

			// 품목중 첫번째 품목의 이름을 저장
			dto.setIncomingItemDisplay(itemNames.get(0).getItemName());

			// 품목 갯수 - 1을 저장
			dto.setOtherCount(itemNames.size() - 1);

		});

		return incomingByPage;
	}

	// 입고 목록 검색 (검색 조건과 페이지네이션 포함)
	public Page<IncomingDTO> findIncomingBySearch(String itemCodeOrName, String reasonOfIncoming,

			Timestamp incomingStartDate_start, Timestamp incomingStartDate_end, String incomingId, String prodOrQualId,
			String status, String managerCodeOrName, Pageable pageable) {
		log.info("findIncomingBySearch()");

		// 리포지토리 호출 시 itemCodeOrName 포함
		Page<IncomingDTO> incomingByPage = incomingRepository.findIncomingLists(reasonOfIncoming,
				incomingStartDate_start, incomingStartDate_end, incomingId, prodOrQualId, status, managerCodeOrName,
				itemCodeOrName, pageable);

		// 각 입고 데이터마다 품목의 이름과 갯수를 구하기 위한 반복문
		// ※최적화 생각해야함
		incomingByPage.forEach(dto -> {

			String incomingId2 = dto.getIncomingId();

			// incoming_items 테이블에서 품목코드와 품목이름을 구함
			List<IncomingItemsDTO> itemNames = incomingItemsRepository.findIncomingItemsListById(incomingId2);

			if (!itemNames.isEmpty()) {
				// 첫 번째 품목의 이름을 설정
				dto.setIncomingItemDisplay(itemNames.get(0).getItemName());
				// 나머지 품목 갯수 설정
				dto.setOtherCount(itemNames.size() - 1);
			} else {
				dto.setIncomingItemDisplay("");
				dto.setOtherCount(0);
			}
		});

		return incomingByPage;
	}

	// 입고 품목 리스트 가져오기
	public List<IncomingItemsDTO> getIncomingItems(String incomingId) {

		return incomingItemsRepository.findByIncomingItems(incomingId);
	}

	// 입고 상세 정보 모달창에서 입고 상태 업데이트
	@Transactional
	public void updateIncomingStatus(String incomingId) {
		Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
		int updatedRows = incomingRepository.updateIncomingStatus(incomingId, currentTime);
		if (updatedRows == 0) {
			throw new EntityNotFoundException("해당 입고 ID가 존재하지 않습니다: " + incomingId);
		}
	}

	// 입고 등록 페이지에서 입고 대상 가져오기
	public List<IncomingInsertDTO> findIncomingInsertList() {

		// 생산 완료가되었지만 아직 입고 등록이 안된 생산데이터 저장
		List<IncomingInsertDTO> incomingInsertDTOProd = incomingRepository.findAllEndOfProduction();

		// 각 생산 완료된 데이터행마다 품목의 이름과 갯수를 구하기 위한 반복문
		// 생산코드 하나에 품목 하나만 있으므로 구현하지 않음. 필요시 구현

		// 입하 검품완료가되었지만 아직 입고 등록이 안된 검품데이터 저장
		List<IncomingInsertDTO> incomingInsertDTOQual = incomingRepository.findAllEndOfQuality();

		// 각 검품완료된 데이터행마다 품목의 이름과 갯수를 구하기 위한 반복문
		// ※최적화 생각해야함
//		incomingInsertDTOQual.forEach(dto -> {
//
//			String QualityOrderId = dto.getIncomingInsertCode();
//
//			// quality_order_items테이블에서 품목코드와 품목이름을 구함
//			List<IncomingItemsDTO> itemNames = incomingItemsRepository.findQualityOrderItemsById(QualityOrderId);
//
//			if (!itemNames.isEmpty()) {
//				// 첫 번째 품목의 이름을 설정
//				dto.setIncomingItemDisplay(itemNames.get(0).getItemName());
//				// 나머지 품목 갯수 설정
//				dto.setOtherCount(itemNames.size() - 1);
//			} else {
//				dto.setIncomingItemDisplay("");
//				dto.setOtherCount(0);
//			}
//		});

		incomingInsertDTOProd.addAll(incomingInsertDTOQual);

		return null;
	}



	
	
	
	
	
	
	
	
	
	
	
	// 출고 페이지 진입시 조회
		public Page<OutgoingDTO> getOutgoingLists(Pageable pageable) {
			log.info("getOutgoingLists()");

			// 페이지 사이즈에 맞는 출고 테이블 데이터 조회
			Page<OutgoingDTO> outgoingByPage = outgoingRepository.getOutgoingLists(pageable);

			// 각 출고데이터마다 품목의 이름과 갯수를 구하기 위한 반복문
			outgoingByPage.forEach(dto -> {

				String outgoingId = dto.getOutgoingId();

				// outgoing_items테이블에서 품목의 이름과 갯수를 구한다.
				List<OutgoingItemsDTO> itemNames = outgoingItemsRepository.findOutgoingItemsListById(outgoingId);

				// 품목중 첫번째 품목의 이름을 저장
				dto.setOutgoingItemDisplay(itemNames.get(0).getItemName());

				// 품목 갯수 - 1을 저장
				dto.setOtherCount(itemNames.size() - 1);

			});

			return outgoingByPage;
		}

		// 출고 목록 검색 (검색 조건과 페이지네이션 포함)
		public Page<OutgoingDTO> findOutgoingBySearch(String itemCodeOrName, String reasonOfOutgoing,

				Timestamp outgoingStartDate_start, Timestamp outgoingStartDate_end, String outgoingId, String prodOrQualId,
				String status, String managerCodeOrName, Pageable pageable) {
			log.info("findOutgoingBySearch()");

			// 리포지토리 호출 시 itemCodeOrName 포함
			Page<OutgoingDTO> outgoingByPage = outgoingRepository.findOutgoingLists(reasonOfOutgoing,
					outgoingStartDate_start, outgoingStartDate_end, outgoingId, prodOrQualId, status, managerCodeOrName,
					itemCodeOrName, pageable);

			// 각 출고 데이터마다 품목의 이름과 갯수를 구하기 위한 반복문
			// ※최적화 생각해야함
			outgoingByPage.forEach(dto -> {

				String outgoingId2 = dto.getOutgoingId();

				// outgoing_items 테이블에서 품목코드와 품목이름을 구함
				List<OutgoingItemsDTO> itemNames = outgoingItemsRepository.findOutgoingItemsListById(outgoingId2);

				if (!itemNames.isEmpty()) {
					// 첫 번째 품목의 이름을 설정
					dto.setOutgoingItemDisplay(itemNames.get(0).getItemName());
					// 나머지 품목 갯수 설정
					dto.setOtherCount(itemNames.size() - 1);
				} else {
					dto.setOutgoingItemDisplay("");
					dto.setOtherCount(0);
				}
			});

			return outgoingByPage;
		}

		// 출고 품목 리스트 가져오기
		public List<OutgoingItemsDTO> getOutgoingItems(String outgoingId) {

			return outgoingItemsRepository.findByOutgoingItems(outgoingId);
		}

		// 출고 상세 정보 모달창에서 출고 상태 업데이트
		@Transactional
		public void updateOutgoingStatus(String outgoingId) {
			Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
			int updatedRows = outgoingRepository.updateOutgoingStatus(outgoingId, currentTime);
			if (updatedRows == 0) {
				throw new EntityNotFoundException("해당 출고 ID가 존재하지 않습니다: " + outgoingId);
			}
		}

		// 출고 등록 페이지에서 출고 대상 가져오기
		public List<OutgoingInsertDTO> findOutgoingInsertList() {

			// 생산 완료가되었지만 아직 출고 등록이 안된 생산데이터 저장
			List<OutgoingInsertDTO> outgoingInsertDTOProd = outgoingRepository.findAllEndOfProduction();

			// 각 생산 요청된 데이터행마다 품목의 이름과 갯수를 구하기 위한 반복문
			// 생산코드 하나에 품목 하나만 있으므로 구현하지 않음. 필요시 구현

			// 입하 검품완료가되었지만 아직 출고 등록이 안된 검품데이터 저장
			List<OutgoingInsertDTO> outgoingInsertDTOQual = outgoingRepository.findAllEndOfQuality();

			// 각 검품완료된 데이터행마다 품목의 이름과 갯수를 구하기 위한 반복문
			// ※최적화 생각해야함
//			outgoingInsertDTOQual.forEach(dto -> {
//
//				String QualitySaleId = dto.getOutgoingInsertCode();
//
//				// quality_sale_items테이블에서 품목코드와 품목이름을 구함
//				List<OutgoingItemsDTO> itemNames = outgoingItemsRepository.findQualitySaleItemsById(QualitySaleId);
//
//				if (!itemNames.isEmpty()) {
//					// 첫 번째 품목의 이름을 설정
//					dto.setOutgoingItemDisplay(itemNames.get(0).getItemName());
//					// 나머지 품목 갯수 설정
//					dto.setOtherCount(itemNames.size() - 1);
//				} else {
//					dto.setOutgoingItemDisplay("");
//					dto.setOtherCount(0);
//				}
//			});

			outgoingInsertDTOProd.addAll(outgoingInsertDTOQual);

			return null;
		}
	
	
	
	
	
	
	
	
	
	
	
	


}
