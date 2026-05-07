import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'CrewCaptain',
  description: 'Self-hosted manager-only CRM for 1:1s, PDP tracking, action items, and kudos',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
