export interface NewsItem {
  id: number;
  title: string;
  url: string;
  source: string;
  keyword: string;
  publishedAt?: string;
  fetchedAt?: string;
}

export interface RefreshNewsPayload {
  keyword: string;
}

export interface RefreshTaskNewsPayload {
  keyword?: string;
}
