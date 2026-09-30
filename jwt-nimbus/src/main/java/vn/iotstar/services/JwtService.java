package vn.iotstar.services;

import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import vn.iotstar.exception.InvalidTokenException;
import vn.iotstar.exception.TokenExpiredException;

@Service
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }

    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        final JWTClaimsSet claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        try {
            long now = System.currentTimeMillis();

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userDetails.getUsername())
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + expiration));
            extraClaims.forEach(claimsBuilder::claim);

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256)
                            .type(com.nimbusds.jose.JOSEObjectType.JWT)
                            .build(),
                    claimsBuilder.build());

            signedJWT.sign(new MACSigner(getSignInKey()));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot sign JWT", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime);
    }

    private JWTClaimsSet extractAllClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                throw new InvalidTokenException("Unsupported JWT algorithm: " + signedJWT.getHeader().getAlgorithm());
            }
            if (!signedJWT.verify(new MACVerifier(getSignInKey()))) {
                throw new InvalidTokenException("JWT signature does not match locally computed signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null) {
                throw new InvalidTokenException("JWT has no expiration time");
            }
            if (exp.before(new Date())) {
                throw new TokenExpiredException("JWT expired at " + exp);
            }
            return claims;
        } catch (ParseException e) {
            throw new InvalidTokenException("Invalid compact JWT string: " + e.getMessage(), e);
        } catch (JOSEException e) {
            throw new InvalidTokenException("Cannot verify JWT: " + e.getMessage(), e);
        }
    }

    private byte[] getSignInKey() {
        return Base64.getDecoder().decode(secretKey);
    }
}