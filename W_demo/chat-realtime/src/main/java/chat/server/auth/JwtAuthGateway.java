package chat.server.auth;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import chat.core.spi.AuthGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtAuthGateway implements AuthGateway {

    private final SecretKey key;
    private final Map<String, Set<String>> userRoomMembership = new ConcurrentHashMap<>();

    public JwtAuthGateway(String secretKey) {
       
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Override
    public boolean authenticate(String username, String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenUser = claims.getSubject();
            return tokenUser.equals(username);

        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean isUserInRoom(String username, String roomId) {
        return userRoomMembership
                .getOrDefault(roomId, Set.of())
                .contains(username);
    }

    @Override
    public boolean canUserJoinRoom(String username, String roomId) {
        return true; 
    }

    // server cần gọi khi user join room
    public void registerJoin(String username, String roomId) {
        userRoomMembership
                .computeIfAbsent(roomId, r -> ConcurrentHashMap.newKeySet())
                .add(username);
    }

    public void registerLeave(String username, String roomId) {
        Set<String> set = userRoomMembership.get(roomId);
        if (set != null) set.remove(username);
    }
}
