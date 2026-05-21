# JWT And Refresh Token Policy

Use JWT only for short-lived access tokens.

Access token:

- signed JWT with HS256;
- default TTL is `30m`;
- subject is member id;
- roles are included as a claim;
- normal API requests should validate this token without a refresh-token DB lookup.

Use an opaque random string for refresh tokens.

Refresh token:

- default TTL is `14d`;
- raw token is returned to the client only once;
- DB stores `SHA-256` hash only;
- refresh rotates the token by marking the old row `rotated_at` and saving a new row;
- rotated/revoked/expired tokens must not be accepted.

This is the current speed/safety balance:

- frequent API calls: stateless JWT validation;
- rare reissue/logout events: DB lookup for refresh-token control.

Do not store raw refresh tokens in the database.
