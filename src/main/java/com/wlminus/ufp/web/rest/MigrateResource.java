package com.wlminus.ufp.web.rest;

import com.wlminus.ufp.domain.Authority;
import com.wlminus.ufp.domain.Student;
import com.wlminus.ufp.domain.User;
import com.wlminus.ufp.repository.AuthorityRepository;
import com.wlminus.ufp.repository.StudentRepository;
import com.wlminus.ufp.repository.UserRepository;
import com.wlminus.ufp.security.AuthoritiesConstants;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/migrate")
public class MigrateResource {
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private StudentRepository studentRepository;

    public MigrateResource(StudentRepository studentRepository, PasswordEncoder passwordEncoder, AuthorityRepository authorityRepository, UserRepository userRepository, CacheManager cacheManager) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    @GetMapping("/user")
    public String getAllStudents() {
        System.out.println("REST request to migrate user for Students");
        List<Student> dataStudent = studentRepository.findAll();
//      Student stu = new Student("20143568", "Quang", "11.06.1996", "TT2-K59", "ToanTin", null);
        for (Student stu : dataStudent) {
            User newUser = new User();

            String encryptedPassword = passwordEncoder.encode(stu.getStudentCode());
            newUser.setLogin(stu.getStudentCode());
            newUser.setPassword(encryptedPassword);
            newUser.setLastName(stu.getName());
            newUser.setEmail(stu.getStudentCode() + "@" + "student.hust.edu.vn");
            newUser.setLangKey("en");
            newUser.setActivated(true);
            newUser.setActivationKey(null);
            Set<Authority> authorities = new HashSet<>();
            authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
            newUser.setAuthorities(authorities);
            newUser.setStudent(stu);

            User newUserCreated = userRepository.save(newUser);
            this.clearUserCaches(newUser);
        }
        return "Import data done";
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evict(user.getEmail());
    }
}
