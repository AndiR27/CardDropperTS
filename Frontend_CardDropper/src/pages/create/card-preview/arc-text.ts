import { Component, input } from '@angular/core';

@Component({
  selector: 'app-arc-text',
  standalone: true,
  template: `
    <svg [attr.width]="width()" [attr.height]="height()"
         [attr.viewBox]="viewBox()" class="arc-text">
      <defs>
        <path [attr.id]="pathId()" [attr.d]="path()"/>
      </defs>
      <text>
        <textPath [attr.href]="'#' + pathId()" startOffset="50%" text-anchor="middle">
          <ng-content/>
        </textPath>
      </text>
    </svg>
  `,
  styles: `
    .arc-text {
      overflow: visible;
    }
    text {
      fill: white;
      font-family: 'GBJenLei', 'BlizzardGlobal', serif;
      stroke: black;
      paint-order: stroke fill;
    }
  `,
})
export class ArcText {
  path    = input.required<string>();
  width   = input<string>('100%');
  height  = input<string>('100%');
  viewBox = input<string | null>(null);
  pathId  = input<string>('arcPath');
}
