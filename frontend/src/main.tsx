import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { BrowserRouter, Routes, Route } from 'react-router-dom'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/:gameId" element={<App />} />
        <Route path="/" element={<App />} />
        {/* Diğer rotalar */}
      </Routes>
    </BrowserRouter>
    
  </React.StrictMode>,
)
