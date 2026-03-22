package com.litdiscussion.repository;

import com.litdiscussion.model.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findByAccessCode(String accessCode);
    List<Classroom> findByStudents_Id(Long userId);
}
