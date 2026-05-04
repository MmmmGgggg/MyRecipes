package com.example.controller;

import com.example.model.*;
import com.example.repository.FavoriteRepository;
import com.example.repository.RecipeRepository;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FavoriteControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private FavoriteRepository favoriteRepo;
    @Autowired private RecipeRepository recipeRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;
    private User testUser;
    private Recipe testRecipe;

    @BeforeEach
    void setUp() {
        favoriteRepo.deleteAll();
        recipeRepo.deleteAll();
        userRepo.deleteAll();

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setName("Test User");
        testUser = userRepo.save(testUser);

        token = jwtUtil.generateToken("test@example.com");

        testRecipe = new Recipe();
        testRecipe.setName("Test Recipe");
        testRecipe.setInstructions("Test");
        testRecipe.setVisibility(Visibility.PUBLIC);
        testRecipe.setUser(testUser);
        testRecipe = recipeRepo.save(testRecipe);
    }

    @Test
    void shouldAddFavorite() throws Exception {
        mvc.perform(post("/api/favorites/" + testRecipe.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Test Recipe"));
    }

    @Test
    void shouldRemoveFavorite() throws Exception {
        mvc.perform(post("/api/favorites/" + testRecipe.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/favorites/" + testRecipe.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldCheckIsFavorite() throws Exception {
        mvc.perform(get("/api/favorites/" + testRecipe.getId() + "/check")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        mvc.perform(post("/api/favorites/" + testRecipe.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/favorites/" + testRecipe.getId() + "/check")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void shouldNotDuplicateFavorite() throws Exception {
        mvc.perform(post("/api/favorites/" + testRecipe.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/favorites/" + testRecipe.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldReturn404ForNonExistentRecipe() throws Exception {
        mvc.perform(post("/api/favorites/999")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectWithoutAuth() throws Exception {
        mvc.perform(post("/api/favorites/" + testRecipe.getId()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/favorites"))
                .andExpect(status().isForbidden());
    }
}
