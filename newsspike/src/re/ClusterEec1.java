package re;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import util.FreebaseEntitySearchResult;
import util.FreebaseSearch;

import com.google.gson.Gson;

import javatools.administrative.D;
import javatools.datatypes.HashCount;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;
import javatools.mydb.Sort;
import javatools.string.StringUtil;
import javatools.ui.HtmlVisual;
import javatools.ui.TxtTable;

class RC2 {
	String r0;
	int size = 0;
	List<String[]> rows = new ArrayList<String[]>();

	public RC2(String r0, int size) {
		this.r0 = r0;
		this.size = size;

	}
}

class PairEec {
	String shareStr;
	Eec eec1;
	Eec eec2;

}

public class ClusterEec1 {

	public static void relstr2eecs(String input_eec, String output_relstr2eec,
			String tmpDir) throws IOException {
		Gson gson = new Gson();
		String[] l;
		DR dr = new DR(input_eec);
		String tempoutput = output_relstr2eec + ".tmp";
		DW dw = new DW(tempoutput);
		int count = 0;
		int numeec = 0;
		while ((l = dr.read()) != null) {
			numeec++;
			String eecstr = l[1];
			Eec eec = gson.fromJson(eecstr, Eec.class);
			Set<String> setRelStr = new HashSet<String>();
			for (Tuple t : eec.getTuples()) {
				String relstr = t.getRel();
				setRelStr.add(relstr);
			}
			for (String r : setRelStr) {
				dw.write(r, eecstr);
				count++;
			}
		}
		D.p("Total str2eec edges", count);
		D.p("Total number of eec", numeec);
		dw.close();
		Sort.sort(tempoutput, output_relstr2eec, tmpDir,
				new Comparator<String[]>() {
					@Override
					public int compare(String[] arg0, String[] arg1) {
						// TODO Auto-generated method stub
						return arg0[0].compareTo(arg1[0]);
					}
				});
	}

	static Gson gson = new Gson();

	public static void relstr2eecs2rcConsiderSize(
			String input_relstr2eecs_sbrelstr,
			String output_re) {
		DR dr = new DR(input_relstr2eecs_sbrelstr);
		DW dw = new DW(output_re);
		List<String[]> b;
		int MAXSIZE = 100;
		int ONLYCONSIDER = 5;
		int[] rcsize = new int[MAXSIZE + 1];
		while ((b = dr.readBlock(0)) != null) {
			Set<String> argpair = new HashSet<String>();
			if (b.size() == 1)
				continue;
			for (String[] l : b) {
				String eecstr = l[1];
				Eec eec = gson.fromJson(eecstr, Eec.class);
				argpair.add(eec.getArg1() + "\t" + eec.getArg2());
			}

			if (argpair.size() > 5) {
				for (String[] l : b)
					dw.write(l);
			}

			if (argpair.size() > MAXSIZE) {
				rcsize[MAXSIZE]++;
			} else {
				rcsize[argpair.size()]++;
			}

		}
		D.p("RC size histogram:");
		for (int i = 0; i < rcsize.length; i++) {
			if (rcsize[i] != 0)
				D.p(i, rcsize[i]);
		}
		dr.close();
		dw.close();
	}

