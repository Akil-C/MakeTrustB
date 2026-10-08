package com.markettrust.product.dto;

import com.markettrust.product.entity.ProductCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String keyword;
    private Long categoryId;
    private Long minPrice;
    private Long maxPrice;
    private ProductCondition condition;
    private String city;
    private String state;
    private String sellerLevel;
    private Double minRating;
    private String sortBy;
    private Double lat;
    private Double lng;
    private Double radiusKm;
    private Integer page = 0;
    private Integer size = 20;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getMinPrice() { return minPrice; }
    public void setMinPrice(Long minPrice) { this.minPrice = minPrice; }

    public Long getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Long maxPrice) { this.maxPrice = maxPrice; }

    public ProductCondition getCondition() { return condition; }
    public void setCondition(ProductCondition condition) { this.condition = condition; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getSellerLevel() { return sellerLevel; }
    public void setSellerLevel(String sellerLevel) { this.sellerLevel = sellerLevel; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(Double radiusKm) { this.radiusKm = radiusKm; }

    public Integer getPage() { return page != null ? page : 0; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size != null ? size : 20; }
    public void setSize(Integer size) { this.size = size; }
}
