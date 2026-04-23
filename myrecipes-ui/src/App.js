import React, { useState } from "react";
import RecipeList from "./components/RecipeList";
import RecipeForm from "./components/RecipeForm";
import RecipeDetail from "./components/RecipeDetail";
import AuthForm from "./components/AuthForm";
import "./App.css";

const API = "http://localhost:8080/api/recipes";

function App() {
  const [view, setView] = useState("list");
  const [editingRecipe, setEditingRecipe] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
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

  const goToList = () => { setView("list"); setEditingRecipe(null); };
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
              <span className="user-name">👤 {auth.name}</span>
              {view !== "form" && <button onClick={goToAdd}>+ Add Recipe</button>}
              <button onClick={handleLogout} className="logout-btn">Logout</button>
            </>
          ) : (
            <button onClick={() => setView("auth")}>Login</button>
          )}
        </div>
      </header>
      <main>
        {view === "auth" && <AuthForm onLogin={(data) => { handleLogin(data); goToList(); }} />}
        {view === "list" && <RecipeList api={API} onSelect={goToDetail} token={auth?.token} />}
        {view === "form" && <RecipeForm api={API} recipe={editingRecipe} onSave={goToList} onCancel={goToList} token={auth?.token} userName={auth?.name} />}
        {view === "detail" && <RecipeDetail api={API} id={selectedId} onBack={goToList} onEdit={goToEdit} token={auth?.token} userEmail={auth?.email} />}
      </main>
    </div>
  );
}

export default App;