	public static void relstr2eecs2rcConsiderArgumentType(
			String input_relstr2eecs_sbrelstr,
			String output_re) {
		DR dr = new DR(input_relstr2eecs_sbrelstr);
		DW dw = new DW(output_re);
		List<String[]> b;
		int MAXSIZE = 100;
		int ONLYCONSIDER = 5;
		int[] rcsize = new int[MAXSIZE + 1];
		while ((b = dr.readBlock(0)) != null) {
			Set<String> argpair = new HashSet<String>();
			List<String[]> tow = new ArrayList<String[]>();
			if (b.size() == 1)
				continue;
			for (String[] l : b) {
				String eecstr = l[1];
				Eec eec = gson.fromJson(eecstr, Eec.class);
				boolean arg1IsNameEntity = false;
				boolean arg2IsNameEntity = false;
				for (Tuple t : eec.getTuples()) {
					String arg1ner = t.getArg1Ner();
					String arg2ner = t.getArg2Ner();
					if (arg1ner.equals("PERSON")
							|| arg1ner.equals("LOCATION")
							|| arg1ner.equals("ORGANIZATION")
							|| arg1ner.equals("MISC")) {
						arg1IsNameEntity = true;
					}
					if (!arg2ner.equals("O")) {
						arg2IsNameEntity = true;
					}
				}
				if (arg1IsNameEntity && arg2IsNameEntity) {
					tow.add(l);
					argpair.add(eec.getArg1() + "\t" + eec.getArg2());
				}
			}
			if (argpair.size() > 1) {
				if (argpair.size() < MAXSIZE)
					rcsize[argpair.size()]++;
				else
					rcsize[MAXSIZE]++;
				for (String[] l : tow)
					dw.write(l);
			}

		}
		D.p("RC size histogram:");
		for (int i = 0; i < rcsize.length; i++) {
			if (rcsize[i] != 0)
				D.p(i, rcsize[i]);
		}
		dr.close();
		dw.close();
	}

