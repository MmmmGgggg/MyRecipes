package com.example.controller;

import com.example.model.Ingredient;
import com.example.repository.IngredientRepository;
import com.example.repository.RecipeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IngredientControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private IngredientRepository ingredientRepo;
    @Autowired private RecipeRepository recipeRepo;

    @BeforeEach
    void setUp() {
        recipeRepo.deleteAll();
        ingredientRepo.deleteAll();
    }

    @Test
    void shouldCreateIngredient() throws Exception {
        mvc.perform(post("/api/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Ingredient("salt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("salt"));
    }

    @Test
    void shouldNotDuplicateIngredient() throws Exception {
        ingredientRepo.save(new Ingredient("salt"));

        mvc.perform(post("/api/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Ingredient("Salt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("salt"));

        mvc.perform(get("/api/ingredients"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldGetAllIngredients() throws Exception {
        ingredientRepo.save(new Ingredient("salt"));
        ingredientRepo.save(new Ingredient("pepper"));

        mvc.perform(get("/api/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldSearchIngredients() throws Exception {
        ingredientRepo.save(new Ingredient("salt"));
        ingredientRepo.save(new Ingredient("pepper"));
        ingredientRepo.save(new Ingredient("salmon"));

        mvc.perform(get("/api/ingredients?search=sal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("salt", "salmon")));
    }
}
