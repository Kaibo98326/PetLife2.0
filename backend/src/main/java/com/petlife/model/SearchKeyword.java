package com.petlife.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "Search_Keywords")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchKeyword implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String keyword;

    @Column(name = "search_count")
    private Integer searchCount = 1;

    @Column(name = "last_search_time")
    @CreationTimestamp
    private LocalDateTime lastSearchTime;
}
