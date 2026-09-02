import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  template: `
    <span class="star-rating">
      @for (star of [1, 2, 3, 4, 5]; track star) {
        <span
          class="star"
          [class.filled]="star <= value"
          [class.interactive]="interactive"
          (click)="onClick(star)"
          >★</span
        >
      }
    </span>
  `,
  styles: [
    `
      .star-rating {
        display: inline-flex;
        gap: 2px;
      }
      .star {
        color: #d0d0d0;
        font-size: 1.1rem;
      }
      .star.filled {
        color: #f5a623;
      }
      .star.interactive {
        cursor: pointer;
      }
    `,
  ],
})
export class StarRatingComponent {
  @Input() value = 0;
  @Input() interactive = false;
  @Output() valueChange = new EventEmitter<number>();

  onClick(star: number): void {
    if (this.interactive) {
      this.valueChange.emit(star);
    }
  }
}
