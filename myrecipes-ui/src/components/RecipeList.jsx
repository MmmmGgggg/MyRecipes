import React, { useState, useEffect, useCallback } from "react";

const INGREDIENT_API = `${window.location.protocol}//${window.location.hostname}:8080/api/ingredients`;
const TAGS_API = `${window.location.protocol}//${window.location.hostname}:8080/api/recipes/tags`;

export default function RecipeList({ api, onSelect, token }) {
  const [recipes, setRecipes] = useState([]);
  const [search, setSearch] = useState("");
  const [ingredients, setIngredients] = useState([]);
  const [selectedIngredients, setSelectedIngredients] = useState([]);
  const [ingredientSearch, setIngredientSearch] = useState("");
  const [showIngredientDropdown, setShowIngredientDropdown] = useState(false);
  const [creators, setCreators] = useState([]);
  const [filterCreator, setFilterCreator] = useState("");
  const [creatorSearch, setCreatorSearch] = useState("");
  const [showCreatorDropdown, setShowCreatorDropdown] = useState(false);
  const [allTags, setAllTags] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [tagSearch, setTagSearch] = useState("");
  const [showTagDropdown, setShowTagDropdown] = useState(false);
  const [activeFilter, setActiveFilter] = useState(null);

  const getHeaders = useCallback(() => {
    return token ? { Authorization: `Bearer ${token}` } : {};
  }, [token]);

  useEffect(() => {
    fetch(INGREDIENT_API).then(r => r.json()).then(setIngredients).catch(console.error);
    fetch(`${api}/creators`).then(r => r.json()).then(setCreators).catch(console.error);
    fetch(TAGS_API).then(r => r.json()).then(setAllTags).catch(console.error);
  }, [api]);

  useEffect(() => {
    const params = new URLSearchParams();
    if (search) params.append("search", search);
    if (filterCreator) params.append("creator", filterCreator);
    selectedTags.forEach(t => params.append("tags", t));
    selectedIngredients.forEach(i => params.append("ingredientIds", i.id));
    const query = params.toString();
    const url = query ? `${api}?${query}` : api;

    fetch(url, { headers: getHeaders() })
      .then(r => {
        if (!r.ok) throw new Error("Failed to fetch");
        return r.json();
      })
      .then(setRecipes)
      .catch(err => {
        console.error("Fetch error:", err);
        setRecipes([]);
      });
  }, [api, search, selectedIngredients, filterCreator, selectedTags, token, getHeaders]);

  const filteredIngredients = ingredients.filter(i =>
    i.name.toLowerCase().includes(ingredientSearch.toLowerCase())
    && !selectedIngredients.some(s => s.id === i.id)
  );

  const filteredTags = allTags.filter(t =>
    t.toLowerCase().includes(tagSearch.toLowerCase())
    && !selectedTags.includes(t)
  );

  const addIngredient = (ingredient) => {
    setSelectedIngredients(prev => [...prev, ingredient]);
    setIngredientSearch("");
    setShowIngredientDropdown(false);
  };

  const removeIngredient = (id) => {
    setSelectedIngredients(prev => prev.filter(i => i.id !== id));
  };

  const addTag = (tag) => {
    if (!selectedTags.includes(tag)) {
      setSelectedTags(prev => [...prev, tag]);
    }
    setTagSearch("");
    setShowTagDropdown(false);
  };

  const removeTag = (tag) => {
    setSelectedTags(prev => prev.filter(t => t !== tag));
  };

  const toggleFilter = (filter) => {
    setActiveFilter(activeFilter === filter ? null : filter);
  };

  const hasActiveFilters = selectedIngredients.length > 0 || selectedTags.length > 0 || filterCreator;

  const clearAllFilters = () => {
    setSelectedIngredients([]);
    setSelectedTags([]);
    setFilterCreator("");
    setCreatorSearch("");
    setActiveFilter(null);
  };

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
        onChange={e => setSearch(e.target.value)}
        className="search-input"
      />

      <div className="filter-buttons">
        <button
          type="button"
          className={`filter-btn ${activeFilter === "creator" || filterCreator ? "filter-btn-active" : ""}`}
          onClick={() => toggleFilter("creator")}
        >
          👤 Creator {filterCreator && `(${filterCreator})`}
        </button>
        <button
          type="button"
          className={`filter-btn ${activeFilter === "tags" || selectedTags.length > 0 ? "filter-btn-active" : ""}`}
          onClick={() => toggleFilter("tags")}
        >
          🏷️ Tags {selectedTags.length > 0 && `(${selectedTags.length})`}
        </button>
        <button
          type="button"
          className={`filter-btn ${activeFilter === "ingredients" || selectedIngredients.length > 0 ? "filter-btn-active" : ""}`}
          onClick={() => toggleFilter("ingredients")}
        >
          🥕 Ingredients {selectedIngredients.length > 0 && `(${selectedIngredients.length})`}
        </button>
        {hasActiveFilters && (
          <button type="button" className="filter-btn filter-btn-clear" onClick={clearAllFilters}>
            ✕ Clear
          </button>
        )}
      </div>

      {activeFilter === "creator" && (
        <div className="filter-panel">
          <div className="ingredient-search-wrapper">
            <input
              type="text"
              placeholder="Type creator name..."
              value={creatorSearch}
              onChange={e => { setCreatorSearch(e.target.value); setShowCreatorDropdown(true); setFilterCreator(""); }}
              onFocus={() => setShowCreatorDropdown(true)}
              onBlur={() => setTimeout(() => setShowCreatorDropdown(false), 200)}
              className="filter-input"
              autoFocus
            />
            {showCreatorDropdown && creatorSearch && creators.filter(c => c.toLowerCase().includes(creatorSearch.toLowerCase())).length > 0 && (
              <div className="suggestions">
                {creators.filter(c => c.toLowerCase().includes(creatorSearch.toLowerCase())).slice(0, 8).map((c, i) => (
                  <div key={i} className="suggestion" onMouseDown={() => { setFilterCreator(c); setCreatorSearch(c); setShowCreatorDropdown(false); setActiveFilter(null); }}>
                    {c}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {activeFilter === "tags" && (
        <div className="filter-panel">
          <div className="ingredient-search-wrapper">
            <input
              type="text"
              placeholder="Type tag name..."
              value={tagSearch}
              onChange={e => { setTagSearch(e.target.value); setShowTagDropdown(true); }}
              onFocus={() => setShowTagDropdown(true)}
              onBlur={() => setTimeout(() => setShowTagDropdown(false), 200)}
              className="filter-input"
              autoFocus
            />
            {showTagDropdown && tagSearch && filteredTags.length > 0 && (
              <div className="suggestions">
                {filteredTags.slice(0, 8).map(t => (
                  <div key={t} className="suggestion" onMouseDown={() => addTag(t)}>
                    {t}
                  </div>
                ))}
              </div>
            )}
          </div>
          {selectedTags.length > 0 && (
            <div className="filter-chips">
              {selectedTags.map(tag => (
                <span key={tag} className="chip chip-active">
                  {tag}
                  <button type="button" className="chip-remove" onClick={() => removeTag(tag)}>✕</button>
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {activeFilter === "ingredients" && (
        <div className="filter-panel">
          <div className="ingredient-search-wrapper">
            <input
              type="text"
              placeholder="Type ingredient name..."
              value={ingredientSearch}
              onChange={e => { setIngredientSearch(e.target.value); setShowIngredientDropdown(true); }}
              onFocus={() => setShowIngredientDropdown(true)}
              onBlur={() => setTimeout(() => setShowIngredientDropdown(false), 200)}
              className="filter-input"
              autoFocus
            />
            {showIngredientDropdown && ingredientSearch && filteredIngredients.length > 0 && (
              <div className="suggestions">
                {filteredIngredients.slice(0, 8).map(i => (
                  <div key={i.id} className="suggestion" onMouseDown={() => addIngredient(i)}>
                    {i.name}
                  </div>
                ))}
              </div>
            )}
          </div>
          {selectedIngredients.length > 0 && (
            <div className="filter-chips">
              {selectedIngredients.map(i => (
                <span key={i.id} className="chip chip-active">
                  {i.name}
                  <button type="button" className="chip-remove" onClick={() => removeIngredient(i.id)}>✕</button>
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {recipes.length === 0 ? (
        <p className="empty">No recipes found. Add your first one!</p>
      ) : (
        <div className="recipe-grid">
          {recipes.map(r => (
            <div key={r.id} className="recipe-card" onClick={() => onSelect(r.id)}>
              <h3>{r.name}</h3>
              <p>{ingredientSummary(r).substring(0, 80)}</p>
              <div className="card-badges">
                {r.tags?.map(tag => (
                  <span key={tag} className="tag-badge">{tag}</span>
                ))}
                {r.visibility === "PRIVATE" && <span className="visibility-badge">🔒</span>}
                {r.visibility === "SHARED" && <span className="visibility-badge">👥</span>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
