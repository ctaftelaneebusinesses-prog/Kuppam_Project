# OneTownCity

## Product
OneTownCity is a local discovery platform.

## Architecture
Django backend
PostgreSQL/Supabase
Responsive server-rendered web
Native Android client
Shared backend/business logic

## Core rule
Web and Android are two clients of ONE product.

## Backend
Django owns:
- business logic
- authorization
- validation
- data integrity
- moderation
- notifications
- listing rules

## Web
Server-rendered responsive UI.
SEO is first-class.

## Android
Native Kotlin/Compose where applicable.
Do not duplicate business rules from Django.

## Data
Never create fake/test production data.

## Security
Never expose:
- secrets
- service-role keys
- credentials
- signing keys
- private certificates

## Development
Never modify architecture without evidence.

## UI
Never solve layout problems by:
- shrinking text
- clipping controls
- negative margins
- hiding functionality
- hardcoded viewport hacks

## Testing
Every meaningful change must have appropriate tests.

## Release
P0/P1 issues block production release.
