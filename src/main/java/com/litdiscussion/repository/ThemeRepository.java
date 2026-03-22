package com.litdiscussion.repository;

import com.litdiscussion.model.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByName(String name);
    List<Theme> findByNameIn(List<String> names);
    List<Theme> findAllByOrderByNameAsc();
}
