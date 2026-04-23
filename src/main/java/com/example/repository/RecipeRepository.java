package com.example.repository;

import com.example.model.Recipe;
import com.example.model.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByNameContainingIgnoreCase(String name);

    List<Recipe> findByCreatorContainingIgnoreCase(String creator);

    @Query("SELECT DISTINCT r FROM Recipe r JOIN r.recipeIngredients ri WHERE ri.ingredient.id = :ingredientId")
    List<Recipe> findByIngredientId(Long ingredientId);

    @Query("SELECT DISTINCT r FROM Recipe r JOIN r.recipeIngredients ri WHERE ri.ingredient.id IN :ingredientIds GROUP BY r HAVING COUNT(DISTINCT ri.ingredient.id) = :count")
    List<Recipe> findByAllIngredientIds(List<Long> ingredientIds, long count);

    @Query("SELECT DISTINCT r.creator FROM Recipe r WHERE r.creator IS NOT NULL AND r.creator <> ''")
    List<String> findDistinctCreators();

    @Query("SELECT DISTINCT t FROM Recipe r JOIN r.tags t ORDER BY t")
    List<String> findDistinctTags();

    List<Recipe> findByVisibility(Visibility visibility);

    @Query("SELECT r FROM Recipe r WHERE r.visibility = 'PUBLIC' AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Recipe> findPublicByNameContaining(String name);

    List<Recipe> findByUserEmail(String email);

    @Query("SELECT r FROM Recipe r LEFT JOIN r.user u WHERE r.visibility = 'PUBLIC' OR u.email = :email")
    List<Recipe> findAccessibleByUser(String email);
}
