package re;

import java.io.BufferedWriter;
import java.io.File;
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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import util.FreebaseEntitySearchResult;
import util.FreebaseSearch;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

//import eventrelation4.Util;
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

public class ClusterEec2 {

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

	public static void generateVerbGroup(String input_rc,
			String output,
			int MIN_NUM_OF_EECS) throws IOException {
		DR dr = new DR(input_rc);
		HashSet<String> arguments = new HashSet<String>();
		List<String[]> b;
		List<RC2> rc2list = new ArrayList<RC2>();
		DW dw = new DW(output);
		while ((b = dr.readBlock(0)) != null) {
			String r0 = b.get(0)[0];
			Set<String> appear = new HashSet<String>();
			// List<String[]> table = new ArrayList<String[]>();
			if (b.size() < MIN_NUM_OF_EECS)
				continue;
			VerbGroup vg = new VerbGroup();
			vg.pivotVerb = r0;
			for (String[] l : b) {
				String eecstr = l[1];
				Eec eec = gson.fromJson(eecstr, Eec.class);
				// for (Tuple t : eec.getTuples()) {
				// String relstr = t.getRel();
				// String a1 = eec.getArg1();
				// String a2 = eec.getArg2();
				// }
				vg.eecs.add(eec);
			}
			dw.write(gson.toJson(vg));
		}
		dw.close();
	}

	public static void letHumanAnnotateStep1(String input_verbgroup,
			String output_woFBTypes,
			String output_arguments) {
		DR dr = new DR(input_verbgroup);
		String[] l;
		List<RC2> rc2list = new ArrayList<RC2>();
		HashSet<String> arguments = new HashSet<String>();
		while ((l = dr.read()) != null) {
			VerbGroup vg = gson.fromJson(l[0], VerbGroup.class);
			Set<String> appear = new HashSet<String>();
			RC2 rc2 = new RC2(vg.pivotVerb, vg.eecs.size());
			rc2list.add(rc2);
			for (Eec eec : vg.eecs) {
				for (Tuple t : eec.getTuples()) {
					String relstr = t.getRel();
					String a1 = eec.getArg1();
					String a2 = eec.getArg2();
					String key = a1 + "\t" + relstr + "\t" + a2;

					if (relstr.equals(vg.pivotVerb)
							&& !appear.contains(key)) {
						String s = StringUtil.join(t.tkn, " ");
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
						appear.add(key);
					}
				}
			}
		}
		DW dw = new DW(output_woFBTypes);
		for (RC2 rc2 : rc2list) {
			dw.write(gson.toJson(rc2));
		}
		dw.close();
		DW dw2 = new DW(output_arguments);
		for (String s : arguments) {
			dw2.write(s);
		}
		dw2.close();
	}

	public static void relstr2groupeecsStep1(String input_rc,
			String output,
			int MIN_NUM_OF_EECS) throws IOException {
		DR dr = new DR(input_rc);
		// BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
		// new FileOutputStream(output), "utf-8"));

		HashSet<String> arguments = new HashSet<String>();
		// DW dwner = new DW(output + ".arguments");
		List<String[]> b;

		List<RC2> rc2list = new ArrayList<RC2>();
		while ((b = dr.readBlock(0)) != null) {
			String r0 = b.get(0)[0];
			List<String[]> labelAB = new ArrayList<String[]>();
			List<String[]> tableAB = new ArrayList<String[]>();
			Set<String> appear = new HashSet<String>();
			// List<String[]> table = new ArrayList<String[]>();
			if (b.size() < MIN_NUM_OF_EECS)
				continue;
			RC2 rc2 = new RC2(r0, b.size());
			rc2list.add(rc2);
			// dw.write(DW.tow("RELATION", r0.toUpperCase(), b.size()));
			for (String[] l : b) {
				String r1 = l[0];
				String eecstr = l[1];
				Eec eec = gson.fromJson(eecstr, Eec.class);
				// write down instances of the pivot relation

				for (Tuple t : eec.getTuples()) {
					String relstr = t.getRel();
					String a1 = eec.getArg1();
					String a2 = eec.getArg2();
					String key = a1 + "\t" + relstr + "\t" + a2;
					if (t.getArg1().equals(a1) && t.getArg2().equals(a2)) { // some
																			// bugs
																			// when
																			// generating
																			// eec
						if (relstr.equals(r1) && !appear.contains(key)) {
							// labelAB.add(new String[] { "TOLABEL", "0", a1,
							// a2,
							// "??" });
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
							// tableAB.add(tow);

							// dw.write(tow);
							appear.add(key);
						}
					}
				}
			}
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

	public static void letHumanAnnotateStep2(String input_rc2woFBTypes,
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
				// number of eecs

			}
		}
		dr.close();

	}

