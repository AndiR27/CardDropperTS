import { PackSlot } from './pack-slot.model';

export interface PackTemplate {
  id: number | null;
  name: string;
  slots: PackSlot[];
}
