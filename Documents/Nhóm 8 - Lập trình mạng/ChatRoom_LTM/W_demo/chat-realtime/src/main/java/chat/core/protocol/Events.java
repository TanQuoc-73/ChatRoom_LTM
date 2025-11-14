package chat.core.protocol;

public class Events {

    public static class UserJoined {
        public final String roomId;
        public final String username;
        public UserJoined(String roomId, String username) {
            this.roomId = roomId; this.username = username;
        }
    }

    public static class UserLeft {
        public final String roomId;
        public final String username;
        public UserLeft(String roomId, String username) {
            this.roomId = roomId; this.username = username;
        }
    }

    public static class UserTyping {
        public final String roomId;
        public final String username;
        public final boolean typing;
        public UserTyping(String roomId, String username, boolean typing) {
            this.roomId = roomId; this.username = username; this.typing = typing;
        }
    }

    public static class Ack {
        public final String messageId;
        public final String to;
        public Ack(String messageId, String to) {
            this.messageId = messageId; this.to = to;
        }
    }
}
