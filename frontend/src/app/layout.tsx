import type { Metadata } from 'next'
import SessionProvider from '@/components/SessionProvider'
import SessionErrorHandler from '@/components/SessionErrorHandler'
import Navigation from '@/components/Navigation'
import ThemeProvider from '@/components/ThemeProvider'
import QuickNoteOverlay from '@/components/quick-notes/QuickNoteOverlay'
import AiCommandTerminal from '@/components/ai-terminal/AiCommandTerminal'
import './globals.css'

export const metadata: Metadata = {
  title: 'CrewCaptain',
  description: 'A private cockpit for people context — self-hosted manager workspace for 1:1s, PDP tracking, action items, and kudos',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <head>
        <link
          rel="preconnect"
          href="https://fonts.googleapis.com"
        />
        <link
          rel="preconnect"
          href="https://fonts.gstatic.com"
          crossOrigin="anonymous"
        />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@500;600;700&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <SessionProvider>
          <SessionErrorHandler />
          <ThemeProvider>
            <Navigation />
            <main>{children}</main>
            <QuickNoteOverlay />
            <AiCommandTerminal />
          </ThemeProvider>
        </SessionProvider>
      </body>
    </html>
  )
}
