package com.example.controller;

import com.example.model.Ingredient;
import com.example.model.Recipe;
import com.example.model.RecipeIngredient;
import com.example.repository.IngredientRepository;
import com.example.repository.RecipeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin
public class RecipeController {

    private final RecipeRepository recipeRepo;
    private final IngredientRepository ingredientRepo;

    public RecipeController(RecipeRepository recipeRepo, IngredientRepository ingredientRepo) {
        this.recipeRepo = recipeRepo;
        this.ingredientRepo = ingredientRepo;
    }

    @GetMapping
    public List<Recipe> getAll(@RequestParam(required = false) String search,
                               @RequestParam(required = false) Long ingredientId,
                               @RequestParam(required = false) String creator) {
        if (ingredientId != null) return recipeRepo.findByIngredientId(ingredientId);
        if (creator != null) return recipeRepo.findByCreatorContainingIgnoreCase(creator);
        if (search != null) return recipeRepo.findByNameContainingIgnoreCase(search);
        return recipeRepo.findAll();
    }

    @GetMapping("/creators")
    public List<String> getCreators() {
        return recipeRepo.findDistinctCreators();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getById(@PathVariable Long id) {
        return recipeRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Recipe create(@RequestBody Recipe recipe) {
        resolveIngredients(recipe);
        return recipeRepo.save(recipe);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> update(@PathVariable Long id, @RequestBody Recipe recipe) {
        return recipeRepo.findById(id).map(existing -> {
            existing.setName(recipe.getName());
            existing.setInstructions(recipe.getInstructions());
            existing.setImageUrl(recipe.getImageUrl());
            existing.setCreator(recipe.getCreator());
            existing.setLanguage(recipe.getLanguage());
            existing.getRecipeIngredients().clear();
            resolveIngredients(recipe);
            existing.getRecipeIngredients().addAll(recipe.getRecipeIngredients());
            existing.getRecipeIngredients().forEach(ri -> ri.setRecipe(existing));
            return ResponseEntity.ok(recipeRepo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!recipeRepo.existsById(id)) return ResponseEntity.notFound().build();
        recipeRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void resolveIngredients(Recipe recipe) {
        for (RecipeIngredient ri : recipe.getRecipeIngredients()) {
            ri.setRecipe(recipe);
            Ingredient ing = ri.getIngredient();
            if (ing != null && ing.getName() != null) {
                ri.setIngredient(ingredientRepo.findByNameIgnoreCase(ing.getName())
                        .orElseGet(() -> ingredientRepo.save(new Ingredient(ing.getName()))));
            }
        }
    }
}
