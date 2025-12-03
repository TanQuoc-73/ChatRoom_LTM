package chat.client.fx;

public class SessionStore {
    private static String sessionToken;
    private static long userId;
    private static String username;

    public static String getSessionToken() {
        return sessionToken;
    }

    public static void setSessionToken(String token) {
        sessionToken = token;
    }

    public static long getUserId() {
        return userId;
    }

    public static void setUserId(long id) {
        userId = id;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String name) {
        username = name;
    }
}
