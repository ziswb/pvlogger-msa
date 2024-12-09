import { Component, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDatepicker, MatDatepickerInputEvent } from '@angular/material/datepicker';
import { AppBus } from '../service/app-bus.service';

@Component({
  selector: 'app-datepicker',
  templateUrl: './datepicker.component.html',
  styleUrls: ['./datepicker.component.sass']
})
export class DatepickerComponent {

  minDate: Date;
  maxDate: Date;
  startDate: Date;
  selectedDate: FormControl;
  sunrise = "8:09"
  sunset = "17:11"
  @ViewChild(MatDatepicker, {static: false}) calendar: MatDatepicker<Date> | undefined;

  constructor(private appBus: AppBus) {
    this.startDate = new Date();
    this.selectedDate = new FormControl(this.startDate);
    this.minDate = new Date(2022, 9, 25);
    this.maxDate = this.startDate;
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
      if (this.calendar != undefined) {
        this.calendar.select(newDate);
      } else {
        console.log("calender is undefined");
      }
    }
  }

  nextClicked() {
    let newDate = new Date(this.selectedDate.value.getTime() + 24*60*60*1000);
    if (newDate.getTime() <= this.maxDate.getTime()) {
      this.selectedDate.setValue(newDate);
      this.startDate = newDate;
      if (this.calendar != undefined) {
        this.calendar.select(newDate);
      } else {
        console.log("calender is undefined");
      }
    }
  }
}
