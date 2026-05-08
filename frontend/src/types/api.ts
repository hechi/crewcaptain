export interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

export class ApiException extends Error {
  constructor(
    public readonly status: number,
    public readonly error: string,
    message: string,
    public readonly timestamp: string
  ) {
    super(message);
    this.name = 'ApiException';
  }
}
