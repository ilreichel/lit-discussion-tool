package com.litdiscussion.service;

import com.litdiscussion.model.*;
import com.litdiscussion.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClassService {

    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public ClassService(ClassroomRepository classroomRepository,
                        UserRepository userRepository,
                        BookRepository bookRepository) {
        this.classroomRepository = classroomRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public User login(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public List<Classroom> getClassesForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return classroomRepository.findByStudents_Id(user.getId());
    }

    public Classroom getClassroomById(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Classroom not found: " + id));
    }

    public List<Book> getBooksForClassroom(Long classroomId) {
        Classroom classroom = getClassroomById(classroomId);
        return classroom.getBooks();
    }
}
