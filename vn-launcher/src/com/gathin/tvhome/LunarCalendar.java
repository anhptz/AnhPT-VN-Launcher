package vn.cleanyourtv.dashboard;

import java.util.Calendar;

/**
 * Vietnamese Lunar Calendar (Am lich Viet Nam - UTC+7) based on Ho Ngoc Duc astronomical algorithm.
 */
public final class LunarCalendar {

    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};
    private static final String[] WEEKDAYS = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    private static final String[] SHORT_WEEKDAYS = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    public static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        }
        return jd;
    }

    private static int getNewMoonDay(int k, double timeZone) {
        double T = k / 1236.85;
        double T2 = T * T;
        double T3 = T2 * T;
        double dr = Math.PI / 180.0;
        double Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
        Jd1 = Jd1 + 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
        double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
        double Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
        double F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
        double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
        C1 = C1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
        C1 = C1 - 0.0004 * Math.sin(dr * 3 * Mpr);
        C1 = C1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mpr));
        C1 = C1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
        C1 = C1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
        C1 = C1 + 0.0010 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
        double deltat;
        if (T < -11) {
            deltat = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3;
        } else {
            deltat = -0.000278 + 0.000265 * T + 0.000262 * T2;
        }
        double JdNew = Jd1 + C1 - deltat;
        return (int) Math.floor(JdNew + 0.5 + timeZone / 24.0);
    }

    private static int getSunLongitude(int jdn, double timeZone) {
        double T = (jdn - 2451545.5 - timeZone / 24.0) / 36525.0;
        double T2 = T * T;
        double dr = Math.PI / 180.0;
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
        double DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
        DL = DL + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.000290 * Math.sin(dr * 3 * M);
        double L = L0 + DL;
        L = L * dr;
        L = L - Math.PI * 2 * (Math.floor(L / (Math.PI * 2)));
        return (int) Math.floor(L / Math.PI * 6.0);
    }

    private static int getLunarMonth11(int yy, double timeZone) {
        int off = jdFromDate(31, 12, yy) - 2415021;
        int k = (int) Math.floor(off / 29.530588853);
        int nm = getNewMoonDay(k, timeZone);
        int sunLong = getSunLongitude(nm, timeZone);
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone);
        }
        return nm;
    }

    private static int getLeapMonthOffset(int a11, double timeZone) {
        int k = (int) Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int last;
        int i = 1;
        int arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone);
        do {
            last = arc;
            i++;
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone);
        } while (arc != last && i < 14);
        return i - 1;
    }

    /**
     * Returns [lunarDay, lunarMonth, lunarYear, lunarLeap]
     */
    public static int[] solarToLunar(int dd, int mm, int yy) {
        double timeZone = 7.0;
        int dayNumber = jdFromDate(dd, mm, yy);
        int k = (int) Math.floor((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = getNewMoonDay(k + 1, timeZone);
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone);
        }
        int a11 = getLunarMonth11(yy, timeZone);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = getLunarMonth11(yy - 1, timeZone);
        } else {
            lunarYear = yy + 1;
            b11 = getLunarMonth11(yy + 1, timeZone);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = (int) Math.floor((monthStart - a11) / 29.0);
        int lunarLeap = 0;
        int lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapMonthDiff = getLeapMonthOffset(a11, timeZone);
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10;
                if (diff == leapMonthDiff) {
                    lunarLeap = 1;
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth = lunarMonth - 12;
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1;
        }
        return new int[]{lunarDay, lunarMonth, lunarYear, lunarLeap};
    }

    public static String getYearCanChi(int lunarYear) {
        String can = CAN[(lunarYear + 6) % 10];
        String chi = CHI[(lunarYear + 8) % 12];
        return can + " " + chi;
    }

    public static String getWeekdayName(Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday .. 7 = Saturday
        return WEEKDAYS[dow - 1];
    }

    public static String getShortWeekday(Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        return SHORT_WEEKDAYS[dow - 1];
    }

    public static final class UpcomingEvent {
        public final String title;
        public final String dateText;
        public final String badgeText;
        public final boolean highlight;

        public UpcomingEvent(String title, String dateText, String badgeText, boolean highlight) {
            this.title = title;
            this.dateText = dateText;
            this.badgeText = badgeText;
            this.highlight = highlight;
        }
    }

    public static String formatCountdownBadge(int days) {
        if (days == 0) return "Hôm nay";
        if (days == 1) return "Ngày mai";
        return "còn " + days + " ngày";
    }

    /**
     * Finds the next Rằm (day 15) or Mùng 1 (day 1) from today.
     */
    public static UpcomingEvent findNextLunarMilestone(Calendar today) {
        Calendar c = (Calendar) today.clone();
        for (int i = 0; i <= 32; i++) {
            int d = c.get(Calendar.DAY_OF_MONTH);
            int m = c.get(Calendar.MONTH) + 1;
            int y = c.get(Calendar.YEAR);
            int[] lunar = solarToLunar(d, m, y);
            if (lunar[0] == 15 || lunar[0] == 1) {
                String title = (lunar[0] == 15 ? "Rằm tháng " : "Mùng 1 tháng ") + lunar[1];
                String dateStr = getWeekdayName(c) + ", " + d + "/" + m;
                String badge = formatCountdownBadge(i);
                return new UpcomingEvent(title, dateStr, badge, i <= 2);
            }
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return new UpcomingEvent("Rằm tháng 9", "Thứ Bảy, 24/10", "Hôm nay", true);
    }

    /**
     * Finds the next major Vietnamese holiday (e.g. 20/11, 22/12, 1/1, 30/4, 2/9, 20/10).
     */
    public static UpcomingEvent findNextSolarHoliday(Calendar today) {
        int[][] holidays = {
            {20, 11}, {22, 12}, {1, 1}, {14, 2}, {8, 3}, {30, 4}, {1, 5}, {1, 6}, {2, 9}, {20, 10}
        };
        String[] names = {
            "Ngày Nhà giáo VN", "Ngày QĐND Việt Nam", "Tết Dương lịch", "Lễ Tình nhân",
            "Quốc tế Phụ nữ", "Giải phóng miền Nam", "Quốc tế Lao động", "Quốc tế Thiếu nhi",
            "Quốc khánh 2/9", "Phụ nữ Việt Nam"
        };

        int bestDays = 9999;
        String bestTitle = "Ngày Nhà giáo VN";
        String bestDate = "Thứ Sáu, 20/11";

        int todayJd = jdFromDate(today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.MONTH) + 1, today.get(Calendar.YEAR));
        int curYear = today.get(Calendar.YEAR);

        for (int i = 0; i < holidays.length; i++) {
            int hd = holidays[i][0];
            int hm = holidays[i][1];
            int hy = curYear;
            int hJd = jdFromDate(hd, hm, hy);
            if (hJd < todayJd) {
                hy = curYear + 1;
                hJd = jdFromDate(hd, hm, hy);
            }
            int diff = hJd - todayJd;
            if (diff >= 0 && diff < bestDays) {
                bestDays = diff;
                bestTitle = names[i];
                Calendar hc = Calendar.getInstance();
                hc.set(hy, hm - 1, hd);
                bestDate = getWeekdayName(hc) + ", " + hd + "/" + hm;
            }
        }
        return new UpcomingEvent(bestTitle, bestDate, formatCountdownBadge(bestDays), bestDays <= 1);
    }

    /**
     * Finds the next Lunar New Year (Mùng 1 tháng 1 âm lịch).
     */
    public static UpcomingEvent findNextTet(Calendar today) {
        Calendar c = (Calendar) today.clone();
        for (int i = 0; i <= 400; i++) {
            int d = c.get(Calendar.DAY_OF_MONTH);
            int m = c.get(Calendar.MONTH) + 1;
            int y = c.get(Calendar.YEAR);
            int[] lunar = solarToLunar(d, m, y);
            if (lunar[0] == 1 && lunar[1] == 1 && lunar[3] == 0) {
                String canChi = getYearCanChi(lunar[2]);
                String title = "Tết " + canChi;
                String dateStr = getWeekdayName(c) + ", " + d + "/" + m + "/" + y;
                return new UpcomingEvent(title, dateStr, formatCountdownBadge(i), i <= 3);
            }
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return new UpcomingEvent("Tết Đinh Mùi", "Thứ Bảy, 6/2/2027", "còn 105 ngày", false);
    }
}
