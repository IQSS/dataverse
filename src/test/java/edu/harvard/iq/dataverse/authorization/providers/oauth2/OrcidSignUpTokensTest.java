package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrcidSignUpTokensTest {

    private static final String SECRET = "orcid-client-secret";
    private static final String REDIRECT_URI = "http://localhost:8000/modern/sign-up";
    private static final String ORCID_URL = "https://orcid.org/0000-0002-1825-0097";
    private static final long NOW = 1_800_000_000_000L;
    // Example values from RFC 7636, Appendix B.
    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Test
    void codeChallenge_isTheS256HashOfTheCodeVerifier() {
        assertThat(OrcidSignUpTokens.toCodeChallenge(CODE_VERIFIER)).isEqualTo(CODE_CHALLENGE);
    }

    @Test
    void codeChallenge_mustBeABase64UrlEncodedSha256Hash() {
        assertThat(OrcidSignUpTokens.isValidCodeChallenge(CODE_CHALLENGE)).isTrue();
        assertThat(OrcidSignUpTokens.isValidCodeChallenge(null)).isFalse();
        assertThat(OrcidSignUpTokens.isValidCodeChallenge("")).isFalse();
        assertThat(OrcidSignUpTokens.isValidCodeChallenge(CODE_CHALLENGE + "x")).isFalse();
        assertThat(OrcidSignUpTokens.isValidCodeChallenge(CODE_CHALLENGE.replace('-', '+'))).isFalse();
    }

    @Test
    void state_isValidForSameRedirectUriAndCodeVerifierWithinTimeout() {
        String state = OrcidSignUpTokens.createState(REDIRECT_URI, CODE_CHALLENGE, SECRET, NOW);

        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, CODE_VERIFIER, SECRET, NOW + 60_000)).isTrue();
    }

    @Test
    void state_isRejectedForOtherRedirectUriSecretOrAfterTimeout() {
        String state = OrcidSignUpTokens.createState(REDIRECT_URI, CODE_CHALLENGE, SECRET, NOW);

        assertThat(OrcidSignUpTokens.isValidState(state, "https://evil.example/sign-up", CODE_VERIFIER, SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, CODE_VERIFIER, "other-secret", NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, CODE_VERIFIER, SECRET, NOW + OrcidSignUpTokens.STATE_TIMEOUT_MILLIS)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, CODE_VERIFIER, SECRET, NOW - 1)).isFalse();
    }

    @Test
    void state_isRejectedWithoutTheMatchingCodeVerifier() {
        String state = OrcidSignUpTokens.createState(REDIRECT_URI, CODE_CHALLENGE, SECRET, NOW);
        String otherVerifier = "a".repeat(43);

        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, otherVerifier, SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, null, SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, "too-short", SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState(state, REDIRECT_URI, CODE_CHALLENGE, SECRET, NOW)).isFalse();
    }

    @Test
    void state_isRejectedWhenMissingOrTampered() {
        assertThat(OrcidSignUpTokens.isValidState(null, REDIRECT_URI, CODE_VERIFIER, SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState("", REDIRECT_URI, CODE_VERIFIER, SECRET, NOW)).isFalse();
        assertThat(OrcidSignUpTokens.isValidState("not-a-real-state", REDIRECT_URI, CODE_VERIFIER, SECRET, NOW)).isFalse();
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
        String state = OrcidSignUpTokens.createState(REDIRECT_URI, CODE_CHALLENGE, SECRET, NOW);
        String token = OrcidSignUpTokens.createOrcidToken(REDIRECT_URI, SECRET, NOW);

        assertThat(OrcidSignUpTokens.readOrcidToken(state, SECRET, NOW)).isEmpty();
        assertThat(OrcidSignUpTokens.isValidState(token, REDIRECT_URI, CODE_VERIFIER, SECRET, NOW)).isFalse();
    }
}
