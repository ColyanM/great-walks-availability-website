import './App.css'
import { useEffect, useState } from 'react'
type HealthResponse = {
  status: string
}

type Walk = {
  id: number
  name: string
}

function App() {
  const title = 'New Zealand Great Walk Alerts Service'
  const [status, setStatus] = useState('Not checked')
  const [walks, setWalks] = useState<Walk[]>([])
  const [selectedWalkId, setSelectedWalkId] = useState('')
  const [walksMessage, setWalksMessage] = useState('Loading walks...')

  async function handleCheck() {
    setStatus('Checking...')

    try {
      const response = await fetch('/api/health')

      if (!response.ok) {
        throw new Error('Health check failed')
      }

      const data: HealthResponse = await response.json()
      setStatus(data.status)
    } catch {
      setStatus('Check failed')
    }
  }
  useEffect(() => {
    let ignore = false

    async function loadWalks() {
      try {
        const response = await fetch('/api/walks')

        if (!response.ok) {
          throw new Error('Could not load walks')
        }

        const data: Walk[] = await response.json()

        if (!ignore) {
          setWalks(data)
          setWalksMessage(data.length === 0 ? 'No walks available.' : '')
        }
      } catch {
        if (!ignore) {
          setWalksMessage('Could not load walks. Please refresh to try again.')
        }
      }
    }

    loadWalks()

    return () => {
      ignore = true
    }
  }, [])
  return (
    <main>
      <h1>
        {title}
      </h1>
      <label htmlFor="walk">Choose a walk</label>
      <select
        id="walk"
        value={selectedWalkId}
        onChange={(event) => setSelectedWalkId(event.target.value)}
        disabled={walks.length === 0}
      >
        <option value="">Select a walk</option>

        {walks.map((walk) => (
          <option key={walk.id} value={walk.id}>
            {walk.name}
          </option>
        ))}
      </select>

      <p>{walksMessage}</p>
      <p>Status: {status}</p>
      <button type="button" onClick={handleCheck}>
        Check backend
      </button>
    </main>
  )
}

export default App
