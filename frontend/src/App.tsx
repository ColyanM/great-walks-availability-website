import './App.css'
import { useState } from 'react'

function App() {
  const title = 'New Zealand Great Walk Alerts Service'
  const [status, setStatus] = useState('Not checked')

  function handleCheck() {
    setStatus('Button clicked')
  }
  return (
    <main>
      <h1>
        {title}
      </h1>
      <p>Status: {status}</p>
      <button type="button" onClick={handleCheck}>
        Check backend
      </button>
    </main>
  )
}

export default App
