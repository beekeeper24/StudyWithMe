# OAuth Login Baseline

Normalize provider attributes before touching member persistence.

Current supported providers:

- Google
- Kakao

Provider identity rule:

- Google subject: `sub`
- Kakao subject: root `id`
- StudyWithMe unique key: `(oauth_provider, oauth_subject)`

Keep provider parsing in `OAuth2UserProfileFactory`. Keep member lookup/creation in `OAuthLoginService`.

First login behavior:

- existing provider account: update email/profile image, keep the same member id;
- new provider account: create active member with `USER` role;
- duplicated nickname: append a deterministic suffix instead of failing the OAuth login.

`CustomOAuth2UserService` is the Spring Security adapter. It delegates provider user-info loading to `DefaultOAuth2UserService`, normalizes attributes, persists/loads the member, then returns `StudyWithMeOAuth2User`.

Provider client registrations should stay in `application-oauth.yml` under the `oauth` profile. Keep real client id/secret values outside git and pass them through environment variables or the deployment secret store.

JWT issuing and the final security filter chain are still separate follow-up work.
