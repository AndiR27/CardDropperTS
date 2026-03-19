export interface PageResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;       // current page (0-based)
    totalElements: number;
    totalPages: number;
  };
}
