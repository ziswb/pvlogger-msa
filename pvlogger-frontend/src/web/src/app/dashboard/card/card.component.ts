import { Component, Input } from '@angular/core';
import { MatCard, MatCardHeader, MatCardTitle, MatCardContent } from '@angular/material/card';

@Component({
    selector: 'app-card',
    templateUrl: './card.component.html',
    styleUrls: ['./card.component.sass'],
    imports: [MatCard, MatCardHeader, MatCardTitle, MatCardContent]
})
export class CardComponent {
  @Input() title: String = '';
}
