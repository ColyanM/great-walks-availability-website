import './App.css'
import { useState } from 'react'

function App() {
  const title = 'New Zealand Great Walk Alerts Service'
  const [status, setStatus] = useState('Not checked')

  async function handleCheck() {
    setStatus('Checking...')

    try {
      const response = await fetch('/api/health')

      if (!response.ok) {
        throw new Error('Health check failed')
      }

      const message = await response.text()
      setStatus(message)
    } catch {
      setStatus('Check failed')
    }
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
