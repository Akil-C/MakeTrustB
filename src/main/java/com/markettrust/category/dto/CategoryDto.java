package com.markettrust.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private Long parentId;
    private Boolean isActive;
    private Integer sortOrder;
    private List<CategoryDto> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<CategoryDto> getChildren() { return children; }
    public void setChildren(List<CategoryDto> children) { this.children = children; }

    public static CategoryDtoBuilder builder() { return new CategoryDtoBuilder(); }

    public static class CategoryDtoBuilder {
        private Long id;
        private String name;
        private String description;
        private String iconUrl;
        private Long parentId;
        private Boolean isActive;
        private Integer sortOrder;
        private List<CategoryDto> children;

        public CategoryDtoBuilder id(Long id) { this.id = id; return this; }
        public CategoryDtoBuilder name(String name) { this.name = name; return this; }
        public CategoryDtoBuilder description(String description) { this.description = description; return this; }
        public CategoryDtoBuilder iconUrl(String iconUrl) { this.iconUrl = iconUrl; return this; }
        public CategoryDtoBuilder parentId(Long parentId) { this.parentId = parentId; return this; }
        public CategoryDtoBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public CategoryDtoBuilder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public CategoryDtoBuilder children(List<CategoryDto> children) { this.children = children; return this; }

        public CategoryDto build() {
            CategoryDto dto = new CategoryDto();
            dto.setId(id);
            dto.setName(name);
            dto.setDescription(description);
            dto.setIconUrl(iconUrl);
            dto.setParentId(parentId);
            dto.setIsActive(isActive);
            dto.setSortOrder(sortOrder);
            dto.setChildren(children);
            return dto;
        }
    }
}
