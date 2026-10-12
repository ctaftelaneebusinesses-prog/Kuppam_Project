# Database

`schema.sql` is the complete OneTownCity database structure (PostgreSQL / Supabase) in one file: every table, column, index and constraint, through migration `core.0053`.

## Where the migrations live

The actual migrations are in [`core/migrations/`](../core/migrations/). Django runs them with `python manage.py migrate`, and Render runs that on every deploy. They are the source of truth. `schema.sql` is a readable snapshot generated from them.

## Using `schema.sql`

- **Read it** to see the whole data model in one place.
- **Create an empty database** by running it in a new PostgreSQL / Supabase project (SQL editor or `psql -f schema.sql`).

It contains structure only, no data. The starter data some migrations add (categories, Indian cities and their coordinates, permissions, platform modules) is created by `python manage.py migrate`, not by this file. For a working database, use `migrate`.

## Keeping it current

After adding a migration, regenerate this file the same way (`sqlmigrate` for every migration, in order) so it matches `core/migrations/`.
