package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrcidSignUpTokensTest {

    private static final String SECRET = "orcid-client-secret";
    private static final String REDIRECT_URI = "http://localhost:8000/modern/sign-up";
    private static final String ORCID_URL = "https://orcid.org/0000-0002-1825-0097";
    private static final long NOW = 1_800_000_000_000L;

    @Test
    void state_isValidForSameRedirectUriWithinTimeout() {
        String state = OrcidSignUpTokens.createState(REDIRECT_URI, SECRET, NOW);

        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, SECRET, NOW + 60_000)).isTrue();
    }

    @Test
    void state_isRejectedForOtherRedirectUriSecretOrAfterTimeout() {
        String state = OrcidSignUpTokens.createState(REDIRECT_URI, SECRET, NOW);

        assertThat(OrcidSignUpTokens.isValidState(state, "https://evil.example/sign-up", SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, "other-secret", NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, SECRET, NOW + OrcidSignUpTokens.STATE_TIMEOUT_MILLIS)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, SECRET, NOW - 1)).isFalse();
    }

    @Test
    void state_isRejectedWhenMissingOrTampered() {
        assertThat(OrcidSignUpTokens.isValidState(null, REDIRECT_URI, SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState("", REDIRECT_URI, SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState("not-a-real-state", REDIRECT_URI, SECRET, NOW)).isFalse();
    }

    @Test
    void orcidToken_returnsOrcidUrlWithinTimeout() {
        String token = OrcidSignUpTokens.createOrcidToken(ORCID_URL, SECRET, NOW);

        assertThat(OrcidSignUpTokens.readOrcidToken(token, SECRET, NOW + 60_000)).contains(ORCID_URL);
    }

    @Test
    void orcidToken_isRejectedWhenExpiredTamperedOrSignedWithOtherSecret() {
        String token = OrcidSignUpTokens.createOrcidToken(ORCID_URL, SECRET, NOW);

        assertThat(OrcidSignUpTokens.readOrcidToken(token, SECRET, NOW + OrcidSignUpTokens.ORCID_TOKEN_TIMEOUT_MILLIS)).isEmpty();
        assertThat(OrcidSignUpTokens.readOrcidToken(token, "other-secret", NOW)).isEmpty();
        assertThat(OrcidSignUpTokens.readOrcidToken(token + "x", SECRET, NOW)).isEmpty();
        assertThat(OrcidSignUpTokens.readOrcidToken(null, SECRET, NOW)).isEmpty();
    }

    @Test
    void stateAndOrcidTokenCannotBeUsedInPlaceOfEachOther() {
        String state = OrcidSignUpTokens.createState(REDIRECT_URI, SECRET, NOW);
        String token = OrcidSignUpTokens.createOrcidToken(REDIRECT_URI, SECRET, NOW);

        assertThat(OrcidSignUpTokens.readOrcidToken(state, SECRET, NOW)).isEmpty();
        assertThat(OrcidSignUpTokens.isValidState(token, REDIRECT_URI, SECRET, NOW)).isFalse();
    }
}
