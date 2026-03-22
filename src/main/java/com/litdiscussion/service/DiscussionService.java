package com.litdiscussion.service;

import com.litdiscussion.model.*;
import com.litdiscussion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private final DiscussionPostRepository postRepository;
    private final QuoteRepository quoteRepository;
    private final ThemeRepository themeRepository;
    private final ChapterRepository chapterRepository;
    private final BookRepository bookRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;

    public DiscussionService(DiscussionPostRepository postRepository,
                             QuoteRepository quoteRepository,
                             ThemeRepository themeRepository,
                             ChapterRepository chapterRepository,
                             BookRepository bookRepository,
                             ClassroomRepository classroomRepository,
                             UserRepository userRepository) {
        this.postRepository = postRepository;
        this.quoteRepository = quoteRepository;
        this.themeRepository = themeRepository;
        this.chapterRepository = chapterRepository;
        this.bookRepository = bookRepository;
        this.classroomRepository = classroomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DiscussionPost createPost(String username, Long classroomId, Long bookId,
                                     Long chapterId, String content,
                                     List<QuoteEntry> quoteEntries) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found"));

        if (quoteEntries == null || quoteEntries.isEmpty()) {
            throw new IllegalArgumentException("At least one quotation is required");
        }

        for (QuoteEntry entry : quoteEntries) {
            String normalizedQuote = normalizeQuote(entry.getText());
            if (normalizedQuote.split("\\s+").length <= 2) {
                throw new IllegalArgumentException("Each quotation must contain more than two words");
            }
            if (entry.getThemeNames() == null || entry.getThemeNames().isEmpty()) {
                throw new IllegalArgumentException("Each quotation must have at least one tag/theme");
            }
        }

        DiscussionPost post = new DiscussionPost(author, classroom, book, chapter, content);
        post = postRepository.save(post);

        for (QuoteEntry entry : quoteEntries) {
            Quote quote = new Quote(entry.getText(), post);
            quote = quoteRepository.save(quote);

            for (String themeName : entry.getThemeNames()) {
                String trimmedName = themeName.trim();
                if (trimmedName.isEmpty()) continue;

                Theme theme = themeRepository.findByName(trimmedName)
                        .orElseGet(() -> themeRepository.save(new Theme(trimmedName, username)));

                quote.getThemes().add(theme);
            }
            quoteRepository.save(quote);
            post.getQuotes().add(quote);
        }

        return post;
    }

    public List<DiscussionPost> getPostsByBook(Long bookId) {
        return postRepository.findByBookIdOrderByChapter_ChapterNumberAscPublishedAtAsc(bookId);
    }

    public List<DiscussionPost> getPostsByClassroom(Long classroomId) {
        return postRepository.findByClassroomIdOrderByChapter_ChapterNumberAscPublishedAtAsc(classroomId);
    }

    public List<DiscussionPost> getPostsByTheme(String themeName) {
        return postRepository.findByThemeNameOrderByChapterAndTime(themeName);
    }

    public List<DiscussionPost> getPostsByThemeAndBook(String themeName, Long bookId) {
        return postRepository.findByThemeNameAndBookIdOrderByChapterAndTime(themeName, bookId);
    }

    public List<Theme> getAllThemes() {
        return themeRepository.findAllByOrderByNameAsc();
    }

    public List<Theme> getThemesByBook(Long bookId) {
        List<DiscussionPost> posts = postRepository.findByBookIdOrderByChapter_ChapterNumberAscPublishedAtAsc(bookId);
        Set<String> themeNames = new TreeSet<>();
        for (DiscussionPost post : posts) {
            for (Quote quote : post.getQuotes()) {
                for (Theme theme : quote.getThemes()) {
                    themeNames.add(theme.getName());
                }
            }
        }
        return themeRepository.findByNameIn(new ArrayList<>(themeNames));
    }

    public Map<String, Long> getThemeFrequencies(Long bookId) {
        List<DiscussionPost> posts;
        if (bookId != null) {
            posts = postRepository.findByBookIdOrderByChapter_ChapterNumberAscPublishedAtAsc(bookId);
        } else {
            posts = postRepository.findAll();
        }
        Map<String, Long> frequencies = new HashMap<>();
        for (DiscussionPost post : posts) {
            for (Quote quote : post.getQuotes()) {
                for (Theme theme : quote.getThemes()) {
                    frequencies.merge(theme.getName(), 1L, Long::sum);
                }
            }
        }
        return frequencies;
    }

    public Map<String, Long> getAllThemeFrequencies() {
        return getThemeFrequencies(null);
    }

    public List<String> detectQuotes(String text) {
        List<String> quotes = new ArrayList<>();
        if (text == null || text.isEmpty()) return quotes;

        String normalized = text.replace('\u201C', '"').replace('\u201D', '"');
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(normalized);

        while (matcher.find()) {
            String quote = matcher.group(1).trim();
            if (quote.split("\\s+").length > 2) {
                quotes.add(quote);
            }
        }
        return quotes;
    }

    private String normalizeQuote(String quote) {
        return quote.trim().replaceAll("\\s+", " ");
    }

    public static class QuoteEntry {
        private String text;
        private List<String> themeNames;

        public QuoteEntry() {}

        public QuoteEntry(String text, List<String> themeNames) {
            this.text = text;
            this.themeNames = themeNames;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<String> getThemeNames() { return themeNames; }
        public void setThemeNames(List<String> themeNames) { this.themeNames = themeNames; }
    }
}
