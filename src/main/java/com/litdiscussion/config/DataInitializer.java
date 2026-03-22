package com.litdiscussion.config;

import com.litdiscussion.model.*;
import com.litdiscussion.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final ThemeRepository themeRepository;
    private final DiscussionPostRepository postRepository;
    private final QuoteRepository quoteRepository;

    public DataInitializer(UserRepository userRepository,
                           ClassroomRepository classroomRepository,
                           BookRepository bookRepository,
                           ChapterRepository chapterRepository,
                           ThemeRepository themeRepository,
                           DiscussionPostRepository postRepository,
                           QuoteRepository quoteRepository) {
        this.userRepository = userRepository;
        this.classroomRepository = classroomRepository;
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.themeRepository = themeRepository;
        this.postRepository = postRepository;
        this.quoteRepository = quoteRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByUsername("student1").isPresent()) {
            return;
        }

        User student1 = new User("student1", "Student 1", "student");
        student1 = userRepository.save(student1);

        Classroom introLit = new Classroom("Intro to English Lit",
                "An introduction to English literature through classic and modern works.",
                "LIT101");
        introLit = classroomRepository.save(introLit);
        introLit.getStudents().add(student1);

        Book braveNewWorld = new Book("Brave New World", "Aldous Huxley",
                "A dystopian novel set in a futuristic World State.");
        braveNewWorld = bookRepository.save(braveNewWorld);

        introLit.getBooks().add(braveNewWorld);
        classroomRepository.save(introLit);

        String[] chapterTitles = {
            "Chapter 1", "Chapter 2", "Chapter 3", "Chapter 4", "Chapter 5",
            "Chapter 6", "Chapter 7", "Chapter 8", "Chapter 9", "Chapter 10",
            "Chapter 11", "Chapter 12", "Chapter 13", "Chapter 14", "Chapter 15",
            "Chapter 16", "Chapter 17", "Chapter 18"
        };

        for (int i = 0; i < chapterTitles.length; i++) {
            Chapter chapter = new Chapter(i + 1, chapterTitles[i], braveNewWorld);
            chapterRepository.save(chapter);
        }

        Theme dystopia = themeRepository.save(new Theme("Dystopia", "system"));
        Theme control = themeRepository.save(new Theme("Social Control", "system"));
        Theme identity = themeRepository.save(new Theme("Identity", "system"));
        Theme freedom = themeRepository.save(new Theme("Freedom", "system"));
        Theme technology = themeRepository.save(new Theme("Technology", "system"));
        Theme conditioning = themeRepository.save(new Theme("Conditioning", "system"));
        Theme happiness = themeRepository.save(new Theme("Happiness", "system"));
        Theme individuality = themeRepository.save(new Theme("Individuality", "system"));

        Chapter ch1 = chapterRepository.findByBookIdAndChapterNumber(braveNewWorld.getId(), 1).orElseThrow();
        Chapter ch2 = chapterRepository.findByBookIdAndChapterNumber(braveNewWorld.getId(), 2).orElseThrow();
        Chapter ch3 = chapterRepository.findByBookIdAndChapterNumber(braveNewWorld.getId(), 3).orElseThrow();
        Chapter ch4 = chapterRepository.findByBookIdAndChapterNumber(braveNewWorld.getId(), 4).orElseThrow();
        Chapter ch5 = chapterRepository.findByBookIdAndChapterNumber(braveNewWorld.getId(), 5).orElseThrow();

        createSamplePost(student1, introLit, braveNewWorld, ch1,
                "The opening of the novel immediately establishes the sterile, controlled environment of the Central London Hatchery. The Bokanovsky Process is described as \"one of the major instruments of social stability\" which shows how science is weaponized against individuality.",
                "\"one of the major instruments of social stability\"", "Dystopia", "Social Control");

        createSamplePost(student1, introLit, braveNewWorld, ch1,
                "The caste system is fascinating - Alphas, Betas, Gammas, Deltas, and Epsilons. Each group is conditioned from birth to accept their role. The Director says \"That is the secret of happiness and virtue - liking what you've got to do.\"",
                "\"That is the secret of happiness and virtue - liking what you've got to do\"", "Conditioning", "Happiness");

        createSamplePost(student1, introLit, braveNewWorld, ch2,
                "Hypnopaedia - sleep teaching - is used to instill moral lessons. The repetition of \"Everyone belongs to everyone else\" shows how language is used as a tool of social control.",
                "\"Everyone belongs to everyone else\"", "Social Control", "Identity");

        createSamplePost(student1, introLit, braveNewWorld, ch3,
                "The juxtaposition of the Savage Reservation and the World State becomes clearer. Mustapha Mond's lecture about \"history is bunk\" reveals how the state suppresses knowledge of the past to maintain control.",
                "\"history is bunk\"", "Dystopia", "Freedom");

        createSamplePost(student1, introLit, braveNewWorld, ch4,
                "Bernard Marx is presented as an outsider - too small for an Alpha, too individualistic for the society. His feeling of being different, \"What's the point of truth or beauty or knowledge when the anthrax bombs are popping all around?\" captures the conflict between individual thought and collective comfort.",
                "\"What's the point of truth or beauty or knowledge when the anthrax bombs are popping all around\"", "Identity", "Individuality", "Freedom");

        createSamplePost(student1, introLit, braveNewWorld, ch5,
                "Lenina's relationship with Henry Foster shows how casual and mechanized human relationships have become. The phrase \"Everyone is happy now\" rings hollow when we see how happiness is manufactured through soma and conditioning.",
                "\"Everyone is happy now\"", "Happiness", "Social Control", "Technology");

        createSamplePost(student1, introLit, braveNewWorld, ch2,
                "The conditioning of infants through Neo-Pavlovian conditioning is disturbing. The way Delta babies are conditioned to hate books and flowers with electric shocks shows \"the principle of making people like their inescapable social destiny.\"",
                "\"the principle of making people like their inescapable social destiny\"", "Conditioning", "Dystopia");

        createSamplePost(student1, introLit, braveNewWorld, ch3,
                "Mond's explanation that \"stability isn't nearly so spectacular as instability\" reveals the trade-off the World State has made - sacrificing art, passion, and truth for comfort and predictability.",
                "\"stability isn't nearly so spectacular as instability\"", "Social Control", "Technology", "Dystopia");

        createSamplePostWithQuotes(student1, introLit, braveNewWorld, ch1,
                "The opening chapter masterfully weaves together multiple dystopian themes. The Director boasts that the Bokanovsky Process is \"one of the major instruments of social stability\" which immediately frames science as a tool of oppression. Meanwhile, the hypnopaedic slogan \"everyone belongs to everyone else\" erases the concept of personal bonds, reducing human connection to a collective mandate.",
                java.util.Map.entry("\"one of the major instruments of social stability\"",
                        new String[]{"Dystopia", "Social Control"}),
                java.util.Map.entry("\"everyone belongs to everyone else\"",
                        new String[]{"Identity", "Conditioning"})
        );
    }

    private void createSamplePost(User author, Classroom classroom, Book book, Chapter chapter,
                                  String content, String quoteText, String... themeNames) {
        DiscussionPost post = new DiscussionPost(author, classroom, book, chapter, content);
        post.setPublishedAt(LocalDateTime.now().minusDays((long) (Math.random() * 30)));
        post = postRepository.save(post);

        Quote quote = new Quote(quoteText, post);
        for (String themeName : themeNames) {
            Theme theme = themeRepository.findByName(themeName).orElseThrow();
            quote.getThemes().add(theme);
        }
        quoteRepository.save(quote);
    }

    @SafeVarargs
    private void createSamplePostWithQuotes(User author, Classroom classroom, Book book, Chapter chapter,
                                            String content, java.util.Map.Entry<String, String[]>... quotes) {
        DiscussionPost post = new DiscussionPost(author, classroom, book, chapter, content);
        post.setPublishedAt(LocalDateTime.now().minusDays((long) (Math.random() * 30)));
        post = postRepository.save(post);

        for (java.util.Map.Entry<String, String[]> entry : quotes) {
            Quote quote = new Quote(entry.getKey(), post);
            for (String themeName : entry.getValue()) {
                Theme theme = themeRepository.findByName(themeName).orElseThrow();
                quote.getThemes().add(theme);
            }
            quoteRepository.save(quote);
        }
    }
}
