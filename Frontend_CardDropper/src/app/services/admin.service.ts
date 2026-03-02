import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PackTemplate, Rarity, User } from '../models';

@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly api = inject(ApiService);

  // ====== Pack Templates ======

  getPackTemplates(): Observable<PackTemplate[]> {
    return this.api.get<PackTemplate[]>('/admin/pack-templates');
  }

  getPackTemplate(id: number): Observable<PackTemplate> {
    return this.api.get<PackTemplate>(`/admin/pack-templates/${id}`);
  }

  createPackTemplate(template: PackTemplate): Observable<PackTemplate> {
    return this.api.post<PackTemplate>('/admin/pack-templates', template);
  }

  updatePackTemplate(id: number, template: PackTemplate): Observable<PackTemplate> {
    return this.api.put<PackTemplate>(`/admin/pack-templates/${id}`, template);
  }

  deletePackTemplate(id: number): Observable<void> {
    return this.api.delete<void>(`/admin/pack-templates/${id}`);
  }

  // ====== Drop Rates ======

  updateDropRateByRarity(rarity: Rarity, dropRate: number): Observable<number> {
    return this.api.put<number>(
      `/admin/drop-rates/by-rarity?rarity=${rarity}&dropRate=${dropRate}`,
      null
    );
  }

  // ====== Users ======

  getUsers(): Observable<User[]> {
    return this.api.get<User[]>('/admin/users');
  }

  getUser(id: number): Observable<User> {
    return this.api.get<User>(`/admin/users/${id}`);
  }
}
