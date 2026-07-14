export interface Result<T> {
  success: boolean
  errorMsg: string | null
  data: T
  total: number | null
}

export interface ScrollResult<T> {
  list: T[]
  minTime: number
  offset: number
}

export class ApiError extends Error {
  readonly status?: number

  constructor(message: string, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}
