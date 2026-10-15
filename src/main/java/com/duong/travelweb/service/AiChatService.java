package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.ChatFeedbackRequestDTO;
import com.duong.travelweb.model.dto.ChatMessageDTO;
import com.duong.travelweb.model.dto.ChatReplyDTO;
import com.duong.travelweb.model.dto.ChatSendRequestDTO;
import com.duong.travelweb.model.dto.ChatSessionCreateRequestDTO;
import com.duong.travelweb.model.dto.ChatSessionDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Trợ lý AI cho khách đã đăng nhập: phiên chat, tin nhắn, đánh giá câu trả lời. */
public interface AiChatService {
    ChatSessionDTO createSession(UUID userId, ChatSessionCreateRequestDTO request);

    /** Các phiên đang mở của user, mới cập nhật trước. */
    Page<ChatSessionDTO> findSessions(UUID userId, int page, int limit);

    /** Đóng phiên (ẩn khỏi danh sách, không xem/gửi tiếp được). */
    void closeSession(UUID userId, UUID sessionId);

    List<ChatMessageDTO> getMessages(UUID userId, UUID sessionId);

    /**
     * Gửi câu hỏi và nhận trả lời. Dịch vụ AI lỗi / hết hạn mức thì câu trả lời là tin nhắn lỗi
     * (isError = true) chứ không ném lỗi, để khách thấy trong lịch sử và thử lại.
     */
    ChatReplyDTO sendMessage(UUID userId, UUID sessionId, ChatSendRequestDTO request);

    /**
     * Trả lời dạng stream, bước 1 (đồng bộ, trong request): kiểm tra + lưu câu hỏi. Lỗi (rỗng, quá dài, quá tần suất,
     * phiên không có) ném ra như sendMessage.
     */
    PendingQuestion submitQuestion(UUID userId, UUID sessionId, ChatSendRequestDTO request);

    /**
     * Bước 2 (chạy nền): gọi AI dạng stream, mỗi đoạn chữ gọi onDelta; cuối cùng lưu và trả câu trả lời như sendMessage
     * (AI lỗi → tin nhắn lỗi). Không ném lỗi do onDelta (khách ngắt kết nối vẫn lưu đủ câu trả lời).
     */
    ChatReplyDTO answerStreaming(PendingQuestion pending, Consumer<String> onDelta);

    /** Câu hỏi đã lưu, chờ trả lời — chỉ để chuyển từ submitQuestion sang answerStreaming. */
    interface PendingQuestion {
    }

    /** Đánh giá 1 câu trả lời của trợ lý trong phiên của chính user; gửi lại thì ghi đè. */
    ChatMessageDTO sendFeedback(UUID userId, UUID messageId, ChatFeedbackRequestDTO request);
}
