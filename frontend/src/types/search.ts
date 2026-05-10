export type SearchResultType =
  | 'PERSON'
  | 'ONE_ON_ONE_ENTRY'
  | 'QUICK_NOTE'
  | 'ACTION_ITEM'
  | 'PDP_GOAL'
  | 'PDP_UPDATE'
  | 'KUDOS';

export interface SearchResultItem {
  id: string;
  type: SearchResultType;
  title: string;
  snippet: string | null;
  personId: string | null;
  personName: string | null;
  sensitive: boolean;
  createdAt: string;
  relevanceScore: number;
}

export interface SearchResponse {
  results: SearchResultItem[];
  query: string;
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
}
