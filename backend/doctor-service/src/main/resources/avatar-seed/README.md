# Doctor avatar seed

The JPEG files in this directory are AI-generated portraits for `.local` demo accounts only.
They must not be presented as verified photographs of real clinicians.

Enable the one-time importer with `DOCTOR_AVATAR_SEED_ENABLED=true` only after setting
the server-side `SUPABASE_SERVICE_ROLE_KEY`. This variable accepts the recommended
`sb_secret_...` key or a legacy `service_role` JWT. The importer is idempotent and skips doctors
that already have an approved avatar.

Example for PowerShell (set the secret locally; do not commit it):

```powershell
$env:SUPABASE_SERVICE_ROLE_KEY = '<service-role-key>'
$env:DOCTOR_AVATAR_SEED_ENABLED = 'true'
mvn spring-boot:run
```

After the log reports `11 new demo avatars imported`, stop the service and set
`DOCTOR_AVATAR_SEED_ENABLED=false` for later starts.
