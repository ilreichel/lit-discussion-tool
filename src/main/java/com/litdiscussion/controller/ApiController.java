package com.litdiscussion.controller;

import com.litdiscussion.model.*;
import com.litdiscussion.service.ClassService;
import com.litdiscussion.service.DiscussionService;
import com.litdiscussion.service.VisualizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final ClassService classService;
    private final DiscussionService discussionService;
    private final VisualizationService visualizationService;

    public ApiController(ClassService classService,
                        DiscussionService discussionService,
                        VisualizationService visualizationService) {
        this.classService = classService;
        this.discussionService = discussionService;
        this.visualizationService = visualizationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        try {
            User user = classService.login(username.trim());
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/classes")
    public ResponseEntity<?> getClasses(@RequestParam String username) {
        try {
            List<Classroom> classes = classService.getClassesForUser(username);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Classroom c : classes) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", c.getId());
                map.put("name", c.getName());
                map.put("description", c.getDescription());
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/classes/{classroomId}/books")
    public ResponseEntity<?> getBooksForClass(@PathVariable Long classroomId) {
        try {
            List<Book> books = classService.getBooksForClassroom(classroomId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Book b : books) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", b.getId());
                map.put("title", b.getTitle());
                map.put("author", b.getAuthor());
                List<Map<String, Object>> chaptersList = new ArrayList<>();
                for (Chapter ch : b.getChapters()) {
                    Map<String, Object> chMap = new LinkedHashMap<>();
                    chMap.put("id", ch.getId());
                    chMap.put("chapterNumber", ch.getChapterNumber());
                    chMap.put("title", ch.getTitle());
                    chaptersList.add(chMap);
                }
                map.put("chapters", chaptersList);
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/books/{bookId}/posts")
    public ResponseEntity<?> getPostsForBook(@PathVariable Long bookId) {
        try {
            List<DiscussionPost> posts = discussionService.getPostsByBook(bookId);
            return ResponseEntity.ok(formatPosts(posts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/themes")
    public ResponseEntity<?> getAllThemes() {
        List<Theme> themes = discussionService.getAllThemes();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Theme t : themes) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/themes/{themeName}/posts")
    public ResponseEntity<?> getPostsForTheme(@PathVariable String themeName,
                                              @RequestParam(required = false) Long bookId) {
        try {
            List<DiscussionPost> posts;
            if (bookId != null) {
                posts = discussionService.getPostsByThemeAndBook(themeName, bookId);
            } else {
                posts = discussionService.getPostsByTheme(themeName);
            }
            return ResponseEntity.ok(formatPosts(posts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/visualization")
    public ResponseEntity<?> getVisualization(@RequestParam(required = false) Long bookId) {
        try {
            VisualizationService.VisualizationData data = visualizationService.generateVisualization(bookId);
            Map<String, Object> result = new LinkedHashMap<>();
            List<Map<String, Object>> nodesList = new ArrayList<>();
            for (VisualizationService.VisualizationNode node : data.getNodes()) {
                Map<String, Object> nMap = new LinkedHashMap<>();
                nMap.put("id", node.getId());
                nMap.put("label", node.getLabel());
                nMap.put("frequency", node.getFrequency());
                nMap.put("size", node.getSize());
                nMap.put("cluster", node.getCluster());
                nMap.put("color", node.getColor());
                nodesList.add(nMap);
            }
            List<Map<String, Object>> edgesList = new ArrayList<>();
            for (VisualizationService.VisualizationEdge edge : data.getEdges()) {
                Map<String, Object> eMap = new LinkedHashMap<>();
                eMap.put("source", edge.getSource());
                eMap.put("target", edge.getTarget());
                eMap.put("weight", edge.getWeight());
                eMap.put("similarity", edge.getSimilarity());
                edgesList.add(eMap);
            }
            result.put("nodes", nodesList);
            result.put("edges", edgesList);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/books")
    public ResponseEntity<?> getAllBooks() {
        List<Book> books = visualizationService.getAllBooks();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Book b : books) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", b.getId());
            map.put("title", b.getTitle());
            map.put("author", b.getAuthor());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            Long classroomId = ((Number) request.get("classroomId")).longValue();
            Long bookId = ((Number) request.get("bookId")).longValue();
            Long chapterId = ((Number) request.get("chapterId")).longValue();
            String content = (String) request.get("content");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Discussion content is required"));
            }

            List<Map<String, Object>> quotesRaw = (List<Map<String, Object>>) request.get("quotes");
            if (quotesRaw == null || quotesRaw.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one quotation is required"));
            }

            List<DiscussionService.QuoteEntry> quoteEntries = new ArrayList<>();
            for (Map<String, Object> q : quotesRaw) {
                String text = (String) q.get("text");
                List<String> themeNames = (List<String>) q.get("themeNames");
                quoteEntries.add(new DiscussionService.QuoteEntry(text, themeNames));
            }

            DiscussionPost post = discussionService.createPost(username, classroomId, bookId, chapterId, content, quoteEntries);
            return ResponseEntity.ok(formatPost(post));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create post: " + e.getMessage()));
        }
    }

    @PostMapping("/detect-quotes")
    public ResponseEntity<?> detectQuotes(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        List<String> quotes = discussionService.detectQuotes(text);
        return ResponseEntity.ok(Map.of("quotes", quotes));
    }

    @GetMapping("/themes/{themeName}/frequencies")
    public ResponseEntity<?> getThemeFrequencies(@RequestParam(required = false) Long bookId) {
        Map<String, Long> frequencies = discussionService.getThemeFrequencies(bookId);
        return ResponseEntity.ok(frequencies);
    }

    private List<Map<String, Object>> formatPosts(List<DiscussionPost> posts) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DiscussionPost post : posts) {
            result.add(formatPost(post));
        }
        return result;
    }

    private Map<String, Object> formatPost(DiscussionPost post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("author", post.getAuthor().getDisplayName());
        map.put("authorUsername", post.getAuthor().getUsername());
        map.put("bookId", post.getBook().getId());
        map.put("bookTitle", post.getBook().getTitle());
        map.put("chapterId", post.getChapter().getId());
        map.put("chapterNumber", post.getChapter().getChapterNumber());
        map.put("chapterTitle", post.getChapter().getTitle());
        map.put("content", post.getContent());
        map.put("publishedAt", post.getPublishedAt().toString());

        List<Map<String, Object>> quotesList = new ArrayList<>();
        for (Quote q : post.getQuotes()) {
            Map<String, Object> qMap = new LinkedHashMap<>();
            qMap.put("id", q.getId());
            qMap.put("text", q.getText());
            List<String> themeNames = new ArrayList<>();
            for (Theme t : q.getThemes()) {
                themeNames.add(t.getName());
            }
            qMap.put("themes", themeNames);
            quotesList.add(qMap);
        }
        map.put("quotes", quotesList);
        return map;
    }
}
