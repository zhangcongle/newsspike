package re;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

import javatools.administrative.D;
import javatools.datatypes.HashCount;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;
import javatools.mydb.Sort;
import javatools.mydb.StringTuple;
import javatools.string.RemoveStopwords;
import javatools.string.StringUtil;
import javatools.ui.HtmlVisual;
import javatools.ui.TxtTable;

public class ParaphraseAnnotation {

	static Gson gson = new Gson();

	public static void getHeadRelArgSenPairFromBags(String input_model,
			String output) {
		DR dr = new DR(input_model);
		DW dw = new DW(output);
		List<String[]> b;
		int count = 0;
		while ((b = dr.readBlock(0)) != null) {
			if (++count % 1000 == 0) {
				D.p(count);
				//				break;
			}
			Eec eec = new Eec(b);
			List<Tuple> tuples = eec.getTuples();
			List<String[]> temp = new ArrayList<String[]>();
			for (Tuple t : tuples) {
				//in order to to compute sentence similarity, we need the "stripped sentence": those tokens excluding arg,rel,arg tuple
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < t.tkn.length; i++) {
					if (i >= t.a1[0] && i < t.a1[1] ||
							i >= t.a2[0] && i < t.a2[1] ||
							i >= t.v[0] && i < t.v[1]) {
						continue;
					}
					sb.append(t.lmma[i] + " ");
				}
				temp.add(new String[] { t.getRelHead(), t.getRel(),
						StringUtil.join(t.tkn, " "), sb.toString() });

			}
			Set<String> appeared = new HashSet<String>();
			for (int i = 0; i < temp.size(); i++) {
				for (int j = i + 1; j < temp.size(); j++) {
					String[] x = temp.get(i);
					String[] y = temp.get(j);
					String key = StringUtil.join(new String[] { x[0], y[0],
							x[1], y[1] }, " ");
					if (!x[0].equals(y[0])) {//different heads
						if (!appeared.contains(key)) {
							if (x[0].compareTo(y[0]) > 0) {
								dw.write(x[0], y[0], x[1], y[1], eec.getArg1(),
										eec.getArg2(), x[2], y[2], eec.getId(),
										x[3], y[3]);
							} else {
								dw.write(y[0], x[0], y[1], x[1], eec.getArg1(),
										eec.getArg2(), y[2], x[2], eec.getId(),
										y[3], x[3]);
							}
							appeared.add(key);
						}
					}
				}
			}
		}
		dr.close();
		dw.close();
	}

	public static void getSameHeadRelArgSenPairFromBags(String input_model,
			String output) {
		DR dr = new DR(input_model);
		DW dw = new DW(output);
		List<String[]> b;
		int count = 0;
		while ((b = dr.readBlock(0)) != null) {
			if (++count % 1000 == 0) {
				D.p(count);
				//				break;
			}
			Eec eec = new Eec(b);
			List<Tuple> tuples = eec.getTuples();
			List<String[]> temp = new ArrayList<String[]>();
			for (Tuple t : tuples) {
				//in order to to compute sentence similarity, we need the "stripped sentence": those tokens excluding arg,rel,arg tuple
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < t.tkn.length; i++) {
					if (i >= t.a1[0] && i < t.a1[1] ||
							i >= t.a2[0] && i < t.a2[1] ||
							i >= t.v[0] && i < t.v[1]) {
						continue;
					}
					sb.append(t.lmma[i] + " ");
				}
				temp.add(new String[] { t.getRelHead(), t.getRel(),
						StringUtil.join(t.tkn, " "), sb.toString() });

			}
			Set<String> appeared = new HashSet<String>();
			for (int i = 0; i < temp.size(); i++) {
				for (int j = i + 1; j < temp.size(); j++) {
					String[] x = temp.get(i);
					String[] y = temp.get(j);
					String key = StringUtil.join(new String[] { x[0], y[0],
							x[1], y[1] }, " ");
					String head1 = x[0];
					String head2 = y[0];
					String rel1 = x[1].replaceAll(" ", "_");
					String rel2 = y[1].replaceAll(" ", "_");
					String sen1 = x[2];
					String sen2 = y[2];
					String excludesen1 = x[3];
					String excludesen2 = y[3];
					if (head1.equals(head2) 
							&& !rel1.equals(rel2)
							&& !rel2.contains(rel1)
							&& !rel1.contains(rel2)) {//different heads
						if (!appeared.contains(key)) {
							if (x[0].compareTo(y[0]) > 0) {
								dw.write(x[0], y[0], rel1, rel2, eec.getArg1(),
										eec.getArg2(), x[2], y[2], eec.getId(),
										x[3], y[3]);
							} else {
								dw.write(y[0], x[0], rel2, rel1, eec.getArg1(),
										eec.getArg2(), y[2], x[2], eec.getId(),
										y[3], x[3]);
							}
							appeared.add(key);
						}
					}
				}
			}
		}
		dr.close();
		dw.close();
	}

	public static void display(String input_relpair_sort, String outputdir)
			throws IOException {
		class CC {
			String rel1, rel2;
			List<String[]> lines;
			int numBags;
		}
		(new File(outputdir)).deleteOnExit();
		if (!new File(outputdir).exists()) {
			(new File(outputdir)).mkdir();
		}
		DR dr = new DR(input_relpair_sort);

		List<String[]> b;
		int count = 0;

		List<CC> ccs = new ArrayList<CC>();

		while ((b = dr.readBlock(new int[] { 0, 1 })) != null) {

			Set<Integer> diffBagIds = new HashSet<Integer>();
			for (String[] l : b) {
				diffBagIds.add(Integer.parseInt(l[8]));
			}
			//			if (b.size() > 1 && diffBagIds.size() > 1)
			{

				String[] x = b.get(0);
				CC cc = new CC();
				ccs.add(cc);
				cc.rel1 = x[0];
				cc.rel2 = x[1];
				cc.lines = b;
				cc.numBags = diffBagIds.size();
			}

		}
		Collections.sort(ccs, new Comparator<CC>() {
			@Override
			public int compare(CC o1, CC o2) {
				// TODO Auto-generated method stub
				return o2.numBags - o1.numBags;
			}
		});

		//		DW dw = new DW(outputdir + "/0.txt");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.txt"), "utf-8"));
		BufferedWriter bwlabel = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.label.txt"), "utf-8"));
		//		DW dwhtml = new DW(outputdir + "/0.html");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ccs.size(); i++) {
			CC cc = ccs.get(i);
			//			System.out.println(TxtTable.getTextTableDisplay(cc.lines));
			HtmlVisual
					.json2htmlStrTable(cc.rel1 + "::" + cc.rel2 + "::"
							+ cc.numBags, cc.lines, sb);
			Set<String> appeared = new HashSet<String>();
			HashCount<String> relpaircount = new HashCount<String>();
			HashMap<String, List<String[]>> key2set = new HashMap<String, List<String[]>>();
			for (String[] l : cc.lines) {
				String key = l[2] + '\t' + l[3];
				relpaircount.add(l[2] + "\t" + l[3]);
				if (!key2set.containsKey(key)) {
					key2set.put(key, new ArrayList<String[]>());
				}
				key2set.get(key).add(new String[] { l[4], l[5], l[6], l[7] });
			}
			for (Entry<String, List<String[]>> e : key2set.entrySet()) {
				count++;
				String w = "1\t" + "\t" +
						e.getKey().replaceAll(" ", "_") + "\t" + cc.rel1 + "\t"
						+ cc.rel2 + "\t"
						+ cc.numBags;

				bw.write(w + "\r\n");
				bwlabel.write(w + "\n");
				List<String[]> table = e.getValue();
				bw.write(TxtTable.getTextTableDisplay(4, new int[] { 10, 10,
						30, 30 }, table));
				bw.write("\r\n");
			}
			//			for (String[] l : cc.lines) {
			//				String k = l[2] + "\t" + l[3];
			//				if (!appeared.contains(k)) {
			//					count++;
			//					bw.write("LABEL\t[X]\t" + l[0] + "\t" + l[1] + "\t"
			//							+ cc.numBags
			//							+ "\t" + l[2].replaceAll(" ", "_") + "\t" +
			//							l[3].replaceAll(" ", "_") + "\t"
			//							+ relpaircount.see(k) + "\r\n");
			//					appeared.add(k);
			//				}
			//			}
			//			bw.write(TxtTable.getTextTableDisplay(cc.lines));
			if (count % 100 == 0) {
				//				dwhtml.write(sb.toString());
				sb = new StringBuilder();
				bwlabel.close();
				bw.close();
				bwlabel = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputdir + "/" + count
								+ ".label.txt"), "utf-8"));
				bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputdir + "/" + count + ".txt"),
						"utf-8"));
				//				dwhtml = new DW(outputdir + "/" + count + ".html");
			}
		}
		//		dwhtml.write(sb.toString());
		//		dwhtml.close();
		bwlabel.close();
		D.p(count);
		dr.close();
		bw.close();
	}

	public static void displayAtLeast2(String input_relpair_sort,
			String outputdir, String dir_labeled)
			throws IOException {
		class CC {
			String rel1, rel2;
			List<String[]> lines;
			int numBags;
		}
		(new File(outputdir)).deleteOnExit();
		if (!new File(outputdir).exists()) {
			(new File(outputdir)).mkdir();
		}
		DR dr = new DR(input_relpair_sort);

		List<String[]> b;
		int count = 0;

		List<CC> ccs = new ArrayList<CC>();
		while ((b = dr.readBlock(new int[] { 0, 1, 2, 3 })) != null) {
			Set<String> diffBag = new HashSet<String>();
			for (String[] l : b) {
				diffBag.add(l[4] + "\t" + l[5]);
			}
			if (b.size() > 1 && diffBag.size() > 1) {

				String[] x = b.get(0);
				CC cc = new CC();
				ccs.add(cc);
				cc.rel1 = x[0];
				cc.rel2 = x[1];
				cc.lines = b;
				cc.numBags = diffBag.size();
			}

		}
		Collections.sort(ccs, new Comparator<CC>() {
			@Override
			public int compare(CC o1, CC o2) {
				// TODO Auto-generated method stub
				return o2.numBags - o1.numBags;
			}
		});

		//		DW dw = new DW(outputdir + "/0.txt");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.txt"), "utf-8"));
		BufferedWriter bwlabel = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.label.txt"), "utf-8"));
		//		DW dwhtml = new DW(outputdir + "/0.html");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ccs.size(); i++) {
			CC cc = ccs.get(i);
			//			System.out.println(TxtTable.getTextTableDisplay(cc.lines));
			HtmlVisual
					.json2htmlStrTable(cc.rel1 + "::" + cc.rel2 + "::"
							+ cc.numBags, cc.lines, sb);
			Set<String> appeared = new HashSet<String>();
			HashCount<String> relpaircount = new HashCount<String>();
			HashMap<String, List<String[]>> key2set = new HashMap<String, List<String[]>>();
			for (String[] l : cc.lines) {
				String key = l[2] + '\t' + l[3];
				relpaircount.add(l[2] + "\t" + l[3]);
				if (!key2set.containsKey(key)) {
					key2set.put(key, new ArrayList<String[]>());
				}
				key2set.get(key).add(new String[] { l[4], l[5], l[6], l[7] });
			}
			for (Entry<String, List<String[]>> e : key2set.entrySet()) {
				count++;
				String w = count + "\t" + e.getKey().replaceAll(" ", "_")
						+ "\t"
						+ cc.rel1 + "\t" + cc.rel2 + "\t" + cc.numBags;
				bw.write(w + "\r\n");
				bwlabel.write(w + "\n");
				List<String[]> table = e.getValue();
				List<String[]> showtable = new ArrayList<String[]>();
				for (int k = 0; k < 10 && k < table.size(); k++) {
					showtable.add(table.get(k));
				}
				bw.write(TxtTable.getTextTableDisplay(4, new int[] { 10, 10,
						30, 30 }, showtable));
				bw.write("\r\n");
			}
			//			for (String[] l : cc.lines) {
			//				String k = l[2] + "\t" + l[3];
			//				if (!appeared.contains(k)) {
			//					count++;
			//					bw.write("LABEL\t[X]\t" + l[0] + "\t" + l[1] + "\t"
			//							+ cc.numBags
			//							+ "\t" + l[2].replaceAll(" ", "_") + "\t" +
			//							l[3].replaceAll(" ", "_") + "\t"
			//							+ relpaircount.see(k) + "\r\n");
			//					appeared.add(k);
			//				}
			//			}
			//			bw.write(TxtTable.getTextTableDisplay(cc.lines));
			//			if (count % 100 == 0) {
			if (false) {
				//				dwhtml.write(sb.toString());
				sb = new StringBuilder();
				bwlabel.close();
				bw.close();
				bwlabel = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputdir + "/" + count
								+ ".label.txt"), "utf-8"));
				bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputdir + "/" + count + ".txt"),
						"utf-8"));
				//				dwhtml = new DW(outputdir + "/" + count + ".html");
			}
		}
		//		dwhtml.write(sb.toString());
		//		dwhtml.close();
		bwlabel.close();
		D.p(count);
		dr.close();
		bw.close();
	}

	public static void loadLabeled(String file,
			Map<StringTuple, Boolean> labeled) {
		DR dr = new DR(file);
		String[] l;
		while ((l = dr.read()) != null) {
			boolean label = false;
			if (l[0].equals("Y")) {
				label = true;
			}
			StringTuple key = new StringTuple(true, l[1],
					l[2]);
			if (!labeled.containsKey(key) || !label) {
				labeled.put(key, label);
			}
		}
		dr.close();
	}

	public static void displayPhraseOverlap(String input_relpair_sort,
			String outputdir, String dir_labeled)
			throws IOException {
		Map<StringTuple, Boolean> labeled = new HashMap<StringTuple, Boolean>();
		if ((new File(dir_labeled)).exists()) {
			String[] list = (new File(dir_labeled)).list();
			for (String f : list) {
				if (f.contains(".congle.")) {
					loadLabeled(dir_labeled + File.separator + f, labeled);
				}
			}
		}
		class CC {
			String rel1, rel2;
			List<String[]> lines;
			int sharedWords;
			String strSharedWords;
		}
		(new File(outputdir)).deleteOnExit();
		if (!new File(outputdir).exists()) {
			(new File(outputdir)).mkdir();
		}
		DR dr = new DR(input_relpair_sort);

		List<String[]> b;
		int count = 0;

		List<CC> ccs = new ArrayList<CC>();
		while ((b = dr.readBlock(new int[] { 0, 1, 2, 3 })) != null) {
			String r1 = b.get(0)[2];
			String r2 = b.get(0)[3];
			//			int numOfShareWords = StringUtil.numOfShareWords(r1,
			//					r2, new boolean[] { true, true, false });
			List<String> sharedwords = StringUtil.getSharedWords(r1, r2,
					new boolean[] { true, true, false });
			//			StringTuple st = new StringTuple(true, r1.replaceAll(" ", "_"),
			//					r2.replaceAll(" ", "_"));
			int numOfShareWords = sharedwords.size();
			if (numOfShareWords > 0) {
				String[] x = b.get(0);
				CC cc = new CC();
				ccs.add(cc);
				cc.rel1 = x[0];
				cc.rel2 = x[1];
				cc.lines = b;
				cc.sharedWords = numOfShareWords;
				cc.strSharedWords = gson.toJson(sharedwords);
			}
		}
		Collections.sort(ccs, new Comparator<CC>() {
			@Override
			public int compare(CC o1, CC o2) {
				// TODO Auto-generated method stub
				return o2.sharedWords - o1.sharedWords;
			}
		});

		//		DW dw = new DW(outputdir + "/0.txt");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.txt"), "utf-8"));
		BufferedWriter bwlabel = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.label.txt"), "utf-8"));
		//		DW dwhtml = new DW(outputdir + "/0.html");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ccs.size(); i++) {
			CC cc = ccs.get(i);
			HashMap<StringTuple, List<String[]>> key2set = new HashMap<StringTuple, List<String[]>>();
			for (String[] l : cc.lines) {
				StringTuple key = new StringTuple(true, l[2], l[3]);
				if (!key2set.containsKey(key)) {
					key2set.put(key, new ArrayList<String[]>());
				}
				key2set.get(key).add(new String[] { l[4], l[5], l[6], l[7] });
			}
			for (Entry<StringTuple, List<String[]>> e : key2set.entrySet()) {
				if (labeled.containsKey(e.getKey())) {
					continue;
				}
				count++;
				String[] r1r2 = e.getKey().getTuple();
				String w = count + "\t" + r1r2[0].replaceAll(" ", "_") + "\t"
						+ r1r2[1].replaceAll(" ", "_")
						+ "\t" + cc.rel1 + "\t" + cc.rel2 + "\t"
						+ cc.strSharedWords + "\t"
						+ cc.sharedWords;
				bw.write(w + "\r\n");
				bwlabel.write(w + "\n");
				List<String[]> table = e.getValue();
				List<String[]> showtable = new ArrayList<String[]>();
				for (int k = 0; k < 10 && k < table.size(); k++) {
					showtable.add(table.get(k));
				}
				bw.write(TxtTable.getTextTableDisplay(4, new int[] { 10, 10,
						30, 30 }, showtable));
				bw.write("\r\n");
			}
			//			for (String[] l : cc.lines) {
			//				String k = l[2] + "\t" + l[3];
			//				if (!appeared.contains(k)) {
			//					count++;
			//					bw.write("LABEL\t[X]\t" + l[0] + "\t" + l[1] + "\t"
			//							+ cc.numBags
			//							+ "\t" + l[2].replaceAll(" ", "_") + "\t" +
			//							l[3].replaceAll(" ", "_") + "\t"
			//							+ relpaircount.see(k) + "\r\n");
			//					appeared.add(k);
			//				}
			//			}
			//			bw.write(TxtTable.getTextTableDisplay(cc.lines));
			//			if (count % 100 == 0) {
			if (false) {
				//				dwhtml.write(sb.toString());
				sb = new StringBuilder();
				bwlabel.close();
				bw.close();
				bwlabel = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputdir + "/" + count
								+ ".label.txt"), "utf-8"));
				bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputdir + "/" + count + ".txt"),
						"utf-8"));
				//				dwhtml = new DW(outputdir + "/" + count + ".html");
			}
		}
		//		dwhtml.write(sb.toString());
		//		dwhtml.close();
		bwlabel.close();
		D.p(count);
		dr.close();
		bw.close();
	}

	public static void displaySentenceSim(String input_relpair_sort,
			String outputdir, String dir_labeled)
			throws IOException {
		Map<StringTuple, Boolean> labeled = new HashMap<StringTuple, Boolean>();
		if ((new File(dir_labeled)).exists()) {
			String[] list = (new File(dir_labeled)).list();
			for (String f : list) {
				if (f.contains(".congle.")) {
					loadLabeled(dir_labeled + File.separator + f, labeled);
				}
			}
		}
		if (labeled.containsKey(new StringTuple(false,
				"have_reach_a_agreement_with", "hire"))) {
			D.p(labeled.get(new StringTuple(false,
					"have_reach_a_agreement_with", "hire")));
		}
		class CC {
			String rel1, rel2;
			List<String[]> lines;
			double sim;
		}
		(new File(outputdir)).deleteOnExit();
		if (!new File(outputdir).exists()) {
			(new File(outputdir)).mkdir();
		}
		DR dr = new DR(input_relpair_sort);

		List<String[]> b;
		int count = 0;

		List<CC> ccs = new ArrayList<CC>();
		while ((b = dr.readBlock(new int[] { 0, 1, 2, 3 })) != null) {
			String r1 = b.get(0)[2];
			String r2 = b.get(0)[3];
			double greatestsimilarity = 0;
			for (String[] l : b) {
				if (l[6].equals(l[7]))
					continue;
				if (r1.contains(r2) || r2.contains(r1))
					continue;
				String sen1 = l[9];
				String sen2 = l[10];
				double sim = StringUtil.cosineSimilarity(sen1.split(" "),
						sen2.split(" "));
				if (sim > greatestsimilarity) {
					greatestsimilarity = sim;
				}
			}
			{
				String[] x = b.get(0);
				CC cc = new CC();
				ccs.add(cc);
				cc.rel1 = x[0];
				cc.rel2 = x[1];
				cc.lines = b;
				cc.sim = greatestsimilarity;
			}
		}
		Collections.sort(ccs, new Comparator<CC>() {
			@Override
			public int compare(CC o1, CC o2) {
				// TODO Auto-generated method stub
				return Double.compare(o2.sim, o1.sim);
			}
		});

		//		DW dw = new DW(outputdir + "/0.txt");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.txt"), "utf-8"));
		BufferedWriter bwlabel = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputdir + "/0.label.txt"), "utf-8"));
		//		DW dwhtml = new DW(outputdir + "/0.html");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ccs.size(); i++) {
			CC cc = ccs.get(i);
			if (cc.sim < 0.8)
				continue;
			HashMap<StringTuple, List<String[]>> key2set = new HashMap<StringTuple, List<String[]>>();
			for (String[] l : cc.lines) {
				StringTuple key = new StringTuple(true, l[2].replaceAll(" ",
						"_"), l[3].replaceAll(" ", "_"));
				if (!key2set.containsKey(key)) {
					key2set.put(key, new ArrayList<String[]>());
				}
				key2set.get(key).add(new String[] { l[4], l[5], l[6], l[7] });
			}
			for (Entry<StringTuple, List<String[]>> e : key2set.entrySet()) {
				if (labeled.containsKey(e.getKey())) {
					continue;
				}

				count++;
				String[] r1r2 = e.getKey().getTuple();
				String w = count + "\t" + r1r2[0].replaceAll(" ", "_") + "\t"
						+ r1r2[1].replaceAll(" ", "_")
						+ "\t" + cc.rel1 + "\t" + cc.rel2 + "\t"
						+ cc.sim;
				bw.write(w + "\r\n");
				bwlabel.write(w + "\n");
				List<String[]> table = e.getValue();
				List<String[]> showtable = new ArrayList<String[]>();
				for (int k = 0; k < 10 && k < table.size(); k++) {
					showtable.add(table.get(k));
				}
				bw.write(TxtTable.getTextTableDisplay(4, new int[] { 10, 10,
						30, 30 }, showtable));
				bw.write("\r\n");
			}

		}
		//		dwhtml.write(sb.toString());
		//		dwhtml.close();
		bwlabel.close();
		D.p(count);
		dr.close();
		bw.close();
	}

	public static void getLabeledParaphrase(String input,
			String output) {
		Date d = new Date();
		DW dw_congle = new DW(output);
		DR dr_error = new DR(input + ".error");
		DR dr_all = new DR(input + ".label.txt");
		Set<String> error = new HashSet<String>();
		String[] l;
		{
			while ((l = dr_error.read()) != null) {
				if (l.length < 3)
					continue;
				error.add(l[1] + "\t" + l[2]);
			}
		}
		{
			while ((l = dr_all.read()) != null) {
				if (error.contains(l[1] + "\t" + l[2])) {
					dw_congle.write("N", l[1], l[2], l[3], l[4]);
				} else {
					dw_congle.write("Y", l[1], l[2], l[3], l[4]);
				}
			}
		}
		dw_congle.close();
		dr_error.close();
		dr_all.close();
	}

	public static void main(String[] args) throws IOException {
		if (args[0].equals("-getHeadRelArgSenPairFromBags")) {
			//-getHeadRelArgSenPairFromBags model relpair
			getHeadRelArgSenPairFromBags(args[1], args[2]);
			Sort.sort(args[2], args[2] + ".sort", ".",
					new Comparator<String[]>() {
						@Override
						public int compare(String[] o1, String[] o2) {
							// TODO Auto-generated method stub
							String key1 = StringUtil.join(new String[] { o1[0],
									o1[1], o1[2], o1[3] }, " ");
							String key2 = StringUtil.join(new String[] { o2[0],
									o2[1], o2[2], o2[3] }, " ");
							return key1.compareTo(key2);
						}
					});

			displayAtLeast2(args[2] + ".sort", args[2] + ".labelatleast2",
					args[2]
							+ ".congle");
		}
		if (args[0].equals("-getSameHeadRelArgSenPairFromBags")) {
			//-getHeadRelArgSenPairFromBags model relpair
			getSameHeadRelArgSenPairFromBags(args[1], args[2]);
			Sort.sort(args[2], args[2] + ".sort", ".",
					new Comparator<String[]>() {
						@Override
						public int compare(String[] o1, String[] o2) {
							// TODO Auto-generated method stub
							String key1 = StringUtil.join(new String[] { o1[0],
									o1[1], o1[2], o1[3] }, " ");
							String key2 = StringUtil.join(new String[] { o2[0],
									o2[1], o2[2], o2[3] }, " ");
							return key1.compareTo(key2);
						}
					});

			displayAtLeast2(args[2] + ".sort", args[2] + ".labelatleast2",
					args[2]
							+ ".congle");
		}
		if (args[0].equals("-labelcommonheadverb")) {
			display(args[1] + ".sort", args[1] + ".label.commonheadverb");
		}
		if (args[0].equals("-labelAtLeast2")) {
			displayAtLeast2(args[1] + ".sort", args[1] + ".label.atleast2",
					args[1] + ".congle");
		}
		if (args[0].equals("-labelphraseoverlap")) {
			//-labelphraseoverlap relpair
			displayPhraseOverlap(args[1] + ".sort", args[1] + ".label.overlap",
					args[1] + ".congle");
		}
		if (args[0].equals("-labelSentenceSim")) {
			//-labelSentenceSim relpair
			displaySentenceSim(args[1] + ".sort", args[1] + ".label.sensim",
					args[1] + ".congle");
		}
		if (args[0].equals("-getLabeledParaphrase")) {
			//-getLabeledParaphrase relpair.congle/0
			//			getLabeledParaphrase(args[1]);
			getLabeledParaphrase(
					"/projects/pardosa/data17/clzhang/re/exp1/relpair.label.commonheadverb/0",
					"/projects/pardosa/data17/clzhang/re/exp1/relpair.congle/commonhead.congle");
			getLabeledParaphrase(
					"/projects/pardosa/data17/clzhang/re/exp1/relpair.label.atleast2/0",
					"/projects/pardosa/data17/clzhang/re/exp1/relpair.congle/atleast2.congle");
			getLabeledParaphrase(
					"/projects/pardosa/data17/clzhang/re/exp1/relpair.label.overlap/0",
					"/projects/pardosa/data17/clzhang/re/exp1/relpair.congle/overlap.congle");
		}

	}
}
