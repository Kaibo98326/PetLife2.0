package com.petlife.service;

import com.petlife.model.SearchKeyword;
import com.petlife.repository.SearchKeywordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SearchKeywordService {

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    /**
     * 紀錄或更新關鍵字搜尋次數
     */
    public void recordKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        
        String cleanKeyword = keyword.trim();
        Optional<SearchKeyword> existing = searchKeywordRepository.findByKeyword(cleanKeyword);
        
        if (existing.isPresent()) {
            SearchKeyword sk = existing.get();
            sk.setSearchCount(sk.getSearchCount() + 1);
            sk.setLastSearchTime(LocalDateTime.now());
            searchKeywordRepository.save(sk);
        } else {
            SearchKeyword sk = new SearchKeyword();
            sk.setKeyword(cleanKeyword);
            sk.setSearchCount(1);
            sk.setLastSearchTime(LocalDateTime.now());
            searchKeywordRepository.save(sk);
        }
    }

    /**
     * 取得熱門前 10 名關鍵字
     */
    public List<SearchKeyword> getHotKeywords() {
        List<SearchKeyword> list = searchKeywordRepository.findTop10HotKeywords();
        
        // 如果資料庫是空的，初始化一些預設關鍵字
        if (list.isEmpty()) {
            initDefaultKeywords();
            list = searchKeywordRepository.findTop10HotKeywords();
        }
        
        return list;
    }

    /**
     * 初始化預設關鍵字
     */
    private void initDefaultKeywords() {
        List<String> defaults = Arrays.asList(
            "原點", "第一饗宴", "法國皇家", "貓砂", 
            "貓抓板", "優格", "鮮樂嚐", "希爾思", 
            "歐睿健", "Petlife", "K9 Natural", "wellness"
        );
        
        for (String kw : defaults) {
            SearchKeyword sk = new SearchKeyword();
            sk.setKeyword(kw);
            sk.setSearchCount((int)(Math.random() * 50) + 10); // 隨機給點次數讓它好看
            sk.setLastSearchTime(LocalDateTime.now());
            searchKeywordRepository.save(sk);
        }
    }
}
