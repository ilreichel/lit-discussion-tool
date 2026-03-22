package com.litdiscussion.model;

import jakarta.persistence.*;

@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer chapterNumber;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    public Chapter() {}

    public Chapter(Integer chapterNumber, String title, Book book) {
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.book = book;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
}
