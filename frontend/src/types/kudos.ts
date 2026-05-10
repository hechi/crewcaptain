// Kudos Types
// Matches API contract from KudosResponses.kt and KudosRequests.kt

export interface Kudos {
  id: string;
  personId: string;
  date: string;
  text: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateKudosRequest {
  date?: string;
  text: string;
  tags?: string[];
}

export interface PaginatedKudosResponse {
  content: Kudos[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
