import './App.css'
import { useEffect, useState } from 'react'
type HealthResponse = {
  status: string
}

type Walk = {
  id: number
  name: string
}

type AlertResponse = {
  id: number
  walkId: number
  startDate: string
  partySize: number
}

function App() {
  const title = 'New Zealand Great Walk Alerts Service'
  const [status, setStatus] = useState('Not checked')
  const [walks, setWalks] = useState<Walk[]>([])
  const [selectedWalkId, setSelectedWalkId] = useState('')
  const [walksMessage, setWalksMessage] = useState('Loading walks...')
  const [startDate, setStartDate] = useState('')
  const [partySize, setPartySize] = useState('1')
  const [formMessage, setFormMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

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
  const selectedWalk = walks.find(
    (walk) => walk.id === Number(selectedWalkId)
  )

  const people = Number(partySize)
  const validPartySize = Number.isInteger(people) && people >= 1
  async function handleSubmit() {
    if (isSubmitting) {
      return
    }
    if (!selectedWalk) {
      setFormMessage('Please choose a walk.')
      return
    }

    if (!startDate) {
      setFormMessage('Please choose a start date.')
      return
    }

    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const today = `${year}-${month}-${day}`

    if (startDate < today) {
      setFormMessage('Your start date cannot be in the past.')
      return
    }

    if (!validPartySize) {
      setFormMessage('Please enter a whole number of people, at least 1.')
      return
    }

    setIsSubmitting(true)
    setFormMessage('Saving your alert...')
    try {
      const response = await fetch('/api/alerts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          walkId: selectedWalk.id,
          startDate: startDate,
          partySize: people,
        }),
      })

      if (response.status === 400) {
        setFormMessage('The backend rejected these details. Please check your inputs.')
        return
      }

      if (!response.ok) {
        throw new Error('Alert request failed')
      }

      const savedAlert: AlertResponse = await response.json()
      setFormMessage(`Alert #${savedAlert.id} saved.`)
    } catch {
      setFormMessage('Could not confirm whether the alert was saved.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main>
      <h1>
        {title}
      </h1>
      <form
        noValidate
        onChange={() => setFormMessage('')}
        onSubmit={(event) => {
          event.preventDefault()
          handleSubmit()
        }}
      >
        <label htmlFor="walk">Choose a walk</label>
        <select
          id="walk"
          value={selectedWalkId}
          onChange={(event) => setSelectedWalkId(event.target.value)}
          disabled={walks.length === 0 || isSubmitting}
        >
          <option value="">Select a walk</option>

          {walks.map((walk) => (
            <option key={walk.id} value={walk.id}>
              {walk.name}
            </option>
          ))}
        </select>

        <div>
          <label htmlFor="startDate">Trip start date</label>
          <input
            id="startDate"
            type="date"
            value={startDate}
            onChange={(event) => setStartDate(event.target.value)}
            disabled={walks.length === 0 || isSubmitting}
          />
        </div>

        <div>
          <label htmlFor="partySize">Number of people</label>
          <input
            id="partySize"
            type="number"
            min="1"
            step="1"
            value={partySize}
            onChange={(event) => setPartySize(event.target.value)}
            disabled={isSubmitting}
          />
        </div>
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving...' : 'Save alert'}
        </button>          <p role="status">{formMessage}</p>
      </form>

      <p>{walksMessage}</p>
      <p>Status: {status}</p>
      <button type="button" onClick={handleCheck}>
        Check backend
      </button>
      {selectedWalk && startDate && validPartySize && (
        <p>
          Alert preview: {selectedWalk.name}, starting {startDate},
          for {people} {people === 1 ? 'person' : 'people'}.
        </p>
      )}
    </main>
  )
}

export default App
