package com.aiblog.dto;

import java.util.List;

public class GlobalSearchResponse {
    private String query;
    private int totalCount;
    private List<GlobalSearchGroupResponse> groups;

    public GlobalSearchResponse() {
    }

    public GlobalSearchResponse(String query, List<GlobalSearchGroupResponse> groups) {
        this.query = query;
        this.groups = groups;
        this.totalCount = groups.stream().mapToInt(group -> group.getItems().size()).sum();
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public List<GlobalSearchGroupResponse> getGroups() { return groups; }
    public void setGroups(List<GlobalSearchGroupResponse> groups) { this.groups = groups; }
}
