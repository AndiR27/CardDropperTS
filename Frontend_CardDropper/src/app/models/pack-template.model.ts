import { PackTemplateSlot } from './pack-template-slot.model';

export interface PackTemplate {
  id: number | null;
  name: string;
  slots: PackTemplateSlot[];
}
