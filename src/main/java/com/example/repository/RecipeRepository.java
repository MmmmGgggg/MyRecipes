package com.example.repository;

import com.example.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByNameContainingIgnoreCase(String name);

    List<Recipe> findByCreatorContainingIgnoreCase(String creator);

    @Query("SELECT DISTINCT r FROM Recipe r JOIN r.recipeIngredients ri WHERE ri.ingredient.id = :ingredientId")
    List<Recipe> findByIngredientId(Long ingredientId);

    @Query("SELECT DISTINCT r.creator FROM Recipe r WHERE r.creator IS NOT NULL AND r.creator <> ''")
    List<String> findDistinctCreators();
}
