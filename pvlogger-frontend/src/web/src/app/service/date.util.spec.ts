import { DateUtil } from './date.util';

describe('DateUtil', () => {

  it('should create an instance from a timestamp', () => {
    const util = new DateUtil(1700000000000); // 2023-11-14T22:13:20Z
    expect(util).toBeTruthy();
  });

  describe('minDate', () => {
    it('should return UTC midnight for the given date', () => {
      // 2023-06-15T14:30:00Z
      const timestamp = Date.UTC(2023, 5, 15, 14, 30, 0);
      const util = new DateUtil(timestamp);

      const min = util.minDate();
      const d = new Date(min);

      expect(d.getUTCFullYear()).toBe(2023);
      expect(d.getUTCMonth()).toBe(5); // June
      expect(d.getUTCDate()).toBe(15);
      expect(d.getUTCHours()).toBe(0);
      expect(d.getUTCMinutes()).toBe(0);
      expect(d.getUTCSeconds()).toBe(0);
    });

    it('should return midnight even for a timestamp at 23:59:59', () => {
      const timestamp = Date.UTC(2024, 0, 1, 23, 59, 59);
      const util = new DateUtil(timestamp);

      const min = util.minDate();
      const d = new Date(min);

      expect(d.getUTCHours()).toBe(0);
      expect(d.getUTCMinutes()).toBe(0);
      expect(d.getUTCSeconds()).toBe(0);
    });
  });

  describe('maxDate', () => {
    it('should return UTC 23:59:59 for the given date', () => {
      const timestamp = Date.UTC(2023, 5, 15, 8, 0, 0);
      const util = new DateUtil(timestamp);

      const max = util.maxDate();
      const d = new Date(max);

      expect(d.getUTCFullYear()).toBe(2023);
      expect(d.getUTCMonth()).toBe(5);
      expect(d.getUTCDate()).toBe(15);
      expect(d.getUTCHours()).toBe(23);
      expect(d.getUTCMinutes()).toBe(59);
      expect(d.getUTCSeconds()).toBe(59);
    });
  });

  describe('minDate and maxDate span exactly one day', () => {
    it('should differ by 23:59:59', () => {
      const timestamp = Date.UTC(2024, 2, 10, 12, 0, 0);
      const util = new DateUtil(timestamp);

      const diff = util.maxDate() - util.minDate();
      // 23h * 3600 + 59m * 60 + 59s = 86399 seconds
      expect(diff).toBe(86399 * 1000);
    });
  });

  describe('edge cases', () => {
    it('should handle new year boundary', () => {
      const timestamp = Date.UTC(2024, 11, 31, 23, 59, 59); // Dec 31
      const util = new DateUtil(timestamp);

      const min = new Date(util.minDate());
      const max = new Date(util.maxDate());

      expect(min.getUTCFullYear()).toBe(2024);
      expect(min.getUTCMonth()).toBe(11);
      expect(min.getUTCDate()).toBe(31);
      expect(max.getUTCDate()).toBe(31);
    });

    it('should handle leap day', () => {
      const timestamp = Date.UTC(2024, 1, 29, 12, 0, 0); // Feb 29, 2024
      const util = new DateUtil(timestamp);

      const min = new Date(util.minDate());
      expect(min.getUTCMonth()).toBe(1);
      expect(min.getUTCDate()).toBe(29);
    });
  });
});
