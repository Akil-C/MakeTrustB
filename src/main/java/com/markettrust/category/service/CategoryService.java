package com.markettrust.category.service;

import com.markettrust.category.dto.CategoryDto;
import com.markettrust.category.dto.CategoryRequest;
import com.markettrust.category.entity.Category;
import com.markettrust.category.repository.CategoryRepository;
import com.markettrust.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    // -------------------------------------------------------------------------
    // Public queries
    // -------------------------------------------------------------------------

    /**
     * Returns all active categories as a hierarchical tree (root → children).
     */
    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        List<Category> all = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
        return buildTree(all, null);
    }

    /**
     * Returns a single category by ID (including inactive – useful for admin).
     */
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = findOrThrow(id);
        List<Category> children = categoryRepository.findByParentIdAndIsActiveTrue(id);
        CategoryDto dto = toDto(category);
        dto.setChildren(children.stream().map(this::toDto).collect(Collectors.toList()));
        return dto;
    }

    // -------------------------------------------------------------------------
    // Admin mutations
    // -------------------------------------------------------------------------

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        if (request.getParentId() != null) {
            findOrThrow(request.getParentId()); // validate parent exists
        }
        Category category = new Category();
        applyRequest(category, request);
        Category saved = categoryRepository.save(category);
        log.info("Created category id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        if (request.getParentId() != null && !request.getParentId().equals(id)) {
            findOrThrow(request.getParentId()); // validate parent exists and not self
        }
        applyRequest(category, request);
        Category saved = categoryRepository.save(category);
        log.info("Updated category id={}", saved.getId());
        return toDto(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        category.setParentId(request.getParentId());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());
    }

    private List<CategoryDto> buildTree(List<Category> all, Long parentId) {
        // Group by parentId for O(n) tree construction
        Map<Long, List<Category>> byParent = all.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getParentId() == null ? -1L : c.getParentId()
                ));

        Long key = parentId == null ? -1L : parentId;
        List<Category> roots = byParent.getOrDefault(key, List.of());
        return roots.stream()
                .map(c -> {
                    CategoryDto dto = toDto(c);
                    List<CategoryDto> children = buildTree(all, c.getId());
                    if (!children.isEmpty()) dto.setChildren(children);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private CategoryDto toDto(Category c) {
        return CategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .iconUrl(c.getIconUrl())
                .parentId(c.getParentId())
                .isActive(c.getIsActive())
                .sortOrder(c.getSortOrder())
                .build();
    }
}
