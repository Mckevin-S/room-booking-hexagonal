import {
  addDays,
  ceilToQuarter,
  dayLabel,
  durationLabel,
  floorToQuarter,
  fromMinutes,
  hourLabel,
  isWeekend,
  nextWorkingDay,
  quarterOptions,
  timeAtOffset,
  timelineOffset,
  toMinutes,
} from './time';

describe('heures et dates', () => {
  it('convertit les heures en minutes et inversement', () => {
    expect(toMinutes('09:30')).toBe(570);
    expect(fromMinutes(570)).toBe('09:30');
    expect(fromMinutes(toMinutes('20:00'))).toBe('20:00');
  });

  it('arrondit au quart d’heure', () => {
    expect(ceilToQuarter(toMinutes('09:01'))).toBe(toMinutes('09:15'));
    expect(ceilToQuarter(toMinutes('09:15'))).toBe(toMinutes('09:15'));
    expect(floorToQuarter(toMinutes('09:14'))).toBe(toMinutes('09:00'));
  });

  it('propose chaque quart d’heure de la plage d’ouverture', () => {
    const options = quarterOptions('08:00', '09:00');
    expect(options).toEqual(['08:00', '08:15', '08:30', '08:45', '09:00']);
  });

  it('affiche heures et durées à la française', () => {
    expect(hourLabel('09:30')).toBe('9h30');
    expect(hourLabel('14:00')).toBe('14h00');
    expect(durationLabel('10:00', '10:45')).toBe('45 min');
    expect(durationLabel('10:00', '12:00')).toBe('2 h');
    expect(durationLabel('10:00', '11:30')).toBe('1 h 30');
  });

  it('nomme les jours proches', () => {
    expect(dayLabel('2026-12-01', '2026-12-01')).toBe("aujourd'hui");
    expect(dayLabel('2026-12-02', '2026-12-01')).toBe('demain');
    expect(dayLabel('2026-12-04', '2026-12-01')).toBe('vendredi 4 décembre');
  });

  it('saute le week-end', () => {
    expect(isWeekend('2026-12-05')).toBe(true);
    expect(isWeekend('2026-12-04')).toBe(false);
    expect(nextWorkingDay('2026-12-05')).toBe('2026-12-07');
    expect(nextWorkingDay('2026-12-04')).toBe('2026-12-04');
    expect(addDays('2026-11-30', 2)).toBe('2026-12-02');
  });
});

describe('frise du planning', () => {
  it('place une heure proportionnellement à la journée d’ouverture', () => {
    expect(timelineOffset('08:00', '08:00', '20:00')).toBe(0);
    expect(timelineOffset('14:00', '08:00', '20:00')).toBe(50);
    expect(timelineOffset('20:00', '08:00', '20:00')).toBe(100);
  });

  it('borne les heures hors ouverture', () => {
    expect(timelineOffset('06:00', '08:00', '20:00')).toBe(0);
    expect(timelineOffset('22:00', '08:00', '20:00')).toBe(100);
  });

  it('retrouve le quart d’heure visé par un clic', () => {
    expect(timeAtOffset(0.5, '08:00', '20:00')).toBe('14:00');
    expect(timeAtOffset(0.51, '08:00', '20:00')).toBe('14:00');
    expect(timeAtOffset(0.53, '08:00', '20:00')).toBe('14:15');
    expect(timeAtOffset(-1, '08:00', '20:00')).toBe('08:00');
  });
});
