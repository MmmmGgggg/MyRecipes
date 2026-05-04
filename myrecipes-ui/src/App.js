import React, { useState, useEffect } from "react";
import RecipeList from "./components/RecipeList";
import RecipeForm from "./components/RecipeForm";
import RecipeDetail from "./components/RecipeDetail";
import AuthForm from "./components/AuthForm";
import "./App.css";

const API = `${window.location.protocol}//${window.location.hostname}:8080/api/recipes`;
const FAVORITES_API = `${window.location.protocol}//${window.location.hostname}:8080/api/favorites`;
const AUTH_API = `${window.location.protocol}//${window.location.hostname}:8080/api/auth`;

function App() {
  const [view, setView] = useState("list");
  const [editingRecipe, setEditingRecipe] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [filterCreator, setFilterCreator] = useState("");
  const [auth, setAuth] = useState(() => {
    const saved = localStorage.getItem("auth");
    return saved ? JSON.parse(saved) : null;
  });

  const handleLogin = (data) => {
    const authData = { token: data.token, email: data.email, name: data.name };
    localStorage.setItem("auth", JSON.stringify(authData));
    setAuth(authData);
  };

  const handleLogout = () => {
    localStorage.removeItem("auth");
    setAuth(null);
    setView("list");
  };

  const handleDeleteAccount = () => {
    if (!window.confirm("Are you sure you want to delete your account? All your recipes will be permanently deleted.")) return;
    if (!window.confirm("This cannot be undone. Are you really sure?")) return;
    fetch(`${AUTH_API}/account`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${auth.token}` },
    }).then(r => {
      if (r.ok) {
        localStorage.removeItem("auth");
        setAuth(null);
        setView("list");
      }
    }).catch(console.error);
  };

  const goToList = () => { setView("list"); setEditingRecipe(null); setFilterCreator(""); };
  const goToCreatorRecipes = (creator) => { setFilterCreator(creator); setView("list"); };
  const goToAdd = () => { setEditingRecipe(null); setView("form"); };
  const goToEdit = (recipe) => { setEditingRecipe(recipe); setView("form"); };
  const goToDetail = (id) => { setSelectedId(id); setView("detail"); };

  return (
    <div className="app">
      <header>
        <h1 onClick={goToList}>🍳 MyRecipes</h1>
        <div className="header-actions">
          {auth ? (
            <>
              <span className="user-name creator-link" onClick={() => goToCreatorRecipes(auth.name)}>👤 {auth.name}</span>
              {view !== "form" && <button onClick={goToAdd}>+ Add Recipe</button>}
              <button onClick={() => setView("favorites")} className={view === "favorites" ? "active-nav" : ""}>❤️ Favorites</button>
              <button onClick={handleLogout} className="logout-btn">Logout</button>
              <button onClick={() => setView("settings")} className="settings-btn">⚙️</button>
            </>
          ) : (
            <button onClick={() => setView("auth")}>Login</button>
          )}
        </div>
      </header>
      <main>
        {view === "auth" && <AuthForm onLogin={(data) => { handleLogin(data); goToList(); }} />}
        {view === "list" && <RecipeList key={filterCreator} api={API} onSelect={goToDetail} token={auth?.token} initialCreator={filterCreator} />}
        {view === "form" && <RecipeForm api={API} recipe={editingRecipe} onSave={goToList} onCancel={goToList} token={auth?.token} userName={auth?.name} />}
        {view === "detail" && <RecipeDetail api={API} id={selectedId} onBack={goToList} onEdit={goToEdit} onCreatorClick={goToCreatorRecipes} token={auth?.token} userEmail={auth?.email} />}
        {view === "favorites" && <FavoritesList api={FAVORITES_API} token={auth?.token} onSelect={goToDetail} />}
        {view === "settings" && (
          <div className="settings-page">
            <h2>⚙️ Settings</h2>
            <div className="settings-section">
              <h3>Account</h3>
              <p className="settings-info">👤 {auth?.name} ({auth?.email})</p>
            </div>
            <div className="settings-section settings-danger">
              <h3>Danger Zone</h3>
              <p className="settings-info">This will permanently delete your account and all your recipes.</p>
              <button onClick={handleDeleteAccount} className="delete-account-btn">Delete Account</button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

function FavoritesList({ api, token, onSelect }) {
  const [recipes, setRecipes] = useState([]);

  useEffect(() => {
    fetch(api, { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.json())
      .then(setRecipes)
      .catch(console.error);
  }, [api, token]);

  if (recipes.length === 0) return <p className="empty">No favorites yet. Save recipes you love!</p>;

  return (
    <div className="recipe-grid">
      {recipes.map(r => (
        <div key={r.id} className="recipe-card" onClick={() => onSelect(r.id)}>
          <h3>{r.name}</h3>
          <div className="card-badges">
            {r.tags?.map(tag => <span key={tag} className="tag-badge">{tag}</span>)}
          </div>
        </div>
      ))}
    </div>
  );
}

export default App;
