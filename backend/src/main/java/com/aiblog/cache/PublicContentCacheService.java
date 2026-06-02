package com.aiblog.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class PublicContentCacheService {

    private static final String POSTS_PREFIX = "public:posts:";
    private static final String SKILLS_PREFIX = "public:skills:";
    private static final String MCPS_PREFIX = "public:mcps:";
    private static final String API_STATIONS_PREFIX = "public:api-stations:";
    private static final String FORUM_CATEGORIES_PREFIX = "public:forum-categories:";
    private static final String SEARCH_PREFIX = "public:search:";

    private final HybridCacheService cacheService;
    private final CacheProperties properties;

    public PublicContentCacheService(HybridCacheService cacheService, CacheProperties properties) {
        this.cacheService = cacheService;
        this.properties = properties;
    }

    public <T> T publicContent(String key, Class<T> type, Supplier<T> loader) {
        return cacheService.getOrLoad(key, type, Duration.ofMillis(Math.max(1, properties.getPublicContentTtlMs())), loader);
    }

    public <T> T publicContent(String key, TypeReference<T> type, Supplier<T> loader) {
        return cacheService.getOrLoad(key, type, Duration.ofMillis(Math.max(1, properties.getPublicContentTtlMs())), loader);
    }

    public <T> T search(String key, Class<T> type, Supplier<T> loader) {
        return cacheService.getOrLoad(searchKey(key), type, Duration.ofMillis(Math.max(1, properties.getSearchTtlMs())), loader);
    }

    public <T> T search(String key, TypeReference<T> type, Supplier<T> loader) {
        return cacheService.getOrLoad(searchKey(key), type, Duration.ofMillis(Math.max(1, properties.getSearchTtlMs())), loader);
    }

    public String postsListKey() {
        return POSTS_PREFIX + "list";
    }

    public String postDetailKey(String slug) {
        return POSTS_PREFIX + "detail:" + normalize(slug);
    }

    public String skillsListKey(String q, String tag, String category) {
        return SKILLS_PREFIX + "list:" + params(q, tag, category);
    }

    public String skillDetailKey(Long id) {
        return SKILLS_PREFIX + "detail:" + id;
    }

    public String mcpsListKey(String q, String tag, String category) {
        return MCPS_PREFIX + "list:" + params(q, tag, category);
    }

    public String mcpDetailKey(Long id) {
        return MCPS_PREFIX + "detail:" + id;
    }

    public String apiStationsListKey(String q, String tag) {
        return API_STATIONS_PREFIX + "list:" + params(q, tag, null);
    }

    public String apiStationDetailKey(Long id) {
        return API_STATIONS_PREFIX + "detail:" + id;
    }

    public String forumCategoriesListKey() {
        return FORUM_CATEGORIES_PREFIX + "list";
    }

    public String forumCategoryDetailKey(Long id) {
        return FORUM_CATEGORIES_PREFIX + "detail:" + id;
    }

    public void evictPosts() {
        cacheService.evictByPrefix(POSTS_PREFIX);
        evictSearch();
    }

    public void evictSkills() {
        cacheService.evictByPrefix(SKILLS_PREFIX);
        evictSearch();
    }

    public void evictMcps() {
        cacheService.evictByPrefix(MCPS_PREFIX);
        evictSearch();
    }

    public void evictApiStations() {
        cacheService.evictByPrefix(API_STATIONS_PREFIX);
        evictSearch();
    }

    public void evictForumCategories() {
        cacheService.evictByPrefix(FORUM_CATEGORIES_PREFIX);
    }

    public void evictSearch() {
        cacheService.evictByPrefix(SEARCH_PREFIX);
    }

    private String searchKey(String key) {
        return SEARCH_PREFIX + normalize(key);
    }

    private String params(String first, String second, String third) {
        return normalize(first) + ":" + normalize(second) + ":" + normalize(third);
    }

    private String normalize(Object value) {
        if (value == null) {
            return "_";
        }
        String normalized = value.toString().trim().toLowerCase();
        return normalized.isEmpty() ? "_" : normalized.replaceAll("[^a-z0-9\\p{IsHan}._-]", "_");
    }
}
