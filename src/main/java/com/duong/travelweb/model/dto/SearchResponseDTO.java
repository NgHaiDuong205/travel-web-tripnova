package com.duong.travelweb.model.dto;

import java.util.List;
import java.util.Map;

public class SearchResponseDTO {
    private String query;
    private String type;
    /** id dòng search_queries, FE gửi kèm khi user bấm vào kết quả (POST /api/search/click/). */
    private Long searchId;
    /** Số kết quả khớp của từng loại (không bị giới hạn bởi limit). */
    private Map<String, Long> counts;
    private List<SearchResultDTO> results;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSearchId() {
        return searchId;
    }

    public void setSearchId(Long searchId) {
        this.searchId = searchId;
    }

    public Map<String, Long> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, Long> counts) {
        this.counts = counts;
    }

    public List<SearchResultDTO> getResults() {
        return results;
    }

    public void setResults(List<SearchResultDTO> results) {
        this.results = results;
    }
}
