import {Component, inject} from '@angular/core';
import {CardService} from '../../../app/services/card.service';
import {Card} from '../../../app/models';

@Component({
  selector: 'app-cards',
  imports: [],
  templateUrl: './cards.html',
  styleUrl: './cards.scss',
})
export class Cards {
  cardService = inject(CardService);

  cards: Card[] = [];

  constructor() {
    this.loadCards();
  }

  //Charge les cartes depuis le service et les stocke dans la propriété cards
  loadCards() {
    this.cardService.getAll().subscribe(cards => {
      this.cards = cards;
    });
    //order by rarity/name
    this.cards.sort((a, b) => {
      if (a.rarity === b.rarity) {
        return a.name.localeCompare(b.name);
      }
      return a.rarity.localeCompare(b.rarity);
    });
  }

  //Filtrer les cartes par rareté
  filterByRarity(rarity: string) {
    return this.cards.filter(card => card.rarity === rarity);
  }

  //Filtrer les cartes par targetUserId mais en choisissant dans une liste déroulante de Users
  filterByTargetUserId(targetUserId: number) {
    return this.cards.filter(card => card.targetUserId === targetUserId);
  }





}
