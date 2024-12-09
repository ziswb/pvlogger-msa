export class DateUtil {

  private readonly _newDate = new Date();

  constructor(date: number) {
    this._newDate = new Date(date);
  }

  minDate() {
    return Date.UTC(this._newDate.getUTCFullYear(), this._newDate.getUTCMonth(), this._newDate.getUTCDate(), 0, 0, 0);
  }

  maxDate() {
    return Date.UTC(this._newDate.getUTCFullYear(), this._newDate.getUTCMonth(), this._newDate.getUTCDate(), 23, 59, 59);
  }
}