	public static void rcProcess1(String input_rc,
			String output) throws IOException {
		DR dr = new DR(input_rc);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(output), "utf-8"));
		List<String[]> b;
		int ONLYCONSIDER = 5;
		while ((b = dr.readBlock(0)) != null) {
			String r0 = b.get(0)[0];
			List<String[]> table = new ArrayList<String[]>();
			if (b.size() < ONLYCONSIDER)
				continue;
			for (String[] l : b) {
				String r1 = l[0];
				String eecstr = l[1];
				Eec eec = gson.fromJson(eecstr, Eec.class);
				Set<String> setRelStr = new HashSet<String>();
				for (Tuple t : eec.getTuples()) {
					String relstr = t.getRel();
					if (!setRelStr.contains(relstr)) {
						table.add(new String[] { relstr, eec.getArg1(),
								eec.getArg2(), t.getArg1Ner(), t.getArg2Ner(),
								StringUtil.join(t.tkn, " ") });
						setRelStr.add(relstr);
					}
				}
			}
			bw.write(r0 + "\n");
			bw.write(TxtTable.getTextTableDisplay(6,
					new int[] { 15, 15, 15, 10, 10, 40 }, table));
			bw.write("\n\n");
		}
		dr.close();
		bw.close();
	}

	public static void relstr2groupeecsStep1(String input_rc,
			String output,
			int MIN_NUM_OF_EECS) throws IOException {
		DR dr = new DR(input_rc);
		HashSet<String> arguments = new HashSet<String>();
		List<String[]> b;
		List<RC2> rc2list = new ArrayList<RC2>();
		while ((b = dr.readBlock(0)) != null) {
			String r0 = b.get(0)[0];
			List<String[]> labelAB = new ArrayList<String[]>();
			List<String[]> tableAB = new ArrayList<String[]>();
			Set<String> appear = new HashSet<String>();
			//			List<String[]> table = new ArrayList<String[]>();
			if (b.size() < MIN_NUM_OF_EECS)
				continue;
			RC2 rc2 = new RC2(r0, b.size());
			rc2list.add(rc2);
			//			dw.write(DW.tow("RELATION", r0.toUpperCase(), b.size()));
			for (String[] l : b) {
				String r1 = l[0];
				String eecstr = l[1];
				Eec eec = gson.fromJson(eecstr, Eec.class);
				//write down instances of the pivot relation

				for (Tuple t : eec.getTuples()) {
					String relstr = t.getRel();
					String a1 = eec.getArg1();
					String a2 = eec.getArg2();
					String key = a1 + "\t" + relstr + "\t" + a2;
					if (t.getArg1().equals(a1) && t.getArg2().equals(a2)) { //some bugs when generating eec
						if (relstr.equals(r1) && !appear.contains(key)) {
							//							labelAB.add(new String[] { "TOLABEL", "0", a1, a2,
							//									"??" });
							String s = StringUtil.join(t.tkn, " ");
							if (s.equals("Microsoft Accuses Google Of Blocking YouTube Windows Phone App .")) {
								D.p(s);
							}
							String[] tow = new String[] { "0",
									eec.getArg1(),
									relstr,
									eec.getArg2(),
									t.getNerStrOfArg1(),
									t.getNerStrOfArg2(),
									t.getArg1Ner(),
									t.getArg2Ner(),
									eec.getAllRelations(),
									StringUtil.join(t.tkn, " ") };
							rc2.rows.add(tow);
							arguments.add(t.getNerStrOfArg1());
							arguments.add(t.getNerStrOfArg2());
							//							tableAB.add(tow);

							//							dw.write(tow);
							appear.add(key);
						}
					}
				}
			}
			//			for (String[] l : b) {
			//				String r1 = l[0];
			//				String eecstr = l[1];
			//				Eec eec = gson.fromJson(eecstr, Eec.class);
			//				//write down instances of the pivot relation
			//				//write down other sentences
			//				for (Tuple t : eec.getTuples()) {
			//					String relstr = t.getRel();
			//					String a1 = eec.getArg1();
			//					String a2 = eec.getArg2();
			//					String key = a1 + "\t" + relstr + "\t" + a2;
			//					if (t.getArg1().equals(a1) && t.getArg2().equals(a2)) { //some bugs when generating eec
			//						if (!relstr.equals(r1) && !appear.contains(key)) {
			//							labelAB.add(new String[] { "TOLABEL", "0", a1, a2,
			//									"??" });
			//							String[] tow = new String[] { "0", eec.getArg1(),
			//									relstr,
			//									eec.getArg2(),
			//									t.getNerStrOfArg1(),
			//									t.getNerStrOfArg2(),
			//									t.getArg1Ner(),
			//									t.getArg2Ner(),
			//									StringUtil.join(t.tkn, " ") };
			//							rc2.rows.add(tow);
			//							arguments.add(t.getNerStrOfArg2());
			//							arguments.add(t.getNerStrOfArg2());
			//							appear.add(key);
			//						}
			//					}
			//				}
			//			}
		}
		dr.close();

		DW dw = new DW(output + ".woFBTypes");
		for (RC2 rc2 : rc2list) {
			dw.write(gson.toJson(rc2));
		}
		dw.close();
		DW dw2 = new DW(output + ".arguments");
		for (String s : arguments) {
			dw2.write(s);
		}
		dw2.close();
	}

	public static void relstr2groupeecsStep2(String input_rc2woFBTypes,
			String input_fbsearch,
			String output) {
		HashMap<String, FreebaseEntitySearchResult> str2fs = new HashMap<String, FreebaseEntitySearchResult>();
		{
			DR dr = new DR(input_fbsearch);
			String[] l;
			while ((l = dr.read()) != null) {
				String argstr = l[0];
				FreebaseEntitySearchResult fs = gson.fromJson(l[1],
						FreebaseEntitySearchResult.class);
				str2fs.put(argstr, fs);
			}
			dr.close();
		}
		{
			DW dw = new DW(output);
			DR dr = new DR(input_rc2woFBTypes);
			String[] l;
			while ((l = dr.read()) != null) {
				RC2 rc2 = gson.fromJson(l[0], RC2.class);
				dw.write(DW.tow("RELATION", rc2.r0.toUpperCase(), rc2.size));
				Collections.sort(rc2.rows, new Comparator<String[]>() {
					@Override
					public int compare(String[] x, String[] y) {
						return (x[6] + " " + x[7]).compareTo(y[6] + " " + y[7]);
					}
				});
				for (String[] a : rc2.rows) {
					String arg1ner = a[6];
					String arg2ner = a[7];
					if (a[4] == null || a[5] == null) {
						D.p(a);
					}
					a[4] = getMostLikelyNotableType(a[4], arg1ner, str2fs);
					a[5] = getMostLikelyNotableType(a[5], arg2ner, str2fs);
					dw.write(a);
				}
				dw.write("");
			}
			dw.close();
		}
	}

	public static void relstr2groupeecsPairwise(String input_rc,
			String output,
			int MIN_NUM_OF_EECS) {

		int NUM1 = 2;
		int NUM2 = 5;
		int NUM3 = 10;
		DR dr = new DR(input_rc);
		HashSet<String> arguments = new HashSet<String>();
		List<String[]> b;
		List<PairEec> pairs = new ArrayList<PairEec>();

		int numDiffKeyRelation = 0;
		Set<String> diffKeyRelations = new HashSet<String>();
		while ((b = dr.readBlock(0)) != null) {

			String r0 = b.get(0)[0];

			Set<String> appear = new HashSet<String>();
			if (b.size() < MIN_NUM_OF_EECS)
				continue;
			List<Eec> listeec = new ArrayList<Eec>();
			for (String[] l : b) {
				String r1 = l[0];
				String eecstr = l[1];
				Eec eec = gson.fromJson(eecstr, Eec.class);
				listeec.add(eec);
			}
			if (listeec.size() > NUM3) {
				numDiffKeyRelation++;
				diffKeyRelations.add(r0);
			}
			List<PairEec> temppairs = new ArrayList<PairEec>();
			for (int i = 0; i < listeec.size(); i++) {
				for (int j = i + 1; j < listeec.size(); j++) {
					Eec eec1 = listeec.get(i);
					Eec eec2 = listeec.get(j);
					PairEec p = new PairEec();
					temppairs.add(p);
					p.eec1 = eec1;
					p.eec2 = eec2;
					p.shareStr = r0;
				}
			}
			Collections.shuffle(temppairs, new Random(1));
			for (int i = 0; i < NUM3; i++) {
				pairs.add(temppairs.get(i));
			}

		}
		dr.close();
		D.p("numDiffKeyRelation", diffKeyRelations.size());
		D.p("pairs.size()", pairs.size());
		for (String keyrel : diffKeyRelations) {
			D.p(keyrel);
		}
		DW dwhtml = new DW(output + ".label.html");
		DW dwtxt = new DW(output + ".label.txt");
		int pairId = 0;
		for (PairEec p : pairs) {
			displayPair(p, pairId, dwhtml, dwtxt);
			pairId++;
		}
		dwhtml.close();
		dwtxt.close();

		//		Collections.shuffle(pairs);
		//		HashCount<String> appearRel = new HashCount<String>();
		//		List<String[]> tow = new ArrayList<String[]>();
		//		for (int k = 0; k < pairs.size(); k++) {
		//			PairEec p = pairs.get(k);
		//			if (appearRel.see(p.shareStr) < 3) {
		//				List<String[]> printeec1 = printEec(p.eec1);
		//				List<String[]> printeec2 = printEec(p.eec2);
		//				tow.add(DW.tow(p.shareStr,
		//						p.eec1.getArg1(),
		//						p.eec1.getArg2(),
		//						p.eec2.getArg1(),
		//						p.eec2.getArg2()
		//						));
		//
		//				for (String[] a : printeec1) {
		//					tow.add(a);
		//				}
		//				for (String[] a : printeec2) {
		//					tow.add(a);
		//				}
		//				appearRel.add(p.shareStr);
		//			}
		//		}
		//		DW dw = new DW(output);
		//		for (String[] w : tow) {
		//			dw.write(w);
		//		}
		//		dw.close();
	}

	public static void displayPair(PairEec p, int pairId, DW dwhtml, DW dwtxt) {

		//		dwtxt.write("1", "PID:" + pairId, p.shareStr, p.eec1.getArg1(),
		//				p.eec1.getArg2(),
		//				p.eec2.getArg1(), p.eec2.getArg2());
		List<String[]> ret = new ArrayList<String[]>();
		StringBuilder sb = new StringBuilder();
		ret.add(new String[] { "1", "PID:" + pairId, p.shareStr,
				p.eec1.getArg1(),
				p.eec1.getArg2(), p.eec2.getArg1(), p.eec2.getArg2() });
		displayEec(p.eec1, ret);
		displayEec(p.eec2, ret);
		HtmlVisual
				.json2htmlStrTable("PID:" + pairId + "::" + p.shareStr, ret, sb);
		dwhtml.write(sb.toString());
		dwtxt.write(ret.get(0));
		//		for (String[] a : ret) {
		//			dwtxt.write(a);
		//		}

	}

	public static void displayEec(Eec eec, List<String[]> ret) {
		Set<String> appear = new HashSet<String>();
		for (Tuple t : eec.tuples) {
			String r = t.getRel();
			String s = StringUtil.join(t.tkn, " ");
			if (!appear.contains(r)) {
				ret.add(DW.tow("", t.getArg1(), r, t.getArg2(), s));
				appear.add(r);
			}
		}
	}

	public static List<String[]> printEec(Eec eec) {
		List<String[]> ret = new ArrayList<String[]>();
		Set<String> appear = new HashSet<String>();
		for (Tuple t : eec.tuples) {
			String r = t.getRel();
			String s = StringUtil.join(t.tkn, " ");
			if (!appear.contains(r)) {
				ret.add(DW.tow(r, s));
				appear.add(r);
			}
		}
		return ret;
	}

	public static String getMostLikelyNotableType(String arg,
			String argner,
			HashMap<String, FreebaseEntitySearchResult> str2fs) {
		if (argner.equals("PERSON") ||
				argner.equals("ORGANIZATION") ||
				argner.equals("LOCATION") ||
				argner.equals("O") ||
				argner.equals("MISC")) {
			FreebaseEntitySearchResult fs1 = str2fs.get(arg);
			if (arg.equals("Tiffany")) {
				D.p(arg);
			}

			if (fs1 != null) {
				return fs1.getMostLikelyNotableType();
			}
			return argner;
		} else {
			return argner;
		}
	}

	public static void rcStatistic(String input_re) {
		DR dr = new DR(input_re);
		List<String[]> b;
		while ((b = dr.readBlock(0)) != null) {
			for (String[] l : b) {
				//number of eecs

			}
		}
		dr.close();

	}

	public static void getNel(String input, String output) {
		String dir = "/projects/pardosa/data17/clzhang/re/";
		FreebaseSearch fs = new FreebaseSearch(dir + "/fbsearch");
		fs.searchListEntities(input, output);
	}

	public static void main(String[] args) throws IOException {
		if (args[0].equals("-relstr2eecs")) {
			relstr2eecs(args[1], args[2], args[3]);
		}
		if (args[0].equals("-relstr2eecs2rcConsiderSize")) {
			relstr2eecs2rcConsiderSize(args[1], args[2]);
		}
		if (args[0].equals("-relstr2eecs2rcConsiderArgumentType")) {
			relstr2eecs2rcConsiderArgumentType(args[1], args[2]);
		}
		//		if (args[0].equals("-rcProcess1")) {
		//			rcProcess1(args[1], args[2]);
		//		}
		if (args[0].equals("-relstr2groupeecsStep1")) {
			relstr2groupeecsStep1(args[1], args[2], 10);
		}
		if (args[0].equals("-getNel")) {
			getNel(args[1], args[2]);
		}
		if (args[0].equals("-relstr2groupeecsStep2")) {
			relstr2groupeecsStep2(args[1], args[2], args[3]);
		}
		if (args[0].equals("-relstr2groupeecsPairwise")) {
			/**obsolete*/
			relstr2groupeecsPairwise(args[1], args[2], 10);
		}

	}
}
