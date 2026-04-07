import { DateUtil } from './date.util';

describe('DateUtil', () => {

  it('should create an instance from a timestamp', () => {
    const util = new DateUtil(1700000000000); // 2023-11-14T22:13:20Z
    expect(util).toBeTruthy();
  });

  describe('minDate', () => {
    it('should return local midnight for the given date', () => {
      // Use a local-time timestamp to avoid cross-day ambiguity
      const input = new Date(2023, 5, 15, 14, 30, 0); // June 15, 2023 14:30 local
      const util = new DateUtil(input.getTime());

      const min = util.minDate();
      const d = new Date(min);

      expect(d.getFullYear()).toBe(2023);
      expect(d.getMonth()).toBe(5); // June
      expect(d.getDate()).toBe(15);
      expect(d.getHours()).toBe(0);
      expect(d.getMinutes()).toBe(0);
      expect(d.getSeconds()).toBe(0);
    });

    it('should return midnight even for a timestamp at 23:59:59', () => {
      const input = new Date(2024, 0, 1, 23, 59, 59); // Jan 1, 2024 23:59:59 local
      const util = new DateUtil(input.getTime());

      const min = util.minDate();
      const d = new Date(min);

      expect(d.getHours()).toBe(0);
      expect(d.getMinutes()).toBe(0);
      expect(d.getSeconds()).toBe(0);
    });
  });

  describe('maxDate', () => {
    it('should return local 23:59:59 for the given date', () => {
      const input = new Date(2023, 5, 15, 8, 0, 0); // June 15, 2023 08:00 local
      const util = new DateUtil(input.getTime());

      const max = util.maxDate();
      const d = new Date(max);

      expect(d.getFullYear()).toBe(2023);
      expect(d.getMonth()).toBe(5);
      expect(d.getDate()).toBe(15);
      expect(d.getHours()).toBe(23);
      expect(d.getMinutes()).toBe(59);
      expect(d.getSeconds()).toBe(59);
    });
  });

  describe('minDate and maxDate span exactly one day', () => {
    it('should differ by 23:59:59', () => {
      const input = new Date(2024, 2, 10, 12, 0, 0); // March 10, 2024 12:00 local
      const util = new DateUtil(input.getTime());

      const diff = util.maxDate() - util.minDate();
      // 23h * 3600 + 59m * 60 + 59s = 86399 seconds
      expect(diff).toBe(86399 * 1000);
    });
  });

  describe('edge cases', () => {
    it('should handle new year boundary', () => {
      const input = new Date(2024, 11, 31, 23, 59, 59); // Dec 31 local
      const util = new DateUtil(input.getTime());

      const min = new Date(util.minDate());
      const max = new Date(util.maxDate());

      expect(min.getFullYear()).toBe(2024);
      expect(min.getMonth()).toBe(11);
      expect(min.getDate()).toBe(31);
      expect(max.getDate()).toBe(31);
    });

    it('should handle leap day', () => {
      const input = new Date(2024, 1, 29, 12, 0, 0); // Feb 29, 2024 local
      const util = new DateUtil(input.getTime());

      const min = new Date(util.minDate());
      expect(min.getMonth()).toBe(1);
      expect(min.getDate()).toBe(29);
    });
  });
});
