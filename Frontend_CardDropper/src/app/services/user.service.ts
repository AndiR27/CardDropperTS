import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { User } from '../models';

@Injectable({ providedIn: 'root' })
export class UserService {

  private readonly api = inject(ApiService);

  getAll(): Observable<User[]> {
    return this.api.get<User[]>('/users');
  }

  /** Public endpoint — no admin role required */
  getAllPublic(): Observable<User[]> {
    return this.api.get<User[]>('/me/users');
  }

  getByUsername(username: string): Observable<User> {
    return this.api.get<User>(`/users/by-username?username=${username}`);
  }

  getByEmail(email: string): Observable<User> {
    return this.api.get<User>(`/users/by-email?email=${email}`);
  }

  create(user: User): Observable<User> {
    return this.api.post<User>('/users', user);
  }

  update(id: number, user: User): Observable<User> {
    return this.api.put<User>(`/users/${id}`, user);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`/users/${id}`);
  }
}
