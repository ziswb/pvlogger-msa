export class DateUtil {

  private readonly _newDate = new Date();

  constructor(date: number) {
    this._newDate = new Date(date);
  }

  minDate() {
    return new Date(this._newDate.getFullYear(), this._newDate.getMonth(), this._newDate.getDate(), 0, 0, 0).getTime();
  }

  maxDate() {
    return new Date(this._newDate.getFullYear(), this._newDate.getMonth(), this._newDate.getDate(), 23, 59, 59).getTime();
  }
}
