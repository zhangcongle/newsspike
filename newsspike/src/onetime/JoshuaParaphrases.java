package onetime;

import javatools.administrative.D;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;

public class JoshuaParaphrases {

	public static void run1() {
		String input = "O:/data17/clzhang/re/joshua/joshua_labeled_paraphrases.old";
		String output = "O:/data17/clzhang/re/joshua/joshua_labeled_paraphrases";
		DR dr = new DR(input);
		DW dw = new DW(output);
		String[] l;
		int numYes = 0;
		while ((l = dr.read()) != null) {
			if (!(l[0].equals("y") || l[0].equals("n"))) {
				// D.p(l[0], l[2], l[3]);
			}
			if (l[1].length() > 0) {
				// D.p(l[0], l[1], l[2], l[3]);
			}
			if (l[0].equals("y")) {
				dw.write("1", l[2], l[3]);
				numYes++;
			}
			if (l[0].equals("n")) {
				dw.write("0", l[2], l[3]);
			}
		}
		D.p("numYes", numYes);
		dr.close();
		dw.close();
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		run1();
	}

}
