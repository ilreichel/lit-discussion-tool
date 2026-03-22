package com.litdiscussion.repository;

import com.litdiscussion.model.DiscussionPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {
    List<DiscussionPost> findByBookIdOrderByChapter_ChapterNumberAscPublishedAtAsc(Long bookId);
    List<DiscussionPost> findByClassroomIdOrderByChapter_ChapterNumberAscPublishedAtAsc(Long classroomId);
    List<DiscussionPost> findByChapterIdOrderByPublishedAtAsc(Long chapterId);

    @Query("SELECT dp FROM DiscussionPost dp WHERE dp.id IN (SELECT dp2.id FROM DiscussionPost dp2 JOIN dp2.quotes q JOIN q.themes t WHERE t.name = :themeName) ORDER BY dp.chapter.chapterNumber ASC, dp.publishedAt ASC")
    List<DiscussionPost> findByThemeNameOrderByChapterAndTime(@Param("themeName") String themeName);

    @Query("SELECT dp FROM DiscussionPost dp WHERE dp.book.id = :bookId AND dp.id IN (SELECT dp2.id FROM DiscussionPost dp2 JOIN dp2.quotes q JOIN q.themes t WHERE t.name = :themeName) ORDER BY dp.chapter.chapterNumber ASC, dp.publishedAt ASC")
    List<DiscussionPost> findByThemeNameAndBookIdOrderByChapterAndTime(@Param("themeName") String themeName, @Param("bookId") Long bookId);
}