	public static void getNel(String input, String output) {
		String dir = "/projects/pardosa/data17/clzhang/re/";
		FreebaseSearch fs = new FreebaseSearch(dir + "/fbsearch");
		fs.searchListEntities(input, output);
	}

	public static void clusterVerbGroupCheating(String input_verbgroup,
			String input_verbgroup_congle,
			String output) {
		DW dw = new DW(output);
		// load labeled verbgroup
		HashMap<String, String> pvArg1Arg2_clustername = new HashMap<String, String>();
		{
			DR dr = new DR(input_verbgroup_congle);
			String[] l;
			List<String[]> buff = new ArrayList<String[]>();
			String pivotverb = "";
			while ((l = dr.read()) != null) {
				if (l[0].equals("RELATION")) {
					for (String[] b : buff) {
						String key = b[1] + "\t" + b[2] + "\t" + b[3];
						String label = b[0];
						if (b[0].equals("") || b[0].equals("0")) {
							label = "NA";
						}
						pvArg1Arg2_clustername.put(key, label);
					}
					buff = new ArrayList<String[]>();
				} else {
					buff.add(l);
				}
			}
			for (String[] b : buff) {
				if (b.length < 3)
					continue;
				String key = b[1] + "\t" + b[2] + "\t" + b[3];
				String label = b[0];
				if (b[0].equals("") || b[0].equals("0")) {
					label = "NA";
				}
				pvArg1Arg2_clustername.put(key, label);
			}
		}
		{
			DR dr = new DR(input_verbgroup);
			DW dwfix = new DW(input_verbgroup + ".fixbug");
			String[] l;
			while ((l = dr.read()) != null) {

				VerbGroup vg = gson.fromJson(l[0], VerbGroup.class);
				VerbGroupCluster vgc = new VerbGroupCluster();
				VerbGroup vg2 = new VerbGroup();
				vg2.pivotVerb = vg.pivotVerb;
				vgc.pivotVerb = vg.pivotVerb;
				if (vg.pivotVerb.equals("close above")) {
					D.p(vg.pivotVerb);
				}
				for (int i = 0; i < vg.eecs.size(); i++) {
					Eec eec = vg.eecs.get(i);
					String key = eec.getArg1() + "\t" + vg.pivotVerb + "\t"
							+ eec.getArg2();
					String label = pvArg1Arg2_clustername.get(key);
					if (label != null) {// missing some labels here
						vgc.eecs.add(eec);
						vgc.clustering.add(label);

						vg2.eecs.add(eec);
						// dw.write(label, eec.getArg1(), vg.pivotVerb,
						// eec.getArg2());
					} else {
						System.out.println("missing the labels\t" + key);
					}
				}
				dw.write(gson.toJson(vgc));
				dwfix.write(gson.toJson(vg2));
				// dw.write(" ");
			}
			dr.close();
			dwfix.close();
		}
		dw.close();
	}

