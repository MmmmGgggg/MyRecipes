import React, { useState, useEffect } from "react";

const INGREDIENT_API = "http://localhost:8080/api/ingredients";

export default function RecipeList({ api, onSelect }) {
  const [recipes, setRecipes] = useState([]);
  const [search, setSearch] = useState("");
  const [ingredients, setIngredients] = useState([]);
  const [filterIngredient, setFilterIngredient] = useState("");
  const [creators, setCreators] = useState([]);
  const [filterCreator, setFilterCreator] = useState("");

  useEffect(() => {
    fetch(INGREDIENT_API).then(r => r.json()).then(setIngredients).catch(console.error);
    fetch(`${api}/creators`).then(r => r.json()).then(setCreators).catch(console.error);
  }, [api]);

  useEffect(() => {
    let url = api;
    if (filterIngredient) url = `${api}?ingredientId=${filterIngredient}`;
    else if (filterCreator) url = `${api}?creator=${filterCreator}`;
    else if (search) url = `${api}?search=${search}`;
    fetch(url).then(r => r.json()).then(setRecipes).catch(console.error);
  }, [api, search, filterIngredient, filterCreator]);

  const ingredientSummary = (recipe) => {
    if (!recipe.recipeIngredients?.length) return "No ingredients";
    return recipe.recipeIngredients.map(ri => ri.ingredient?.name).join(", ");
  };

  return (
    <div>
      <input
        type="text"
        placeholder="Search recipes..."
        value={search}
        onChange={e => { setSearch(e.target.value); setFilterIngredient(""); setFilterCreator(""); }}
        className="search-input"
      />
      <div className="filter-row">
        <select
          className="filter-select"
          value={filterIngredient}
          onChange={e => { setFilterIngredient(e.target.value); setSearch(""); setFilterCreator(""); }}
        >
          <option value="">Filter by ingredient...</option>
          {ingredients.map(i => (
            <option key={i.id} value={i.id}>{i.name}</option>
          ))}
        </select>
        <select
          className="filter-select"
          value={filterCreator}
          onChange={e => { setFilterCreator(e.target.value); setSearch(""); setFilterIngredient(""); }}
        >
          <option value="">Filter by creator...</option>
          {creators.map((c, i) => (
            <option key={i} value={c}>{c}</option>
          ))}
        </select>
      </div>
      {recipes.length === 0 ? (
        <p className="empty">No recipes found. Add your first one!</p>
      ) : (
        <div className="recipe-grid">
          {recipes.map(r => (
            <div key={r.id} className="recipe-card" onClick={() => onSelect(r.id)}>
              <h3>{r.name}</h3>
              <p>{ingredientSummary(r).substring(0, 80)}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
