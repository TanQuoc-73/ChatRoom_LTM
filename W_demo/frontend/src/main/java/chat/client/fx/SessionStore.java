package chat.client.fx;

/**
 * Lưu trữ thông tin phiên đăng nhập của người dùng
 * Singleton pattern - dữ liệu được chia sẻ toàn ứng dụng
 */
public class SessionStore {

    // ========== PRIVATE FIELDS ==========
    private static String sessionToken;
    private static Long userId;  // Dùng Long thay vì long để có thể null
    private static String username;
    private static String displayName;
    private static String avatarUrl;

    // ========== TOKEN MANAGEMENT ==========

    /**
     * Lưu JWT token
     */
    public static void setSessionToken(String token) {
        sessionToken = token;
    }

    /**
     * Lấy JWT token hiện tại
     */
    public static String getSessionToken() {
        return sessionToken;
    }

    /**
     * Kiểm tra token có hợp lệ không
     */
    public static boolean hasValidToken() {
        return sessionToken != null && !sessionToken.trim().isEmpty();
    }

    // ========== USER ID MANAGEMENT ==========

    /**
     * Lưu User ID
     */
    public static void setUserId(Long id) {
        userId = id;
    }

    /**
     * Lấy User ID hiện tại
     */
    public static Long getUserId() {
        return userId;
    }

    /**
     * Kiểm tra User ID có hợp lệ không
     */
    public static boolean hasValidUserId() {
        return userId != null && userId > 0;
    }

    // ========== USERNAME MANAGEMENT ==========

    /**
     * Lưu username
     */
    public static void setUsername(String name) {
        username = name;
    }

    /**
     * Lấy username hiện tại
     */
    public static String getUsername() {
        return username;
    }

    // ========== DISPLAY NAME MANAGEMENT ==========

    /**
     * Lưu tên hiển thị
     */
    public static void setDisplayName(String name) {
        displayName = name;
    }

    /**
     * Lấy tên hiển thị
     */
    public static String getDisplayName() {
        return displayName;
    }

    // ========== AVATAR MANAGEMENT ==========

    /**
     * Lưu URL avatar
     */
    public static void setAvatarUrl(String url) {
        avatarUrl = url;
    }

    /**
     * Lấy URL avatar
     */
    public static String getAvatarUrl() {
        return avatarUrl;
    }

    // ========== SESSION VALIDATION ==========

    /**
     * Kiểm tra người dùng đã đăng nhập đầy đủ chưa
     * @return true nếu có token và userId hợp lệ
     */
    public static boolean isLoggedIn() {
        return hasValidToken() && hasValidUserId();
    }

    /**
     * Kiểm tra phiên có đầy đủ thông tin không
     */
    public static boolean isSessionComplete() {
        return hasValidToken() && hasValidUserId() && username != null;
    }

    // ========== SESSION OPERATIONS ==========

    /**
     * Khởi tạo phiên đăng nhập với thông tin đầy đủ
     */
    public static void initSession(String token, Long id, String user) {
        sessionToken = token;
        userId = id;
        username = user;
    }

    /**
     * Khởi tạo phiên đăng nhập với thông tin mở rộng
     */
    public static void initSession(String token, Long id, String user, String display, String avatar) {
        sessionToken = token;
        userId = id;
        username = user;
        displayName = display;
        avatarUrl = avatar;

    }


        private static String role;
    public static void setRole(String r) {
    role = r;
}

/**
 * Lấy role hiện tại
 */
public static String getRole() {
    return role;
}

/**
 * Kiểm tra admin
 */
public static boolean isAdmin() {
    return "ADMIN".equalsIgnoreCase(role);
}

/**
 * Kiểm tra moderator
 */
public static boolean isModerator() {
    return "MOD".equalsIgnoreCase(role) || isAdmin();
}

    /**
     * Xóa toàn bộ thông tin phiên (Đăng xuất)
     */
    public static void clearSession() {
        sessionToken = null;
        userId = null;
        username = null;
        displayName = null;
        avatarUrl = null;
        role = null;
    }

    /**
     * Cập nhật thông tin profile (không động đến token)
     */
    public static void updateProfile(String user, String display, String avatar) {
        username = user;
        displayName = display;
        avatarUrl = avatar;
    }

    // ========== DEBUG & UTILITY ==========

    /**
     * Lấy thông tin phiên dưới dạng String (để debug)
     */
    public static String getSessionInfo() {
        return String.format(
                "SessionStore { token: %s, userId: %s, username: %s, displayName: %s }",
                sessionToken != null ? "***" + sessionToken.substring(Math.max(0, sessionToken.length() - 8)) : "null",
                userId,
                username,
                displayName
        );
    }

    /**
     * Kiểm tra xem có dữ liệu nào trong session không
     */
    public static boolean isEmpty() {
        return sessionToken == null && userId == null && username == null;
    }

    /**
     * Reset về trạng thái ban đầu (tương tự clearSession nhưng rõ ràng hơn)
     */
    public static void reset() {
        clearSession();
    }


}