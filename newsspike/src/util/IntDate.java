package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IntDate {
	Date date;

	public IntDate(int d) throws ParseException {
		date = new SimpleDateFormat("yyyyMMdd").parse(d + "");
	}

	public int diff(Date d2) {
		long diffInLong = date.getTime() - d2.getTime();
		int days = (int) (diffInLong / 1000 / 3600 / 24);
		return days;
	}

	public static int diff(int d1, int d2) {
		try {
			Date date1 = new SimpleDateFormat("yyyyMMdd").parse(d1 + "");
			Date date2 = new SimpleDateFormat("yyyyMMdd").parse(d2 + "");
			long diffInLong = date1.getTime() - date2.getTime();
			return (int) (diffInLong / 1000 / 3600 / 24);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static void main(String[] args) throws ParseException {
		System.out.println(diff(20121209, 20121213));
		//		System.out.println(id.calendar.getTime());
	}

}
