package com.itwillbs.entity;

import com.itwillbs.domain.transaction.OrderDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString
public class Order {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "total_price")
    private int totalPrice;

    @Column(name = "order_date")
    private Timestamp orderDate;

    @Column(name = "due_date")
    private Timestamp dueDate;

    @Column(name = "real_date")
    private Timestamp realDate;

    @Column(name = "status")
    private String status;

    @Column(name = "note")
    private String note;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "manager") manager 엔티티를 변수 타입으로 사용하셔야 할 것 같습니다.
    @Column(name = "manager")
    private String manager;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "supplier_code")
    @Column(name = "supplier_code")
    private String supplierCode;

    // OrderItems와의 관계 설정
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItems> orderItems = new ArrayList<>();

}
