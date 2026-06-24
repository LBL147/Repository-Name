export interface NewsItemResponse {
  id: number;
  title: string;
  url: string;
  source: string;
  keyword: string;
  publishedAt?: string;
  fetchedAt?: string;
}

export interface RefreshNewsResponse {
  records: NewsItemResponse[];
  fetchedCount: number;
  insertedCount: number;
  source: string;
  cacheFallback: boolean;
  message?: string;
}

export interface TaskNewsResponse {
  id: number;
  newsId: number;
  title: string;
  url: string;
  source: string;
  keyword: string;
  publishedAt?: string;
  fetchedAt?: string;
  associatedAt?: string;
}

export interface RefreshTaskNewsResponse {
  keyword: string;
  records: TaskNewsResponse[];
  fetchedCount: number;
  insertedCount: number;
  associatedCount: number;
  source: string;
  cacheFallback: boolean;
  refreshSucceeded: boolean;
  message?: string;
}

export interface NewsQuery {
  keyword?: string;
  page?: number;
  size?: number;
}

export interface RefreshNewsPayload {
  keyword: string;
}

export interface RefreshTaskNewsPayload {
  keyword?: string;
}
