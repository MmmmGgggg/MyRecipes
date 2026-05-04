package com.example.controller;

import com.example.model.User;
import com.example.model.Recipe;
import com.example.model.Visibility;
import com.example.repository.FavoriteRepository;
import com.example.repository.RecipeRepository;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepo;
    @Autowired private RecipeRepository recipeRepo;
    @Autowired private FavoriteRepository favoriteRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        favoriteRepo.deleteAll();
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

    @Test
    void shouldDeleteAccountAndAllRecipes() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setName("Test User");
        user = userRepo.save(user);

        Recipe recipe = new Recipe();
        recipe.setName("My Recipe");
        recipe.setInstructions("Test");
        recipe.setVisibility(Visibility.PUBLIC);
        recipe.setUser(user);
        recipeRepo.save(recipe);

        String token = jwtUtil.generateToken("user@example.com");

        mvc.perform(delete("/api/auth/account")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted"));

        mvc.perform(get("/api/recipes"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldRejectDeleteWithoutAuth() throws Exception {
        mvc.perform(delete("/api/auth/account"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteOtherUsersFavoritesWhenAccountDeleted() throws Exception {
        User userB = new User();
        userB.setEmail("userb@example.com");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setName("User B");
        userB = userRepo.save(userB);

        Recipe recipe = new Recipe();
        recipe.setName("B's Recipe");
        recipe.setInstructions("Test");
        recipe.setVisibility(Visibility.PUBLIC);
        recipe.setUser(userB);
        recipe = recipeRepo.save(recipe);

        // User A favorites User B's recipe
        User userA = new User();
        userA.setEmail("usera@example.com");
        userA.setPassword(passwordEncoder.encode("password"));
        userA.setName("User A");
        userA = userRepo.save(userA);

        String tokenA = jwtUtil.generateToken("usera@example.com");
        mvc.perform(post("/api/favorites/" + recipe.getId())
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // User B deletes their account
        String tokenB = jwtUtil.generateToken("userb@example.com");
        mvc.perform(delete("/api/auth/account")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // User A's favorites should be empty
        mvc.perform(get("/api/favorites")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
