import React, { useState, useEffect } from "react";

const FAVORITES_API = `${window.location.protocol}//${window.location.hostname}:8080/api/favorites`;

export default function RecipeDetail({ api, id, onBack, onEdit, onCreatorClick, token, userEmail }) {
  const [recipe, setRecipe] = useState(null);
  const [isFavorite, setIsFavorite] = useState(false);

  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  useEffect(() => {
    fetch(`${api}/${id}`, { headers }).then(r => r.json()).then(setRecipe).catch(console.error);
    if (token) {
      fetch(`${FAVORITES_API}/${id}/check`, { headers })
        .then(r => r.json()).then(setIsFavorite).catch(console.error);
    }
  }, [api, id, token]);

  const toggleFavorite = () => {
    const method = isFavorite ? "DELETE" : "POST";
    fetch(`${FAVORITES_API}/${id}`, { method, headers })
      .then(() => setIsFavorite(!isFavorite))
      .catch(console.error);
  };

  const handleDelete = () => {
    if (window.confirm("Delete this recipe?")) {
      fetch(`${api}/${id}`, { method: "DELETE", headers }).then(onBack).catch(console.error);
    }
  };

  if (!recipe) return <p>Loading...</p>;

  const isOwner = userEmail && recipe.user?.email === userEmail;

  return (
    <div className="recipe-detail">
      <button onClick={onBack} className="back-btn">← Back</button>
      {token && (
        <button onClick={toggleFavorite} className={`favorite-btn ${isFavorite ? "favorited" : ""}`}>
          {isFavorite ? "❤️ Saved" : "🤍 Save"}
        </button>
      )}
      {recipe.imageUrl && <img src={recipe.imageUrl.startsWith("/") ? `${window.location.protocol}//${window.location.hostname}:8080${recipe.imageUrl}` : recipe.imageUrl} alt={recipe.name} />}
      <h2>{recipe.name}</h2>
      {recipe.visibility && <p className="recipe-meta">{({PRIVATE:"🔒 Private",SHARED:"👥 Shared",PUBLIC:"🌍 Public"})[recipe.visibility]}</p>}
      {recipe.tags?.length > 0 && (
        <div className="detail-tags">
          {recipe.tags.map(tag => <span key={tag} className="tag-badge">{tag}</span>)}
        </div>
      )}
      {recipe.creator && <p className="recipe-meta"><span className="creator-link" onClick={(e) => { e.stopPropagation(); onCreatorClick(recipe.creator); }}>👤 {recipe.creator}</span></p>}
      {recipe.language && <p className="recipe-meta">🌐 {recipe.language}</p>}
      {(recipe.servings || recipe.prepTimeMinutes || recipe.cookTimeMinutes) && (
        <div className="recipe-time-info">
          {recipe.servings && <span>🍽️ {recipe.servings} servings</span>}
          {recipe.prepTimeMinutes && <span>⏱️ Prep: {recipe.prepTimeMinutes} min</span>}
          {recipe.cookTimeMinutes && <span>🔥 Cook: {recipe.cookTimeMinutes} min</span>}
          {recipe.totalTimeMinutes && <span>⏰ Total: {recipe.totalTimeMinutes} min</span>}
        </div>
      )}
      <h3>Ingredients</h3>
      <ul className="ingredient-list">
        {recipe.recipeIngredients?.map((ri, i) => (
          <li key={i}>
            <strong>{ri.ingredient?.name}</strong>
            {ri.amount && <span> — {ri.amount}</span>}
          </li>
        ))}
      </ul>
      <h3>Instructions</h3>
      <p>{recipe.instructions}</p>
      {isOwner && (
        <div className="detail-buttons">
          <button onClick={() => onEdit(recipe)}>Edit</button>
          <button onClick={handleDelete} className="delete-btn">Delete</button>
        </div>
      )}
    </div>
  );
}
