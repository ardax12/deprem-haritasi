package com.ag.deprem.model.dto;

public class PageInfoDto {
    private int size;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private String nextCursor;

    public PageInfoDto() {
    }

    public PageInfoDto(int size, Long totalElements, Integer totalPages, Integer currentPage, String nextCursor) {
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.nextCursor = nextCursor;
    }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public Long getTotalElements() { return totalElements; }
    public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }

    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

    public Integer getCurrentPage() { return currentPage; }
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
}
