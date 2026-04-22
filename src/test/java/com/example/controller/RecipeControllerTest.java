package com.example.controller;

import com.example.model.Ingredient;
import com.example.model.Recipe;
import com.example.model.RecipeIngredient;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private RecipeRepository recipeRepo;
    @Autowired private IngredientRepository ingredientRepo;

    @BeforeEach
    void setUp() {
        recipeRepo.deleteAll();
        ingredientRepo.deleteAll();
    }

    private Recipe createRecipe(String name, String creator, String language) {
        Recipe r = new Recipe();
        r.setName(name);
        r.setInstructions("Test instructions");
        r.setCreator(creator);
        r.setLanguage(language);
        return r;
    }

    @Test
    void shouldCreateRecipe() throws Exception {
        Recipe recipe = createRecipe("Pasta", "John", "Italian");

        mvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(recipe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pasta"))
                .andExpect(jsonPath("$.creator").value("John"))
                .andExpect(jsonPath("$.language").value("Italian"));
    }

    @Test
    void shouldCreateRecipeWithIngredients() throws Exception {
        String json = """
                {
                    "name": "Pasta",
                    "instructions": "Cook it",
                    "recipeIngredients": [
                        {"ingredient": {"name": "spaghetti"}, "amount": "200g"},
                        {"ingredient": {"name": "eggs"}, "amount": "3"}
                    ]
                }
                """;

        mvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeIngredients", hasSize(2)))
                .andExpect(jsonPath("$.recipeIngredients[0].ingredient.name").value("spaghetti"))
                .andExpect(jsonPath("$.recipeIngredients[0].amount").value("200g"));
    }

    @Test
    void shouldGetAllRecipes() throws Exception {
        recipeRepo.save(createRecipe("Pasta", "John", "Italian"));
        recipeRepo.save(createRecipe("Tacos", "Maria", "Spanish"));

        mvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldGetRecipeById() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Pasta", "John", "Italian"));

        mvc.perform(get("/api/recipes/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pasta"));
    }

    @Test
    void shouldReturn404ForMissingRecipe() throws Exception {
        mvc.perform(get("/api/recipes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSearchByName() throws Exception {
        recipeRepo.save(createRecipe("Pasta Carbonara", "John", "Italian"));
        recipeRepo.save(createRecipe("Tacos", "Maria", "Spanish"));

        mvc.perform(get("/api/recipes?search=pasta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Pasta Carbonara"));
    }

    @Test
    void shouldFilterByCreator() throws Exception {
        recipeRepo.save(createRecipe("Pasta", "John", "Italian"));
        recipeRepo.save(createRecipe("Tacos", "Maria", "Spanish"));

        mvc.perform(get("/api/recipes?creator=Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].creator").value("Maria"));
    }

    @Test
    void shouldFilterByIngredient() throws Exception {
        Ingredient egg = ingredientRepo.save(new Ingredient("egg"));

        Recipe pasta = createRecipe("Pasta", "John", "Italian");
        pasta = recipeRepo.save(pasta);
        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipe(pasta);
        ri.setIngredient(egg);
        ri.setAmount("3");
        pasta.getRecipeIngredients().add(ri);
        recipeRepo.save(pasta);

        recipeRepo.save(createRecipe("Salad", "Maria", "English"));

        mvc.perform(get("/api/recipes?ingredientId=" + egg.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Pasta"));
    }

    @Test
    void shouldGetDistinctCreators() throws Exception {
        recipeRepo.save(createRecipe("Pasta", "John", "Italian"));
        recipeRepo.save(createRecipe("Tacos", "Maria", "Spanish"));
        recipeRepo.save(createRecipe("Pizza", "John", "Italian"));

        mvc.perform(get("/api/recipes/creators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("John", "Maria")));
    }

    @Test
    void shouldUpdateRecipe() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Pasta", "John", "Italian"));

        Recipe updated = createRecipe("Pasta Carbonara", "John", "Italian");

        mvc.perform(put("/api/recipes/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pasta Carbonara"));
    }

    @Test
    void shouldReturn404WhenUpdatingMissingRecipe() throws Exception {
        mvc.perform(put("/api/recipes/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createRecipe("X", "Y", "Z"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteRecipe() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Pasta", "John", "Italian"));

        mvc.perform(delete("/api/recipes/" + saved.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/recipes/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingMissingRecipe() throws Exception {
        mvc.perform(delete("/api/recipes/999"))
                .andExpect(status().isNotFound());
    }
}
