package chat.client.fx;

/**
 * Lưu trữ thông tin phiên đăng nhập của người dùng.
 * Sử dụng static fields để chia sẻ dữ liệu trên toàn ứng dụng (Singleton Pattern).
 */
public class SessionStore {

    // ========== PRIVATE FIELDS ==========
    private static String sessionToken;
    private static Long userId;
    private static String username;
    private static String displayName;
    private static String avatarUrl;
    private static String role; //

    // ========== TOKEN MANAGEMENT ==========

    /**
     * Lưu JWT token.
     */
    public static void setSessionToken(String token) {
        sessionToken = token;
    }

    /**
     * Lấy JWT token hiện tại.
     */
    public static String getSessionToken() {
        return sessionToken;
    }

    /**
     * Kiểm tra token có hợp lệ không.
     */
    public static boolean hasValidToken() {
        return sessionToken != null && !sessionToken.trim().isEmpty();
    }

    // ========== USER ID MANAGEMENT ==========

    /**
     * Lưu User ID.
     */
    public static void setUserId(Long id) {
        userId = id;
    }

    /**
     * Lấy User ID hiện tại.
     */
    public static Long getUserId() {
        return userId;
    }

    /**
     * Kiểm tra User ID có hợp lệ không.
     */
    public static boolean hasValidUserId() {
        return userId != null && userId > 0;
    }

    // ========== USERNAME MANAGEMENT ==========

    /**
     * Lưu username.
     */
    public static void setUsername(String name) {
        username = name;
    }

    /**
     * Lấy username hiện tại.
     */
    public static String getUsername() {
        return username;
    }

    // ========== DISPLAY NAME MANAGEMENT ==========

    /**
     * Lưu tên hiển thị.
     */
    public static void setDisplayName(String name) {
        displayName = name;
    }

    /**
     * Lấy tên hiển thị.
     */
    public static String getDisplayName() {
        return displayName;
    }

    // ========== AVATAR MANAGEMENT ==========

    /**
     * Lưu URL avatar.
     */
    public static void setAvatarUrl(String url) {
        avatarUrl = url;
    }

    /**
     * Lấy URL avatar.
     */
    public static String getAvatarUrl() {
        return avatarUrl;
    }

    // ========== ROLE MANAGEMENT ==========

    /**
     * Lưu vai trò của người dùng hiện tại.
     */
    public static void setRole(String r) {
        role = r;
    }

    /**
     * Lấy vai trò hiện tại của người dùng.
     */
    public static String getRole() {
        return role;
    }

    /**
     * Kiểm tra người dùng hiện tại có phải là Admin không.
     * @return true nếu vai trò là "ADMIN".
     */
    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    /**
     * Kiểm tra người dùng hiện tại có phải là Moderator hoặc Admin không.
     * @return true nếu vai trò là "MOD" hoặc "ADMIN".
     */
    public static boolean isModerator() {
        return "MOD".equalsIgnoreCase(role) || isAdmin();
    }

    // ========== SESSION OPERATIONS ==========

    /**
     * Khởi tạo phiên đăng nhập với thông tin đầy đủ, bao gồm cả vai trò.
     */
    public static void initSession(String token, Long id, String user, String display, String avatar, String r) {
        sessionToken = token;
        userId = id;
        username = user;
        displayName = display;
        avatarUrl = avatar;
        role = r;
    }

    /**
     * Xóa toàn bộ thông tin phiên (Đăng xuất).
     */
    public static void clearSession() {
        sessionToken = null;
        userId = null;
        username = null;
        displayName = null;
        avatarUrl = null;
        role = null; // <-- Đừng quên xóa vai trò
    }

    // ========== DEBUG & UTILITY ==========

    /**
     * Lấy thông tin phiên dưới dạng String (để debug).
     */
    public static String getSessionInfo() {
        return String.format(
                "SessionStore { token: %s, userId: %s, username: %s, displayName: %s, role: %s }",
                sessionToken != null ? "***" + sessionToken.substring(Math.max(0, sessionToken.length() - 8)) : "null",
                userId,
                username,
                displayName,
                role
        );
    }

    /**
     * Kiểm tra xem có dữ liệu nào trong session không.
     */
    public static boolean isEmpty() {
        return sessionToken == null && userId == null && username == null;
    }

    /**
     * Reset về trạng thái ban đầu (tương tự clearSession nhưng rõ ràng hơn).
     */
    public static void reset() {
        clearSession();
    }
}