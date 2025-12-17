package com.nhom8.chat.realtime;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.nhom8.chat.repository.ConversationRepository;
import com.nhom8.chat.service.ConversationService;

import chat.core.spi.RoomStore;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbRoomStore implements RoomStore {

    private final ConversationRepository convRepo;
    private final ConversationService conversationService;

    @Override
    public boolean roomExists(String roomId) {
        Long convId = parseConvId(roomId);
        return convRepo.existsById(convId);
    }

    @Override
    public void createRoom(String roomId, String createdBy) {
        // Không tạo room ở đây, vì đã có ConversationService + REST API
        // Tuỳ bạn: có thể throw hoặc để no-op
        throw new UnsupportedOperationException("Room is managed by ConversationService");
    }

    @Override
    public void deleteRoom(String roomId) {
        // Xoá room phải dùng ConversationService.deleteConversation
        throw new UnsupportedOperationException("Room is managed by ConversationService");
    }

    @Override
    public Set<String> getRoomUsers(String roomId) {
        Long convId = parseConvId(roomId);
        List<Long> memberIds = conversationService.getConversationMemberIds(convId);
        return memberIds.stream()
                .map(id -> "user_" + id)
                .collect(Collectors.toSet());
    }

    @Override
    public List<String> getAllRooms() {
        return convRepo.findAll().stream()
                .map(conv -> "conv_" + conv.getId())
                .collect(Collectors.toList());
    }

    @Override
    public void addUserToRoom(String roomId, String username) {
        // Thêm / xoá thành viên nên đi qua ConversationService (REST) thay vì SPI
        // Ở realtime, chỉ cần đảm bảo user online thì joinRoom() từ ChatRealtimeBridge
        // nên có thể để trống hoặc throw UnsupportedOperationException
    }

    @Override
    public void removeUserFromRoom(String roomId, String username) {
        // Tương tự addUserToRoom – quyền sửa member ở ConversationService
    }

    private Long parseConvId(String roomId) {
        if (!roomId.startsWith("conv_")) {
            throw new IllegalArgumentException("Invalid roomId: " + roomId);
        }
        return Long.parseLong(roomId.substring(5));
    }
}
