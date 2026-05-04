package com.example.controller;

import com.example.model.*;
import com.example.repository.IngredientRepository;
import com.example.repository.RecipeRepository;
import com.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeRepository recipeRepo;
    private final IngredientRepository ingredientRepo;
    private final UserRepository userRepo;

    public RecipeController(RecipeRepository recipeRepo, IngredientRepository ingredientRepo,
                            UserRepository userRepo) {
        this.recipeRepo = recipeRepo;
        this.ingredientRepo = ingredientRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Recipe> getAll(@RequestParam(required = false) String search,
                               @RequestParam(required = false) List<Long> ingredientIds,
                               @RequestParam(required = false) String creator,
                               @RequestParam(required = false) List<String> tags,
                               Authentication auth) {
        List<Recipe> recipes;

        if (auth != null) {
            recipes = recipeRepo.findAccessibleByUser(auth.getName());
        } else {
            recipes = recipeRepo.findByVisibility(Visibility.PUBLIC);
        }

        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            recipes = recipes.stream()
                    .filter(r -> r.getName().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }

        if (creator != null && !creator.isBlank()) {
            String lower = creator.toLowerCase();
            recipes = recipes.stream()
                    .filter(r -> r.getCreator() != null && r.getCreator().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }

        if (tags != null && !tags.isEmpty()) {
            recipes = recipes.stream()
                    .filter(r -> r.getTags() != null && r.getTags().containsAll(tags))
                    .collect(Collectors.toList());
        }

        if (ingredientIds != null && !ingredientIds.isEmpty()) {
            recipes = recipes.stream()
                    .filter(r -> {
                        List<Long> recipeIngIds = r.getRecipeIngredients().stream()
                                .map(ri -> ri.getIngredient().getId())
                                .toList();
                        return recipeIngIds.containsAll(ingredientIds);
                    })
                    .collect(Collectors.toList());
        }

        return recipes.stream()
                .sorted(Comparator.comparing(Recipe::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @GetMapping("/creators")
    public List<String> getCreators() {
        return recipeRepo.findDistinctCreators();
    }

    @GetMapping("/tags")
    public List<String> getTags() {
        return recipeRepo.findDistinctTags();
    }

    @GetMapping("/mine")
    public List<Recipe> getMyRecipes(Authentication auth) {
        return recipeRepo.findByUserEmail(auth.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getById(@PathVariable Long id, Authentication auth) {
        return recipeRepo.findById(id)
                .filter(recipe -> recipe.getVisibility() == Visibility.PUBLIC
                        || (auth != null && recipe.getUser() != null && recipe.getUser().getEmail().equals(auth.getName())))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Recipe create(@RequestBody Recipe recipe, Authentication auth) {
        userRepo.findByEmail(auth.getName()).ifPresent(user -> {
            recipe.setUser(user);
            if (recipe.getCreator() == null || recipe.getCreator().isBlank()) {
                recipe.setCreator(user.getName());
            }
        });
        if (recipe.getVisibility() == null) recipe.setVisibility(Visibility.PRIVATE);
        resolveIngredients(recipe);
        return recipeRepo.save(recipe);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> update(@PathVariable Long id, @RequestBody Recipe recipe, Authentication auth) {
        return recipeRepo.findById(id)
                .filter(existing -> existing.getUser().getEmail().equals(auth.getName()))
                .map(existing -> {
                    existing.setName(recipe.getName());
                    existing.setInstructions(recipe.getInstructions());
                    existing.setImageUrl(recipe.getImageUrl());
                    existing.setCreator(recipe.getCreator());
                    existing.setLanguage(recipe.getLanguage());
                    existing.setServings(recipe.getServings());
                    existing.setPrepTimeMinutes(recipe.getPrepTimeMinutes());
                    existing.setCookTimeMinutes(recipe.getCookTimeMinutes());
                    existing.setVisibility(recipe.getVisibility() != null ? recipe.getVisibility() : existing.getVisibility());
                    existing.setTags(recipe.getTags() != null ? recipe.getTags() : existing.getTags());
                    existing.getRecipeIngredients().clear();
                    resolveIngredients(recipe);
                    existing.getRecipeIngredients().addAll(recipe.getRecipeIngredients());
                    existing.getRecipeIngredients().forEach(ri -> ri.setRecipe(existing));
                    return ResponseEntity.ok(recipeRepo.save(existing));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        return recipeRepo.findById(id)
                .filter(recipe -> recipe.getUser().getEmail().equals(auth.getName()))
                .map(recipe -> {
                    recipeRepo.delete(recipe);
                    return ResponseEntity.noContent().<Void>build();
                }).orElse(ResponseEntity.notFound().build());
    }

    private void resolveIngredients(Recipe recipe) {
        for (RecipeIngredient ri : recipe.getRecipeIngredients()) {
            ri.setRecipe(recipe);
            Ingredient ing = ri.getIngredient();
            if (ing != null && ing.getName() != null) {
                String normalized = ing.getName().trim().toLowerCase();
                ri.setIngredient(ingredientRepo.findByNameIgnoreCase(normalized)
                        .orElseGet(() -> ingredientRepo.save(new Ingredient(normalized))));
            }
        }
    }
}
