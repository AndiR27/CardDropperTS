import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import type { DodgeScore } from '../models/dodge-score.model';

export interface DodgeStartResponse {
  token: string;
}

@Injectable({ providedIn: 'root' })
export class DodgeService {

  private readonly api = inject(ApiService);

  startGame(): Observable<DodgeStartResponse> {
    return this.api.post<DodgeStartResponse>('/me/dodge/start', {});
  }

  submitScore(token: string, score: number): Observable<DodgeScore> {
    return this.api.post<DodgeScore>('/me/dodge/score', { token, score });
  }

  getMyScore(): Observable<DodgeScore> {
    return this.api.get<DodgeScore>('/me/dodge/score');
  }

  getLeaderboard(): Observable<DodgeScore[]> {
    return this.api.get<DodgeScore[]>('/me/dodge/leaderboard');
  }
}
