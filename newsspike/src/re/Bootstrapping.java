package re;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.Util;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

import javatools.administrative.D;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;
import javatools.string.StringUtil;
import javatools.ui.TxtTable;

public class Bootstrapping {
	static Gson gson = new Gson();

	public static void selectSeeds(String input_eecsets,
			String output_seeds) {
		DW dw = new DW(output_seeds);
		DR dr = new DR(input_eecsets);
		String[] l;
		while ((l = dr.read()) != null) {
			int id = Integer.parseInt(l[0]);
			if (id == 472) {
				Eec eec = gson.fromJson(l[1], Eec.class);
				dw.write("company_hire_person", "hire", l[1]);
				System.out.println(eec.displayInTable());
			}
			if (id == 14452) {
				Eec eec = gson.fromJson(l[1], Eec.class);
				dw.write("company_fire_person", "fire", l[1]);
				System.out.println(eec.displayInTable());
			}
		}
		dr.close();
		dw.close();
	}

	public static void bootInit(String input_eecsets,
			String input_relstr2eecid,
			String input_seeds,
			String output_prefix) throws IOException {
		HashMap<Integer, Eec> eecid2eec = new HashMap<Integer, Eec>();
		{
			DR dr = new DR(input_eecsets);
			String[] l;
			while ((l = dr.read()) != null) {
				Eec eec = gson.fromJson(l[1], Eec.class);
				eecid2eec.put(eec.getId(), eec);
			}
			dr.close();
		}
		System.err.println("Load Complete");
		HashMultimap<String, Integer> map_relstr2eecid = HashMultimap.create();
		HashMultimap<Integer, String> map_eecid2relstr = HashMultimap.create();
		{
			DR dr = new DR(input_relstr2eecid);
			String[] l;
			while ((l = dr.read()) != null) {
				String relstr = l[0];
				int eecid = Integer.parseInt(l[1]);
				map_relstr2eecid.put(relstr, eecid);
				map_eecid2relstr.put(eecid, relstr);
			}
			dr.close();
		}
		{
			DR dr = new DR(input_seeds);
			String[] l;
			while ((l = dr.read()) != null) {
				//for one relation, bootstrap one level
				int LABELID = 0;
				HashSet<String> seeds = new HashSet<String>();
				String relname = l[0];
				String[] seed_rels = l[1].split(";");
				for (String s : seed_rels)
					seeds.add(s);

				Set<String> allRelations = new HashSet<String>();
				Set<Integer> alleecs = new HashSet<Integer>();
				allRelations.addAll(seeds);
				D.p(allRelations.toString());
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(output_prefix + "/" + relname
								+ ".eec.tolabel"), "utf-8"));
				DW dw = new DW(output_prefix + "/" + relname
						+ ".eec.tolabel.list");
				HashMultimap<String, String[]> rel2evidences = HashMultimap
						.create();
				HashSet<String> duplicated = new HashSet<String>();
				for (String r : allRelations) {
					D.p("extend one r", r);
					Set<Integer> temp = map_relstr2eecid.get(r);
					for (int eecid : temp) {
						if (!alleecs.contains(eecid)) {
							StringBuilder sb = new StringBuilder();
							alleecs.add(eecid);
							Eec eec = eecid2eec.get(eecid);
							sb.append(Util.replaceBlank("EEC[]\t"
									+ eec.getArg1() + "\t" + eec.getArg2()
									+ "\t"
									+ eec.getDate())
									+ "\r\n");
							Set<String> rels = new HashSet<String>();
							for (int tid = 0; tid < eec.getTuples().size(); tid++) {
								//							for (Tuple t : eec.getTuples()) {
								Tuple t = eec.getTuples().get(tid);
								String rel = Util.replaceBlank(t.getRel());
								String d = rel + "\t" + eec.getArg1() + "\t"
										+ eec.getArg2();
								if (!duplicated.contains(d)) {
									rel2evidences.put(rel,
											new String[] {
													LABELID + "[]",
													eec.getId() + "",
													eec.getArg1(),
													eec.getArg2(),
													StringUtil.join(t.tkn, " ")
											});
									dw.write(LABELID, eec.getId(), tid, gson.toJson(t));
									LABELID++;
									duplicated.add(d);
								}
							}
						}
					}
				}
				for (String rel : rel2evidences.keySet()) {
					StringBuilder sb = new StringBuilder();
					sb.append("RELATION[Y]\t" + rel + "\n");
					List<String[]> all = new ArrayList<String[]>(
							rel2evidences.get(rel));
					sb.append(TxtTable.getTextTableDisplay(5, new int[] {10, 10,
							15, 15, 50 }, all));
//					for (String[] a : all) {
//						sb.append("sentence[Y]\t" + a[0] + "\t" + a[1] + "\t"
//								+ a[2] + "\t" + a[3] + "\n");
//					}
					bw.write(sb.toString() + "\n\n");
				}
				bw.close();
				dw.close();
			}
			dr.close();
		}
	}

	public static void bootAllInit(String input_eecsets,
			String input_relstr2eecid,
			String[] seedRelationPhrases,
			String output_prefix) throws IOException {
		BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(output_prefix + "." + 0
								+ ".eec.tolabel"),
						"utf-8"));
		//		DW dweectolabel = new DW(output_prefix + "." + 0 + ".eec.tolabel");
		HashMap<Integer, Eec> eecid2eec = new HashMap<Integer, Eec>();
		{
			DR dr = new DR(input_eecsets);
			String[] l;
			while ((l = dr.read()) != null) {
				Eec eec = gson.fromJson(l[1], Eec.class);
				eecid2eec.put(eec.getId(), eec);
			}
			dr.close();
		}
		System.err.println("Load Complete");
		DW dwlog = new DW(output_prefix + ".log");
		HashSet<String> seeds = new HashSet<String>();
		{
			for (String s : seedRelationPhrases)
				seeds.add(s);
		}
		HashMultimap<String, Integer> map_relstr2eecid = HashMultimap.create();
		HashMultimap<Integer, String> map_eecid2relstr = HashMultimap.create();
		{
			DR dr = new DR(input_relstr2eecid);
			String[] l;
			while ((l = dr.read()) != null) {
				String relstr = l[0];
				int eecid = Integer.parseInt(l[1]);
				map_relstr2eecid.put(relstr, eecid);
				map_eecid2relstr.put(eecid, relstr);
			}
			dr.close();
		}
		int ITERATION = 10;
		Set<String> allRelations = new HashSet<String>();
		Set<Integer> alleecs = new HashSet<Integer>();

		Set<String> bufferRelations = new HashSet<String>();
		Set<Integer> bufferEecids = new HashSet<Integer>();

		List<String[]> log = new ArrayList<String[]>();
		bufferRelations.addAll(seeds);
		allRelations.addAll(seeds);
		//		for (int k = 1; k <= ITERATION; k++) {
		int iter = 0;
		{
			//from relation strings to eecsets
			HashMultimap<String, String[]> rel2evidences = HashMultimap
					.create();
			HashSet<String> duplicated = new HashSet<String>();
			for (String r : bufferRelations) {
				Set<Integer> temp = map_relstr2eecid.get(r);
				for (int eecid : temp) {
					if (!alleecs.contains(eecid)) {
						StringBuilder sb = new StringBuilder();
						bufferEecids.add(eecid);
						log.add(DW.tow("rel-->eec", iter, r, eecid));
						Eec eec = eecid2eec.get(eecid);
						sb.append(Util.replaceBlank("EEC[]\t"
								+ eec.getArg1() + "\t" + eec.getArg2() + "\t"
								+ eec.getDate()) + "\r\n");
						Set<String> rels = new HashSet<String>();
						for (Tuple t : eec.getTuples()) {
							String rel = Util.replaceBlank(t.getRel());
							String d = rel + "\t" + eec.getArg1() + "\t"
									+ eec.getArg2();
							if (!duplicated.contains(d)) {
								rel2evidences.put(rel, new String[] {
										eec.getArg1(),
										eec.getArg2(),
										StringUtil.join(t.tkn, " ")
								});
								duplicated.add(d);
							}

							//							rels.add(Util.replaceBlank(t.getRel()));
						}
						//						for (String rel : rels) {
						//							rel2evidences.put(rel, new String[] {
						//									eec.getArg1(),
						//									eec.getArg2(),
						//
						//							});
						//							sb.append("REL[]\t" + rel + "\r\n");
						//							allRelations.add(rel);
						//						}
						//						sb.append(eec.displayInTable() + "\r\n");
						//						bw.write(sb.toString());
					}
				}
			}

			for (String rel : rel2evidences.keySet()) {
				StringBuilder sb = new StringBuilder();
				sb.append("RELATION[Y]\t" + rel + "\n");
				List<String[]> all = new ArrayList<String[]>(
						rel2evidences.get(rel));
				sb.append(TxtTable.getTextTableDisplay(3, new int[] { 15, 15,
						50 }, all));
				for (String[] a : all) {
					sb.append("ONER[Y]\t" + a[0] + "\t" + a[1] + "\t" + a[2]
							+ "\n");
				}
				bw.write(sb.toString() + "\n\n");
			}

			//			allRelations.addAll(bufferRelations);
			//			bufferRelations = new HashSet<String>();
			//			//from eec to relation strings
			//			for (int eecid : bufferEecids) {
			//				Set<String> rels = map_eecid2relstr.get(eecid);
			//				for (String r : rels) {
			//					if (!allRelations.contains(r)) {
			//						bufferRelations.add(r);
			//						log.add(DW.tow("eec-->rel", k, eecid, r));
			//					}
			//				}
			//			}
			//			alleecs.addAll(bufferEecids);
			//			bufferEecids = new HashSet<Integer>();
		}
		//print the log
		for (String[] l : log) {
			dwlog.write(l);
		}
		dwlog.close();
		bw.close();
		//		dweectolabel.close();
	}

	public static void selectSeed(String input_eecsets,
			String[] seedRelationPhrases,
			String output) {
		HashSet<String> seeds = new HashSet<String>();
		for (String s : seedRelationPhrases)
			seeds.add(s);
		DR dr = new DR(input_eecsets);
		DW dw = new DW(output);
		String[] l;
		while ((l = dr.read()) != null) {
			Eec eec = gson.fromJson(l[0], Eec.class);
			boolean yes = false;
			for (Tuple t : eec.getTuples()) {
				if (seeds.contains(t.getRel())) {
					yes = true;
				}
			}
			if (yes) {
				D.p(eec.getArg1(), eec.getArg2(), eec.getDate());
				System.out.println(eec.displayInTable());
				dw.write(l);
			}
		}
		dr.close();
		dw.close();
	}

	public static void selectSeed2(String input_model) {
		DR dr = new DR(input_model);
		List<String[]> b;
		int count = 0;
		while ((b = dr.readBlock(0)) != null) {
			if (++count % 1000 == 0) {
				//				D.p(count);
				//				break;
			}
			Eec eec = new Eec(b);
			{
				List<Tuple> tuples = eec.getTuples();
				for (Tuple t : tuples) {
					if (t.getRel().equals("hire")) {
						D.p(eec.getArg1(), eec.getArg2());
					}
				}
			}
		}
		dr.close();
	}

	public static void main(String[] args) throws IOException {
		String dir = "/projects/pardosa/data17/clzhang/re/exp2";
		String input_eecsets = dir + "/eecsets";
		String input_relstr2eecid = dir + "/relstr2eecid";
		String output_seedeecs = dir + "/seeds/seeds";
		String dir_seeds = dir + "/seeds";
		String dir_sons = dir + "/sons";

		if (args[0].equals("-selectSeeds")) {
			selectSeeds(input_eecsets, dir_seeds + "/seeds");
		}
		if (args[0].equals("-bootInit")) {
			bootInit(input_eecsets,
					input_relstr2eecid,
					output_seedeecs,
					dir_sons);
		}
		if (args[0].equals("-bootAllInit")) {
			bootAllInit(input_eecsets,
					input_relstr2eecid,
					new String[] { "hire" },
					dir_seeds + "/hire");
		}
		//		selectSeed(input_eecsets,
		//				new String[] { "hire" },
		//				dir_seeds + "/hire");
	}
}
