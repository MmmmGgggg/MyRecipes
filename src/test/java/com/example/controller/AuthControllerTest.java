package com.example.controller;

import com.example.model.User;
import com.example.repository.RecipeRepository;
import com.example.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepo;
    @Autowired private RecipeRepository recipeRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        recipeRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "user@example.com",
                        "password", "password123",
                        "name", "Test User"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setName("Existing");
        userRepo.save(user);

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "user@example.com",
                        "password", "password123",
                        "name", "New User"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    void shouldRejectRegisterWithoutEmail() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("password", "password123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginWithCorrectCredentials() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setName("Test User");
        userRepo.save(user);

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "user@example.com",
                        "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setName("Test User");
        userRepo.save(user);

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "user@example.com",
                        "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void shouldRejectNonExistentEmail() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "nobody@example.com",
                        "password", "password123"))))
                .andExpect(status().isUnauthorized());
    }
}
