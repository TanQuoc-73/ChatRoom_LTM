/**
 * User-related TypeScript type definitions
 * Matches backend UserProfileDTO structure
 */

export interface UserProfile {
  userId: number;
  username: string;
  email: string;
  displayName?: string;
  firstName?: string;
  lastName?: string;
  bio?: string;
  website?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';
  dateOfBirth?: string; // ISO date string
  verified?: boolean;
  lastActive?: string; // ISO datetime string
  avatarUrl?: string;
  coverUrl?: string;
}

export interface UserSearchParams {
  username: string;
}

export interface UserSearchResponse {
  success: boolean;
  data?: UserProfile[];  // Changed to array to support multiple results
  error?: string;
}

// For backward compatibility - single user response
export interface UserSearchSingleResponse {
  success: boolean;
  data?: UserProfile;
  error?: string;
}

// For future pagination support
export interface UserSearchPaginatedResponse {
  success: boolean;
  data?: UserProfile[];
  total?: number;
  page?: number;
  pageSize?: number;
  error?: string;
}
