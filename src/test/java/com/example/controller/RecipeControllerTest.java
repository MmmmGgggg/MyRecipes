package com.example.controller;

import com.example.model.*;
import com.example.repository.IngredientRepository;
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
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        recipeRepo.deleteAll();
        ingredientRepo.deleteAll();
        userRepo.deleteAll();

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setName("Test User");
        testUser = userRepo.save(testUser);

        token = jwtUtil.generateToken("test@example.com");
    }

    private Recipe createRecipe(String name, String creator, String language, Visibility visibility) {
        Recipe r = new Recipe();
        r.setName(name);
        r.setInstructions("Test instructions");
        r.setCreator(creator);
        r.setLanguage(language);
        r.setVisibility(visibility);
        r.setUser(testUser);
        return r;
    }

    @Test
    void shouldCreateRecipeWhenAuthenticated() throws Exception {
        String json = """
                {"name": "Pasta", "creator": "John", "language": "Italian", "visibility": "PUBLIC"}
                """;

        mvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pasta"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    void shouldRejectCreateWithoutAuth() throws Exception {
        String json = """
                {"name": "Pasta", "creator": "John", "language": "Italian"}
                """;

        mvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateRecipeWithIngredients() throws Exception {
        String json = """
                {
                    "name": "Pasta",
                    "instructions": "Cook it",
                    "visibility": "PUBLIC",
                    "recipeIngredients": [
                        {"ingredient": {"name": "spaghetti"}, "amount": "200g"},
                        {"ingredient": {"name": "eggs"}, "amount": "3"}
                    ]
                }
                """;

        mvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeIngredients", hasSize(2)))
                .andExpect(jsonPath("$.recipeIngredients[0].ingredient.name").value("spaghetti"))
                .andExpect(jsonPath("$.recipeIngredients[0].amount").value("200g"));
    }

    @Test
    void shouldGetPublicRecipesWithoutAuth() throws Exception {
        recipeRepo.save(createRecipe("Pasta", "John", "Italian", Visibility.PUBLIC));
        recipeRepo.save(createRecipe("Secret Tacos", "Maria", "Spanish", Visibility.PRIVATE));

        mvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Pasta"));
    }

    @Test
    void shouldGetPublicAndOwnRecipesWhenAuthenticated() throws Exception {
        recipeRepo.save(createRecipe("Pasta", "John", "Italian", Visibility.PUBLIC));
        recipeRepo.save(createRecipe("Secret Tacos", "Maria", "Spanish", Visibility.PRIVATE));

        mvc.perform(get("/api/recipes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldGetMyRecipes() throws Exception {
        recipeRepo.save(createRecipe("Pasta", "John", "Italian", Visibility.PUBLIC));
        recipeRepo.save(createRecipe("Secret Tacos", "Maria", "Spanish", Visibility.PRIVATE));

        mvc.perform(get("/api/recipes/mine")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldGetPublicRecipeByIdWithoutAuth() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Pasta", "John", "Italian", Visibility.PUBLIC));

        mvc.perform(get("/api/recipes/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pasta"));
    }

    @Test
    void shouldNotGetPrivateRecipeWithoutAuth() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Secret", "John", "Italian", Visibility.PRIVATE));

        mvc.perform(get("/api/recipes/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetPrivateRecipeAsOwner() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Secret", "John", "Italian", Visibility.PRIVATE));

        mvc.perform(get("/api/recipes/" + saved.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Secret"));
    }

    @Test
    void shouldReturn404ForMissingRecipe() throws Exception {
        mvc.perform(get("/api/recipes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetDistinctCreators() throws Exception {
        recipeRepo.save(createRecipe("Pasta", "John", "Italian", Visibility.PUBLIC));
        recipeRepo.save(createRecipe("Tacos", "Maria", "Spanish", Visibility.PUBLIC));
        recipeRepo.save(createRecipe("Pizza", "John", "Italian", Visibility.PUBLIC));

        mvc.perform(get("/api/recipes/creators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("John", "Maria")));
    }

    @Test
    void shouldUpdateOwnRecipe() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Pasta", "John", "Italian", Visibility.PUBLIC));

        String json = """
                {"name": "Pasta Carbonara", "creator": "John", "language": "Italian", "visibility": "PUBLIC"}
                """;

        mvc.perform(put("/api/recipes/" + saved.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pasta Carbonara"));
    }

    @Test
    void shouldNotUpdateOtherUsersRecipe() throws Exception {
        User other = new User();
        other.setEmail("other@example.com");
        other.setPassword(passwordEncoder.encode("password"));
        other.setName("Other");
        other = userRepo.save(other);

        Recipe recipe = createRecipe("Pasta", "Other", "Italian", Visibility.PUBLIC);
        recipe.setUser(other);
        Recipe saved = recipeRepo.save(recipe);

        String json = """
                {"name": "Hacked", "creator": "Hacker", "language": "Italian"}
                """;

        mvc.perform(put("/api/recipes/" + saved.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingMissingRecipe() throws Exception {
        String json = """
                {"name": "X", "creator": "Y", "language": "Z"}
                """;

        mvc.perform(put("/api/recipes/999")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteOwnRecipe() throws Exception {
        Recipe saved = recipeRepo.save(createRecipe("Pasta", "John", "Italian", Visibility.PUBLIC));

        mvc.perform(delete("/api/recipes/" + saved.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/recipes/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotDeleteOtherUsersRecipe() throws Exception {
        User other = new User();
        other.setEmail("other@example.com");
        other.setPassword(passwordEncoder.encode("password"));
        other.setName("Other");
        other = userRepo.save(other);

        Recipe recipe = createRecipe("Pasta", "Other", "Italian", Visibility.PUBLIC);
        recipe.setUser(other);
        Recipe saved = recipeRepo.save(recipe);

        mvc.perform(delete("/api/recipes/" + saved.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingMissingRecipe() throws Exception {
        mvc.perform(delete("/api/recipes/999")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDefaultVisibilityToPrivate() throws Exception {
        String json = """
                {"name": "Pasta", "creator": "John", "language": "Italian"}
                """;

        mvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test
    void shouldLinkRecipeToLoggedInUser() throws Exception {
        String json = """
                {"name": "Pasta", "instructions": "Cook it", "visibility": "PUBLIC"}
                """;

        mvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void shouldSeeOtherUsersPublicRecipesWhenLoggedIn() throws Exception {
        User other = new User();
        other.setEmail("other@example.com");
        other.setPassword(passwordEncoder.encode("password"));
        other.setName("Other");
        other = userRepo.save(other);

        Recipe otherPublic = createRecipe("Other Public", "Other", "Italian", Visibility.PUBLIC);
        otherPublic.setUser(other);
        recipeRepo.save(otherPublic);

        Recipe otherPrivate = createRecipe("Other Private", "Other", "Italian", Visibility.PRIVATE);
        otherPrivate.setUser(other);
        recipeRepo.save(otherPrivate);

        recipeRepo.save(createRecipe("My Private", "Me", "Italian", Visibility.PRIVATE));

        mvc.perform(get("/api/recipes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Other Public", "My Private")));
    }

    @Test
    void shouldNotSeeOtherUsersPrivateRecipesWhenLoggedIn() throws Exception {
        User other = new User();
        other.setEmail("other@example.com");
        other.setPassword(passwordEncoder.encode("password"));
        other.setName("Other");
        other = userRepo.save(other);

        Recipe otherPrivate = createRecipe("Secret Recipe", "Other", "Italian", Visibility.PRIVATE);
        otherPrivate.setUser(other);
        recipeRepo.save(otherPrivate);

        mvc.perform(get("/api/recipes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldSeePublicRecipesWithNoOwnerWhenLoggedIn() throws Exception {
        Recipe orphan = new Recipe();
        orphan.setName("Old Recipe");
        orphan.setInstructions("Test");
        orphan.setVisibility(Visibility.PUBLIC);
        recipeRepo.save(orphan);

        mvc.perform(get("/api/recipes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Old Recipe"));
    }
}
