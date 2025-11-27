import { API_ENDPOINTS } from '../lib/constants';
import { SessionManager, handleApiError } from '../lib/utils/session';
import type { UserProfile, UserSearchResponse, UserSearchSingleResponse } from '@/types/user.types';

/**
 * User Service
 * Handles all user-related API calls
 */
export const UserService = {
  /**
   * Search users by username
   * @param username - Username to search for
   * @returns UserSearchResponse with user profile if found
   */
  searchUsers: async (username: string): Promise<UserSearchResponse> => {
    if (!username || username.trim().length === 0) {
      return {
        success: false,
        error: 'Username is required',
      };
    }

    try {
      const response = await fetch(API_ENDPOINTS.USERS.SEARCH(username), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...SessionManager.getAuthHeader(),
        },
      });

      if (!response.ok) {
        if (response.status === 404) {
          return {
            success: true,
            data: [], // No users found, return empty array
          };
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data: UserProfile[] = await response.json(); // Expect array from backend
      
      return {
        success: true,
        data,
      };
    } catch (error) {
      return {
        success: false,
        error: handleApiError(error),
      };
    }
  },

  /**
   * Get user profile by ID
   * @param userId - User ID to fetch
   * @returns UserSearchResponse with user profile
   */
  getUserProfile: async (userId: number): Promise<UserSearchSingleResponse> => {
    try {
      const response = await fetch(API_ENDPOINTS.USERS.BY_ID(userId), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...SessionManager.getAuthHeader(),
        },
      });

      if (!response.ok) {
        if (response.status === 404) {
          return {
            success: false,
            error: 'User not found',
          };
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data: UserProfile = await response.json();
      
      return {
        success: true,
        data,
      };
    } catch (error) {
      return {
        success: false,
        error: handleApiError(error),
      };
    }
  },

  /**
   * Get current user's profile
   * @returns UserSearchResponse with current user profile
   */
  getMyProfile: async (): Promise<UserSearchSingleResponse> => {
    try {
      const response = await fetch(API_ENDPOINTS.USERS.PROFILE, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...SessionManager.getAuthHeader(),
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data: UserProfile = await response.json();
      
      return {
        success: true,
        data,
      };
    } catch (error) {
      return {
        success: false,
        error: handleApiError(error),
      };
    }
  },
};
