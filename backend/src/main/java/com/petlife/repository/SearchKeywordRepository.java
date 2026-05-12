package com.petlife.repository;

import com.petlife.model.SearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Integer> {
    
    Optional<SearchKeyword> findByKeyword(String keyword);

    @Query(value = "SELECT TOP 10 * FROM Search_Keywords ORDER BY search_count DESC", nativeQuery = true)
    List<SearchKeyword> findTop10HotKeywords();
}
