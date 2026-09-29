package com.duong.travelweb.model.dto;

/** Thống kê theo chuỗi tìm kiếm (đã chuẩn hoá chữ thường, bỏ khoảng trắng thừa). */
public class SearchQueryStatDTO {
    private String queryText;
    private long searches;
    private long users;
    private long clicks;
    /** Số lần tìm không ra kết quả nào — gợi ý nội dung còn thiếu. */
    private long zeroResults;

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public long getSearches() {
        return searches;
    }

    public void setSearches(long searches) {
        this.searches = searches;
    }

    public long getUsers() {
        return users;
    }

    public void setUsers(long users) {
        this.users = users;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }

    public long getZeroResults() {
        return zeroResults;
    }

    public void setZeroResults(long zeroResults) {
        this.zeroResults = zeroResults;
    }
}
