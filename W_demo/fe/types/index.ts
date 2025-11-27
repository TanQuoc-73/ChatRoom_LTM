// Authentication Types
export interface LoginRequest {
  username: string;
  password: string;
  deviceType?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
  firstName?: string;
  lastName?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  otp: string;
  newPassword: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  userId?: number;
  username?: string;
  displayName?: string;
  sessionToken?: string;
}

// User Types
export interface User {
  id: number;
  username: string;
  email: string;
  displayName?: string;
  firstName?: string;
  lastName?: string;
}

// Message Types
export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  messageType: string;
  sentAt: string;
  attachmentIds?: number[];
  clientCid?: string;
  isEdited: boolean;
}

export interface MessageRequest {
  conversationId: number;
  content: string;
  messageType?: string;
  attachmentMediaIds?: number[];
  clientCid?: string;
}

// Conversation Types
export interface Conversation {
  id: number;
  name?: string;
  type: 'PRIVATE' | 'GROUP';
  createdAt: string;
  lastActivity?: string;
  lastMessageId?: number;
}

export interface ConversationCreateRequest {
  name?: string;
  type: 'PRIVATE' | 'GROUP';
  memberUserIds: number[];
}

// Re-export user types
export * from './user.types';