	public static void letHumanAnnotateMissing(String input_verbgroup, String input_verbgroup_congle,
			String dir_paraphrases, String input_fbsearch, String output) {
		HashMap<String, Double> paraphrases = new HashMap<String, Double>();
		HashMap<String, String> pvArg1Arg2_clustername = new HashMap<String, String>();
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
			File dir = new File(dir_paraphrases);
			String[] list = dir.list();
			for (String f : list) {
				DR dr = new DR(dir + "/" + f);
				String[] l;
				while ((l = dr.read()) != null) {
					if (l.length < 3)
						continue;
					double score = Double.parseDouble(l[0]);
					String pair = Util.pairAB(l[1], l[2]);
					if (paraphrases.containsKey(pair)) {
						// when there is confusion, tag the pair as not
						// paraphrase
						double oldscore = paraphrases.get(pair);
						score = Math.min(oldscore, score);
					}
					paraphrases.put(pair, score);
				}
				dr.close();
			}
		}
		{
			DR dr = new DR(input_verbgroup_congle);
			String[] l;
			List<String[]> buff = new ArrayList<String[]>();
			String pivotverb = "";
			while ((l = dr.read()) != null) {
				if (l[0].equals("RELATION")) {
					for (String[] b : buff) {
						String key = b[1] + "\t" + b[2] + "\t" + b[3];
						String label = b[0];
						if (b[0].equals("") || b[0].equals("0")) {
							label = "NA";
						}
						pvArg1Arg2_clustername.put(key, label);
					}
					buff = new ArrayList<String[]>();
				} else {
					buff.add(l);
				}
			}
			for (String[] b : buff) {
				if (b.length < 3)
					continue;
				String key = b[1] + "\t" + b[2] + "\t" + b[3];
				String label = b[0];
				if (b[0].equals("") || b[0].equals("0")) {
					label = "NA";
				}
				pvArg1Arg2_clustername.put(key, label);
			}
		}
		// load labeled verbgroup
		{
			DR dr = new DR(input_verbgroup);
			DW dw = new DW(output);
			String[] l;
			while ((l = dr.read()) != null) {
				VerbGroup vg = gson.fromJson(l[0], VerbGroup.class);
				String pivotVerb = vg.pivotVerb;
				for (int i = 0; i < vg.eecs.size(); i++) {
					Eec eec = vg.eecs.get(i);
					String key = eec.getArg1() + "\t" + vg.pivotVerb + "\t"
							+ eec.getArg2();
					String label = pvArg1Arg2_clustername.get(key);
					if (label == null) {// missing some labels here
						label = "???";
					}
					String pivotsentence = "";
					for (Tuple t : eec.tuples) {
						String relstr = t.getRel();
						if (relstr.equals(pivotVerb)) {
							pivotsentence = StringUtil.join(t.tkn, " ");
							break;
						}
					}
					Tuple t = eec.tuples.get(0);
					String nerstrofarg1 = t.getNerStrOfArg1();
					String nerstrofarg2 = t.getNerStrOfArg2();
					String nerarg1 = t.getArg1Ner();
					String nerarg2 = t.getArg2Ner();

					String nefarg1 = getMostLikelyNotableType(nerstrofarg1, nerarg1, str2fs);
					String nefarg2 = getMostLikelyNotableType(nerstrofarg2, nerarg2, str2fs);

					dw.write("eeccluster", label, eec.getArg1(), vg.pivotVerb, eec.getArg2(), nerarg1, nerarg2,
							nefarg1, nefarg2, pivotsentence);
				}
				Set<String> appeared = new HashSet<String>();
				appeared.add(pivotVerb);
				for (int i = 0; i < vg.eecs.size(); i++) {
					Eec eec = vg.eecs.get(i);
					String pivotsentence = "";
					String pivotHead = "";
					for (Tuple t : eec.tuples) {
						String relstr = t.getRel();
						if (relstr.equals(pivotVerb)) {
							pivotsentence = StringUtil.join(t.tkn, " ");
							pivotHead = t.getRelHead();
							break;
						}
					}
					for (Tuple t : eec.tuples) {
						String relstr = t.getRel();
						String relhead = t.getRelHead();
						if (!appeared.contains(relstr)) {
							String s = StringUtil.join(t.tkn, " ");
							String pair = Util.pairAB(relstr, pivotVerb);
							String label = "";
							if (paraphrases.containsKey(pair)) {
								label = paraphrases.get(pair) + "";
							} else {
								label = "???";
							}

							appeared.add(relstr);
							dw.write("paraphrase", label, relstr, pivotVerb, relhead.equals(pivotHead), s,
									pivotsentence);
						}
					}

				}
			}
			dr.close();
			dw.close();
		}
	}

	public static void clusterVerbGroupNaiveModel(String input_verbgroup,
			String output) {
		{
			DR dr = new DR(input_verbgroup);
			DW dw = new DW(output);
			String[] l;
			while ((l = dr.read()) != null) {
				VerbGroupCluster vgc = VerbGroupCluster.fromJsonVerbGroup(l[0]);
				for (int i = 0; i < vgc.eecs.size(); i++) {
					vgc.clustering.add("ALLIN");
				}
				dw.write(gson.toJson(vgc));
			}

			dw.close();
			dr.close();
		}
	}

	public static void precisionRecallCompareTwoClusterings(
			String input_verbgroup_gold,
			String input_verbgroup_predict,
			String output) {
		StringBuilder sb = new StringBuilder();
		int[] overallpr = new int[3];
		{
			DR dr = new DR(input_verbgroup_gold);
			DR dr2 = new DR(input_verbgroup_predict);
			String[] l;
			String[] l2;
			while ((l = dr.read()) != null) {
				l2 = dr2.read();
				VerbGroupCluster vgcGold = VerbGroupCluster.fromJsonVerbGroup(l[0]);
				VerbGroupCluster vgcPredict = VerbGroupCluster.fromJsonVerbGroup(l2[0]);
				D.p(vgcGold.pivotVerb);
				if (vgcGold.pivotVerb.equals("close above")) {
					D.p(vgcGold.pivotVerb);
				}
				List<String> golds = vgcGold.clustering;
				List<String> predicts = vgcPredict.clustering;
				List<String> names = new ArrayList<String>();
				for (int i = 0; i < vgcGold.eecs.size(); i++) {
					Eec eec = vgcGold.eecs.get(i);
					names.add(eec.getNameId());
				}
				EvaluateClustering ec = new EvaluateClustering(golds, predicts);
				ec.setNames(names);
				int[] pr = new int[3];
				ec.pairwiseEvaluation(vgcGold.pivotVerb, sb, false, pr);
				overallpr[0] += pr[0];
				overallpr[1] += pr[1];
				overallpr[2] += pr[2];
				// break;
			}
		}
		{
			// overall precision recall
			List<String[]> table = new ArrayList<String[]>();
			table.add(DW.tow("precision", overallpr[0] * 1.0 / (overallpr[0] + overallpr[1]), overallpr[0],
					overallpr[1]));
			table.add(DW.tow("recall", overallpr[0] * 1.0 / (overallpr[0] + overallpr[2]), overallpr[0], overallpr[2]));
			HtmlVisual.json2htmlStrTable("Overall Precision Recall", table, sb);
		}
		DW dw = new DW(output);
		dw.write(sb.toString());
		dw.close();
	}

	public static void precisionRecallCompareTwoClusteringsOld(
			String input_verbgroup_gold,
			String input_verbgroup_result) {
		Set<String> golds = new HashSet<String>();
		Set<String> predicts = new HashSet<String>();

		Set<String> labeledPivotVerbs = new HashSet<String>();
		positivePairsOfClusteringResult(input_verbgroup_gold,
				labeledPivotVerbs, golds, true);
		positivePairsOfClusteringResult(input_verbgroup_result,
				labeledPivotVerbs, predicts, false);
		// Set<String> predicts =
		// positivePairsOfClusteringResult(input_verbgroup_result);
		int truePos = 0, falsePos = 0, falseNeg = 0;
		for (String a : golds) {
			if (predicts.contains(a)) {
				truePos++;
			} else {
				falseNeg++;
			}
		}
		for (String a : predicts) {
			if (!golds.contains(a)) {
				falsePos++;
			}
		}
		double precision = truePos * 1.0 / (truePos + falsePos);
		double recall = truePos * 1.0 / (truePos + falseNeg);
		D.p("p/r", precision, recall, truePos, falsePos, falseNeg);
	}

	public static void positivePairsOfClusteringResult
			(String input_verbgroup_clustering,
					Set<String> labeledPivotVerb,
					Set<String> retPairs,
					boolean isGold) {
		{
			DR dr = new DR(input_verbgroup_clustering);
			String[] l;
			while ((l = dr.read()) != null) {
				VerbGroupCluster vgc = VerbGroupCluster.fromJsonVerbGroup(l[0]);
				if (isGold) {
					for (int i = 0; i < vgc.eecs.size(); i++) {
						if (!vgc.clustering.get(i).equals("NA")) {
							labeledPivotVerb.add(vgc.pivotVerb);
							break;
						}
					}
					for (int i = 0; i < vgc.eecs.size(); i++) {
						Eec eec1 = vgc.eecs.get(i);
						for (int j = i + 1; j < vgc.eecs.size(); j++) {
							Eec eec2 = vgc.eecs.get(j);
							if (vgc.inSameCluster(i, j)) {
								retPairs.add(vgc.pivotVerb + "\t"
										+ Util.pairAB(eec1.getId() + "",
												eec2.getId() + ""));
							}
						}
					}

				}
				if (!isGold && labeledPivotVerb.contains(vgc.pivotVerb)) {
					for (int i = 0; i < vgc.eecs.size(); i++) {
						Eec eec1 = vgc.eecs.get(i);
						for (int j = i + 1; j < vgc.eecs.size(); j++) {
							Eec eec2 = vgc.eecs.get(j);
							if (vgc.inSameCluster(i, j)) {
								retPairs.add(vgc.pivotVerb + "\t"
										+ Util.pairAB(eec1.getId() + "",
												eec2.getId() + ""));
							}
						}
					}
				}
			}
		}
	}

	public static void tolabelRelationphrasesInVerbGroup(
			String input_verbgroup,
			String dirpairwise_paraphrase,
			String output_relationphrasesInVerbGroupTolabel
			) {
		HashMap<String, Boolean> paraphraseLabeled = new HashMap<String, Boolean>();
		loadParaphraseLabeled(dirpairwise_paraphrase + "/instances_labeled",
				paraphraseLabeled);
		HashMap<String, String> paraphrasePredicted = new HashMap<String, String>();
		loadParaphrasePredicted(dirpairwise_paraphrase + "/instances_tolabel",
				paraphrasePredicted);

		DR dr = new DR(input_verbgroup);
		DW dw = new DW(output_relationphrasesInVerbGroupTolabel);
		String[] l;
		HashCount<String> hc = new HashCount();
		HashCount<String> headcount = new HashCount();
		// List<String[]> tolabel = new ArrayList<String[]>();
		while ((l = dr.read()) != null) {
			VerbGroupCluster vgc = VerbGroupCluster.fromJsonVerbGroup(l[0]);
			RelationOfVerbGroupCluster rvgc = new RelationOfVerbGroupCluster(
					vgc);
			List<String[]> temp = rvgc.getToLabel();
			Set<String> appeared = new HashSet<String>();
			int count = 0;
			int diffcount = 0;

			for (String[] a : temp) {
				String k = Util.pairAB(a[0], a[1]);
				int yesOrNo = 0;
				String type = "";
				double confidence = 0;
				if (a[4].equals(a[5])) {
					yesOrNo = 1;
					type = "sameHead";
					confidence = 10.0;
				} else if (paraphraseLabeled.containsKey(k)) {
					boolean predict = paraphraseLabeled.get(k);
					yesOrNo = predict ? 1 : -1;
					type = "labeled";
					confidence = 100;
				} else if (paraphrasePredicted.containsKey(k)) {
					String[] ab = paraphrasePredicted.get(k).split("_");
					yesOrNo = Integer.parseInt(ab[0]);
					type = "sievepred";
					confidence = Double.parseDouble(ab[1]);
				} else {
					yesOrNo = -1;
					type = "nopred";
					String head0 = Util.pairAB(a[4], a[5]);
					confidence = 0;
					// D.p("why no prediction", a[0], a[1]);
				}
				hc.add(type);
				if (!appeared.contains(k)) {
					dw.write(yesOrNo, confidence, a[0], a[1], a[2], a[3],
							a[4], a[5], a[6], a[7]);
					appeared.add(k);
					count++;
					if (!a[4].equals(a[5]))
						diffcount++;
				}
			}
			// D.p(rvgc.pivotVerb, count, diffcount);
		}
		hc.printAll();
		dr.close();
		dw.close();

	}

	static void loadParaphraseLabeled(String input,
			Map<String, Boolean> isParaphrase) {
		DR dr = new DR(input);
		String[] l;
		while ((l = dr.read()) != null) {
			boolean res = Boolean.parseBoolean(l[0]);
			String[] ab = l[1].split("\\$\\$");
			String r1 = ab[0].replaceAll("_", " ");
			String r2 = ab[1].replaceAll("_", " ");
			String k = Util.pairAB(r1, r2);
			isParaphrase.put(k, res);
		}
		dr.close();
	}

	static void loadParaphrasePredicted(String input,
			Map<String, String> isParaphrase) {
		DR dr = new DR(input);
		String[] l;
		while ((l = dr.read()) != null) {
			int res = l[0].equals("1") ? 1 : -1;
			String r1 = l[1].replaceAll("_", " ");
			String r2 = l[2].replaceAll("_", " ");
			double score = Double.parseDouble(l[3]);
			String k = Util.pairAB(r1, r2);
			isParaphrase.put(k, res + "_" + score);
		}
		dr.close();
	}

	/**
	 * Merge two clusters of two verbgroup (e.g. C-acquire-C & C-buy-C are
	 * equal) if (1) acquire and buy are paraphrase (2) there is an EEC-set
	 * being the instance of both C-acquire-C and C-buy-C
	 */
	public static void clusterCluster(String input_verbgroupclustering,
			String input_relationsInVerbGroup_prediction,
			String output) throws IOException {
		HashMap<String, Double> relpair2predict = new HashMap<String, Double>();
		{
			DR dr = new DR(input_relationsInVerbGroup_prediction);
			String[] l;
			while ((l = dr.read()) != null) {
				String k = Util.pairAB(l[2], l[3]);
				relpair2predict.put(k, Double.parseDouble(l[0]));
			}
			dr.close();
		}
		List<String[]> all = new ArrayList<String[]>();
		{
			DR dr = new DR(input_verbgroupclustering);

			String[] l;
			while ((l = dr.read()) != null) {
				VerbGroupCluster vgc = VerbGroupCluster.fromJsonVerbGroup(l[0]);
				HashMultimap<String, Eec> eecclusters = HashMultimap.create();
				for (int i = 0; i < vgc.clustering.size(); i++) {
					eecclusters.put(vgc.clustering.get(i), vgc.eecs.get(i));
				}
				for (String clustername : eecclusters.keySet()) {
					Set<Eec> eeccluster = eecclusters.get(clustername);
					if (eeccluster.size() < 2 || clustername.equals("NA")) {
						continue;
					}
					for (Eec eec : eeccluster) {
						all.add(DW.tow(vgc.pivotVerb, clustername,
								eec.getNameId()));
					}
				}
			}
		}

		HashMultimap<String, String[]> eec2cluster = HashMultimap.create();
		for (String[] a : all) {
			eec2cluster.put(a[2], a);
		}
		HashMultimap<String, String> posClusterpair2eecs = HashMultimap
				.create();
		HashMultimap<String, String> negClusterpair2eecs = HashMultimap
				.create();
		for (String eecname : eec2cluster.keySet()) {
			List<String[]> temp = new ArrayList<String[]>(
					eec2cluster.get(eecname));
			for (int i = 0; i < temp.size(); i++) {
				for (int j = i + 1; j < temp.size(); j++) {
					String[] x = temp.get(i);
					String[] y = temp.get(j);
					String verbpair = Util.pairAB(x[0], y[0]);
					String clusterpair = Util.pairAB(x[1], y[1]);
					if (relpair2predict.containsKey(verbpair)
							&& relpair2predict.get(verbpair) > 0.9) {
						posClusterpair2eecs.put(clusterpair, "+" + verbpair
								+ "@" + eecname);
					}
					else {
						negClusterpair2eecs.put(clusterpair, "-" + verbpair
								+ "@" + eecname);
					}
				}
			}
		}
		DW dw = new DW(output);
		for (String clusterpair : posClusterpair2eecs.keySet()) {
			Set<String> shouldbeNull = negClusterpair2eecs.get(clusterpair);
			if (shouldbeNull.size() > 0)
				dw.write(clusterpair, posClusterpair2eecs.get(clusterpair),
						negClusterpair2eecs.get(clusterpair));
		}
		dw.close();
	}

	/** return training sentences */
	public static void buildRelationExtractor(
			String input_verbgroupclustering,
			String input_relationsInVerbGroup_prediction,
			String output_sentences
			) throws IOException {
		HashMap<String, Double> relpair2predict = new HashMap<String, Double>();
		{
			DR dr = new DR(input_relationsInVerbGroup_prediction);
			String[] l;
			while ((l = dr.read()) != null) {
				String k = Util.pairAB(l[2], l[3]);
				relpair2predict.put(k, Double.parseDouble(l[0]));
			}
			dr.close();
		}
		{
			DR dr = new DR(input_verbgroupclustering);
			List<String[]> all = new ArrayList<String[]>();
			String[] l;
			while ((l = dr.read()) != null) {
				VerbGroupCluster vgc = VerbGroupCluster.fromJsonVerbGroup(l[0]);
				HashMultimap<String, Eec> eecclusters = HashMultimap.create();
				for (int i = 0; i < vgc.clustering.size(); i++) {
					eecclusters.put(vgc.clustering.get(i), vgc.eecs.get(i));
				}
				for (String clustername : eecclusters.keySet()) {
					Set<Eec> eeccluster = eecclusters.get(clustername);
					if (eeccluster.size() < 2 || clustername.equals("NA")) {
						continue;
					}
					D.p("RELATION CLASS", clustername, eeccluster.size());
					for (Eec eec : eeccluster) {
						for (Tuple t : eec.tuples) {
							String relpair = Util.pairAB(t.getRel(),
									vgc.pivotVerb);
							if (relpair2predict.get(relpair) > 0.9) {
								all.add(DW.tow(clustername, "+", eec.getArg1(),
										eec.getArg2(), t.getRel(),
										t.getSentence()));
							} else {
								all.add(DW.tow(clustername, "-", eec.getArg1(),
										eec.getArg2(), t.getRel(),
										t.getSentence()));
							}
						}
					}
				}
				D.p(vgc.pivotVerb);
			}
			Collections.sort(all, new Comparator<String[]>() {
				@Override
				public int compare(String[] o1, String[] o2) {
					// TODO Auto-generated method stub
					return (o1[0] + "\t" + o1[1]).compareTo(o2[0] + "\t"
							+ o2[1]);
				}
			});
			DW dw = new DW(output_sentences);
			for (String[] w : all) {
				dw.write(w);
			}
			dr.close();
			dw.close();
		}
	}

	public static void main(String[] args) throws IOException {
		if (args[0].equals("-relstr2eecs")) {
			relstr2eecs(args[1], args[2], args[3]);
		}

		if (args[0].equals("-relstr2eecs2rcConsiderArgumentType")) {
			relstr2eecs2rcConsiderArgumentType(args[1], args[2]);
		}
		if (args[0].equals("-generateVerbGroup")) {
			generateVerbGroup(args[1], args[2], 10);
		}
		if (args[0].equals("-letHumanAnnotate")) {
			String input_verbgroup = args[1];
			String output = args[2];
			String output_woFBTypes = output + ".woFBTypes";
			String output_arguments = output + ".arguments";
			String output_argumentsfbtypes = output + ".argumentsFbTypes";
			letHumanAnnotateStep1(input_verbgroup, output_woFBTypes,
					output_arguments);
			getNel(output_arguments, output_argumentsfbtypes);
			letHumanAnnotateStep2(output_woFBTypes, output_argumentsfbtypes,
					output);

		}

		if (args[0].equals("-relstr2groupeecsStep1")) {
			relstr2groupeecsStep1(args[1], args[2], 10);
		}
		if (args[0].equals("-getNel")) {
			getNel(args[1], args[2]);
		}
		if (args[0].equals("-clusterVerbGroupCheating")) {
			clusterVerbGroupCheating(args[1], args[2], args[3]);
		}
		if (args[0].equals("-clusterVerbGroupNaiveModel")) {
			clusterVerbGroupNaiveModel(args[1], args[2]);
		}
		if (args[0].equals("-precisionRecallCompareTwoClusterings")) {
			precisionRecallCompareTwoClusterings(args[1], args[2], args[3]);
		}
		// try to cluster relation phrases
		if (args[0].equals("-tolabelRelationphrasesInVerbGroup")) {
			tolabelRelationphrasesInVerbGroup(args[1], args[2], args[3]);
		}
		if (args[0].equals("-letHumanAnnotateMissing")) {
			letHumanAnnotateMissing(args[1], args[2], args[3], args[4], args[5]);
		}
		if (args[0].equals("-buildRelationExtractor")) {
			buildRelationExtractor(args[1], args[2], args[3]);
		}
		if (args[0].equals("-clusterCluster")) {
			clusterCluster(args[1], args[2], args[3]);
		}

	}
}
