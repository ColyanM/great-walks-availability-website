import { useEffect, useState } from 'react'
import type { AlertResponse, Walk } from './types'

type SavedAlertsProps = {
    walks: Walk[]
    refreshVersion: number
}

export default function SavedAlerts({
    walks,
    refreshVersion,
}: SavedAlertsProps) {
    const [alerts, setAlerts] = useState<AlertResponse[] | null>(null)
    const [error, setError] = useState('')

    useEffect(() => {
        let ignore = false

        async function loadAlerts() {
            try {
                const response = await fetch('/api/alerts')

                if (!response.ok) {
                    throw new Error('Could not load alerts')
                }

                const data: AlertResponse[] = await response.json()

                if (!ignore) {
                    setAlerts(data)
                    setError('')
                }
            } catch {
                if (!ignore) {
                    setError('Could not load saved alerts. Refresh the page to try again.')
                }
            }
        }

        loadAlerts()

        return () => {
            ignore = true
        }
    }, [refreshVersion])

    return (
        <section>
            <h2>Saved alerts</h2>

            {error ? (
                <p role="alert">{error}</p>
            ) : alerts === null ? (
                <p>Loading saved alerts...</p>
            ) : alerts.length === 0 ? (
                <p>You haven’t saved any alerts yet.</p>
            ) : (
                <ul>
                    {alerts.map((alert) => {
                        const walk = walks.find((walk) => walk.id === alert.walkId)

                        return (
                            <li key={alert.id}>
                                {walk?.name ?? `Walk #${alert.walkId}`} — {alert.startDate}
                                {' — '}
                                {alert.partySize} {alert.partySize === 1 ? 'person' : 'people'}
                            </li>
                        )
                    })}
                </ul>
            )}
        </section>
    )
}