import { Component, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormsModule } from '@angular/forms';
import { MatDatepicker, MatDatepickerInputEvent, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { AppBus } from '../service/app-bus.service';
import { MatInput } from '@angular/material/input';
import { MatFormField, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';

@Component({
    selector: 'app-datepicker',
    templateUrl: './datepicker.component.html',
    styleUrls: ['./datepicker.component.sass'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [MatIconButton, MatIcon, MatFormField, MatInput, FormsModule, MatDatepickerInput, MatDatepickerToggle, MatSuffix, MatDatepicker]
})
export class DatepickerComponent {

  minDate: Date;
  maxDate: Date;
  startDate: Date;
  selectedDate: FormControl;
  @ViewChild(MatDatepicker, {static: false}) calendar: MatDatepicker<Date> | undefined;

  constructor(private appBus: AppBus) {
    this.startDate = new Date();
    this.selectedDate = new FormControl(this.startDate);
    this.minDate = new Date(2022, 9, 25);
    this.maxDate = this.startDate;
    // Follow date changes made elsewhere, e.g. an open tab moving on past midnight.
    this.appBus.dateChanged.pipe(takeUntilDestroyed()).subscribe((date) => {
      if (date > this.maxDate) {
        this.maxDate = date;
      }
      this.startDate = date;
      this.selectedDate.setValue(date);
    });
  }

  addEvent(type: string, event: MatDatepickerInputEvent<unknown, unknown | null>) {
    this.appBus.publishDateChanged(<Date> event.value);
    this.selectedDate = new FormControl(<Date> event.value);
  }

  previousDisabled() {
    return this.selectedDate.value.getTime() <= this.minDate.getTime();
  }

  nextDisabled() {
    return this.selectedDate.value.getTime() >= this.maxDate.getTime();
  }

  previousClicked() {
    let newDate = new Date(this.selectedDate.value.getTime() - 24*60*60*1000);
    if (newDate.getTime() >= this.minDate.getTime()) {
      this.selectedDate.setValue(newDate);
      this.startDate = newDate;
      this.calendar?.select(newDate);
    }
  }

  nextClicked() {
    let newDate = new Date(this.selectedDate.value.getTime() + 24*60*60*1000);
    if (newDate.getTime() <= this.maxDate.getTime()) {
      this.selectedDate.setValue(newDate);
      this.startDate = newDate;
      this.calendar?.select(newDate);
    }
  }
}
