import { useState, useCallback, useEffect } from 'react';
import { UserService } from '@/services/user.service';
import type { UserProfile } from '@/types/user.types';

interface UseUserSearchReturn {
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  users: UserProfile[];  // Changed to array
  isLoading: boolean;
  error: string | null;
  searchUsers: (query: string) => Promise<void>;
  clearSearch: () => void;
}

/**
 * Custom hook for user search functionality
 * Provides debounced search with loading and error states
 */
export function useUserSearch(debounceMs: number = 300): UseUserSearchReturn {
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [users, setUsers] = useState<UserProfile[]>([]);  // Changed to array
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Debounced search function
  const searchUsers = useCallback(async (query: string) => {
    if (!query || query.trim().length === 0) {
      setUsers([]);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await UserService.searchUsers(query);
      
      if (response.success && response.data) {
        setUsers(response.data);  // Already an array
        setError(null);
      } else {
        setUsers([]);
        setError(response.error || 'Failed to search users');
      }
    } catch (err) {
      setUsers([]);
      setError('An unexpected error occurred');
      console.error('Search error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Debounce effect
  useEffect(() => {
    if (!searchQuery || searchQuery.trim().length === 0) {
      setUsers([]);
      setError(null);
      return;
    }

    const timeoutId = setTimeout(() => {
      searchUsers(searchQuery);
    }, debounceMs);

    return () => clearTimeout(timeoutId);
  }, [searchQuery, debounceMs, searchUsers]);

  const clearSearch = useCallback(() => {
    setSearchQuery('');
    setUsers([]);
    setError(null);
    setIsLoading(false);
  }, []);

  return {
    searchQuery,
    setSearchQuery,
    users,
    isLoading,
    error,
    searchUsers,
    clearSearch,
  };
}
