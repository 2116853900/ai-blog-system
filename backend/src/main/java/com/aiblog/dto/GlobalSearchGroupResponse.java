package com.aiblog.dto;

import java.util.List;

public class GlobalSearchGroupResponse {
    private String type;
    private String label;
    private List<GlobalSearchItemResponse> items;

    public GlobalSearchGroupResponse() {
    }

    public GlobalSearchGroupResponse(String type, String label, List<GlobalSearchItemResponse> items) {
        this.type = type;
        this.label = label;
        this.items = items;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public List<GlobalSearchItemResponse> getItems() { return items; }
    public void setItems(List<GlobalSearchItemResponse> items) { this.items = items; }
}
