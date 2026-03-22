package com.litdiscussion.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_post_id", nullable = false)
    private DiscussionPost discussionPost;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "quote_themes",
        joinColumns = @JoinColumn(name = "quote_id"),
        inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    private List<Theme> themes = new ArrayList<>();

    public Quote() {}

    public Quote(String text, DiscussionPost discussionPost) {
        this.text = text;
        this.discussionPost = discussionPost;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public DiscussionPost getDiscussionPost() { return discussionPost; }
    public void setDiscussionPost(DiscussionPost discussionPost) { this.discussionPost = discussionPost; }
    public List<Theme> getThemes() { return themes; }
    public void setThemes(List<Theme> themes) { this.themes = themes; }
}
