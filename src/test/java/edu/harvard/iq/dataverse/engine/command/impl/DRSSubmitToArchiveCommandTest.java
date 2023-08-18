package edu.harvard.iq.dataverse.engine.command.impl;

import org.erdtman.jcs.JsonCanonicalizer;
import org.junit.jupiter.api.Test;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
//import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.fail;

public class DRSSubmitToArchiveCommandTest {

    /* Simple test of JWT encode/decode functionality
     * 
     */
    @Test
    public void createJWT() throws CommandException {

        String privKeyString = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCzSwj+c/uiRz5A"
                + "OiDWsV5pxJrdzlDRV2PKKwRGCzhv1MEPwQCvFp6wZRDgCE4EfpVUuByNInV1eOfr"
                + "BjwIlxp8hv9RPYCAsPCFV46VLeZsr8FOfvqI6IswYqB3qwdi5NW+CuJRLgTFJP87"
                + "X5GgoItVnE0/DxIuZobuaEEzPa8TV8kUvdehzxTlkMTay5J/USeyKsUjPozqgKtN"
                + "4ScCWrQx2FXEuKoCg85wNgFRJHgSGBH07lNAYV2tOz+w0ToSNzKswNqhTpRl7W61"
                + "gzDCFJu6IYreH9bH5eh/Z9BzjNOs16k0Ok2PmQhOhHYCT3fdkKogriSREVN5dlHi"
                + "FV7eB577AgMBAAECggEAPGfLX+8zmDjogDsVVT/szzWt94zLLbyDollb1z1whjzn"
                + "zqb31AWK8WMbjF8/6cO8DA77j5FMgYd6m3Q+RaajBdF1s6lE4ha68jHNl/Ue7P9J"
                + "4WhmgDnYqzSPW8IDew4d9Sk1lqQqd0E/vIE2TyfHydAfNl+dgISKcUgur1TY52rb"
                + "taldnMP44BoXSeKM1qMAE7tWXDQlRjDdcx2Vn6nKJ4iCC6490JSGaFpsoock9wkF"
                + "Fi1euzVnvX3ksyioXHMZwzZ9ErCHsI+Px25xiroyloxeoj0zfcA8kZcC9vyoa9HF"
                + "2p62iK6RM7JCQc7yMcSN2Fp8PzyHlOLgdI+8CKV4AQKBgQDYmVFenIbapLgN3uyW"
                + "gPTgUQGdnLf2S1g1HHHw7+74aZuMKq20w8Ikv6qWMx07R05gm8yxQ1Z4ciLcEw2z"
                + "KBurLte/t6ZAJXQ7wnbPyX1JPFQNxKJrPKq+FynnANrdPVgwUunmO9JJbsudU/cG"
                + "WKaQiG0w5ltvXg1NY5i1doifawKBgQDT6HFxh31nGUySNRQloE9mpvbzT35ornvl"
                + "0oMlCYX2M52C3/nH/rq30woP4hDMBlvq3V6blOzPHzQwlu4+4OKBqvxlAluYIoXP"
                + "QD1vJhb7eti+mYnIWyQ6hnAhrg/WDxn69mixEson2EL68+WRawz61h3WbfKoivbe"
                + "YP02G2uysQKBgBOPFLf0boED6tLl1HtqvbIb3od7BWmqOBbjsK5PHEc2UiOAHxt5"
                + "qehjnmXdy7/0mnFC4GMJb5+Evv0cg1owPv9gRX88eDjGqQ5UayIsUbHxTq3HmdsR"
                + "KWHs+Y2wmBLuXS5P7msp771N0fktAduC2denWiTWSF9wIMdiPQH16DRtAoGBAKs4"
                + "ABmEKT4ZgfYMryervRwrQhPcIj5A5VkP2+kcJcKFd/pcMH15A7Mt8M5ekcXYSYKe"
                + "tSeukBzWkJvGB+CEYl/1IRQYcJufIVERDdJ2C1HMs75lXp+ljMNBBu8frin+b7aI"
                + "TJTuoqrJIW2VjeMOhSFTyi4NDmlCRy/tXArQ4xcxAoGAUppOsJZeF/1kPQIFwBkS"
                + "bVuGxMscWKswHy6dXEq2VabVGBL8H33PkpJRBnw7S/f+8wvk9dX63NuTF6VYM546"
                + "J73YadnpU82C+7OnaTTCDVPfXYgPFLpE9xKFKkRFacgUbEnvZ2i0zSUquH0RAyaK"
                + "tJ0d/dnd5TQUccAZwT8Nrw0=";

        String pubKeyString = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs0sI/nP7okc+QDog1rFe"
                + "acSa3c5Q0VdjyisERgs4b9TBD8EArxaesGUQ4AhOBH6VVLgcjSJ1dXjn6wY8CJca"
                + "fIb/UT2AgLDwhVeOlS3mbK/BTn76iOiLMGKgd6sHYuTVvgriUS4ExST/O1+RoKCL"
                + "VZxNPw8SLmaG7mhBMz2vE1fJFL3Xoc8U5ZDE2suSf1EnsirFIz6M6oCrTeEnAlq0"
                + "MdhVxLiqAoPOcDYBUSR4EhgR9O5TQGFdrTs/sNE6EjcyrMDaoU6UZe1utYMwwhSb"
                + "uiGK3h/Wx+Xof2fQc4zTrNepNDpNj5kIToR2Ak933ZCqIK4kkRFTeXZR4hVe3gee"
                + "+wIDAQAB";

        String fakeBody = "{\n"
                + "    \"s3_bucket_name\": \"dataverse-export-dev\",\n"
                + "    \"package_id\": \"doi-10-5072-fk2-e6cmkr.v1.18\",\n"
                + "    \"s3_path\": \"doi-10-5072-fk2-e6cmkr\",\n"
                + "    \"admin_metadata\": {\n"
                + "        \"accessFlag\": \"N\",\n"
                + "        \"contentModel\": \"opaque\",\n"
                + "        \"depositingSystem\": \"Harvard Dataverse\",\n"
                + "        \"firstGenerationInDrs\": \"unspecified\",\n"
                + "        \"objectRole\": \"CG:DATASET\",\n"
                + "        \"usageClass\": \"LOWUSE\",\n"
                + "        \"storageClass\": \"AR\",\n"
                + "        \"s3_bucket_name\": \"dataverse-export-dev\",\n"
                + "        \"ownerCode\": \"123\",\n"
                + "        \"billingCode\": \"456\",\n"
                + "        \"resourceNamePattern\": \"pattern\",\n"
                + "        \"urnAuthorityPath\": \"path\",\n"
                + "        \"depositAgent\": \"789\",\n"
                + "        \"depositAgentEmail\": \"someone@mailinator.com\",\n"
                + "        \"successEmail\": \"winner@mailinator.com\",\n"
                + "        \"failureEmail\": \"loser@mailinator.com\",\n"
                + "        \"successMethod\": \"method\",\n"
                + "        \"adminCategory\": \"root\"\n"
                + "    }\n"
                + "}";
        
        byte[] encoded = Base64.getDecoder().decode(privKeyString);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            RSAPrivateKey privKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
            //RSAPublicKey publicKey;
            /*
             * If public key is needed: encoded = Base64.decodeBase64(publicKeyPEM);
             * 
             * KeyFactory keyFactory = KeyFactory.getInstance("RSA"); X509EncodedKeySpec
             * keySpec = new X509EncodedKeySpec(encoded); return (RSAPublicKey)
             * keyFactory.generatePublic(keySpec); RSAPublicKey publicKey = new
             * RSAPublicKey(System.getProperty(RS256_KEY));
             * 
             * 
             */
            String canonicalBody = new JsonCanonicalizer(fakeBody).getEncodedString();
            System.out.println("Canonical form:"+ canonicalBody);
            
            Algorithm algorithmRSA = Algorithm.RSA256(null, privKey);
            String token1 = DRSSubmitToArchiveCommand.createJWTString(algorithmRSA, "InstallationBrandName", fakeBody, 5);
            
            System.out.println("JWT: " + token1);
            DecodedJWT jwt = JWT.decode(token1);
            System.out.println(jwt.getPayload());
        } catch (Exception e) {
            System.out.println(e.getClass() + e.getLocalizedMessage());
            e.printStackTrace();
            //Any exception is a failure, otherwise decoding worked.
            fail(e.getLocalizedMessage());
        }

    }
}
