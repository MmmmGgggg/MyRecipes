import React, { useState, useEffect } from "react";

export default function RecipeDetail({ api, id, onBack, onEdit, token, userEmail }) {
  const [recipe, setRecipe] = useState(null);

  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  useEffect(() => {
    fetch(`${api}/${id}`, { headers }).then(r => r.json()).then(setRecipe).catch(console.error);
  }, [api, id, token]);

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
      {recipe.imageUrl && <img src={recipe.imageUrl} alt={recipe.name} />}
      <h2>{recipe.name}</h2>
      {recipe.visibility && <p className="recipe-meta">{({PRIVATE:"🔒 Private",SHARED:"👥 Shared",PUBLIC:"🌍 Public"})[recipe.visibility]}</p>}
      {recipe.tags?.length > 0 && (
        <div className="detail-tags">
          {recipe.tags.map(tag => <span key={tag} className="tag-badge">{tag}</span>)}
        </div>
      )}
      {recipe.creator && <p className="recipe-meta">👤 {recipe.creator}</p>}
      {recipe.language && <p className="recipe-meta">🌐 {recipe.language}</p>}
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
