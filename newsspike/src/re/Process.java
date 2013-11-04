package re;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import util.Util;

import javatools.administrative.D;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;
import javatools.mydb.Sort;
import javatools.string.StringUtil;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

public class Process {
	public static void model2EECsets(String input_model, String output_eecsets) {
		Gson gson = new Gson();
		DR dr = new DR(input_model);
		DW dw = new DW(output_eecsets);
		List<String[]> b;
		int count = 0;
		while ((b = dr.readBlock(0)) != null) {
			if (++count % 1000 == 0) {
				D.p(count);
			}
			Eec eec = new Eec(b);
			String eecstr = gson.toJson(eec);
			dw.write(eec.getId(), eecstr);
		}
		dw.close();
	}

	public static void relstr2eecid(String input_model,
			String output_relstr2eecid) throws IOException {
		Gson gson = new Gson();
		DR dr = new DR(input_model);
		DW dw_relstr2id = new DW(output_relstr2eecid);
		List<String[]> b;
		int count = 0;
		while ((b = dr.readBlock(0)) != null) {
			if (++count % 1000 == 0) {
				D.p(count);
			}
			Eec eec = new Eec(b);
			String eecstr = gson.toJson(eec);
			Set<String> temp = new HashSet<String>();
			for (Tuple t : eec.getTuples()) {
				temp.add(Util.replaceBlank(t.getRel()));
			}
			for (String r : temp) {
				dw_relstr2id
						.write(r, eec.getId(), eec.getArg1(), eec.getArg2());
			}
		}
		dw_relstr2id.close();
		Sort.sort(output_relstr2eecid, output_relstr2eecid + ".sbrel", ".",
				new Comparator<String[]>() {
					@Override
					public int compare(String[] arg0, String[] arg1) {
						// TODO Auto-generated method stub
						return arg0[0].compareTo(arg1[0]);
					}

				});
	}

	public static void titleRelations(String input_eecsets) {
		Gson gson = new Gson();
		DR dr = new DR(input_eecsets);
		class CC {
			String r;
			List<Eec> eecs;

			public CC(String r) {
				this.r = r;
				eecs = new ArrayList<Eec>();
			}
		}
		String[] l;
		HashMap<String, CC> titlerel2eecs = new HashMap<String, CC>();
		while ((l = dr.read()) != null) {
			Eec eec = gson.fromJson(l[1], Eec.class);
			for (Tuple t : eec.getTuples()) {
				if (t.artOffset == 0) {
					String r = t.getRel();
					if (!titlerel2eecs.containsKey(r)) {
						titlerel2eecs.put(r, new CC(r));
					}
					titlerel2eecs.get(r).eecs.add(eec);
					//					titlerel2sentence.put(t.getRel(), new String[] {
					//							eec.getId() + "",
					//							eec.getArg1(),
					//							eec.getArg2(),
					//							StringUtil.join(t.tkn, " ")
					//					});
				}
			}
		}
		List<CC> CClists = new ArrayList<CC>(titlerel2eecs.values());
		Collections.sort(CClists, new Comparator<CC>() {
			@Override
			public int compare(CC a, CC b) {
				// TODO Auto-generated method stub
				return b.eecs.size() - a.eecs.size();
			}
		});
		for (CC c : CClists) {
			D.p(c.r);
		}
		dr.close();
	}

	public static void main(String[] args) throws IOException {
		//re.Process -model2EECsets model eecsets
		if (args[0].equals("-model2EECsets")) {
			model2EECsets(args[1], args[2]);
		}
		if (args[0].equals("-relstr2eecid")) {
			relstr2eecid(args[1], args[2]);
		}
		if (args[0].equals("-titleRelations")) {
			//check the most important relations, appears in title
			titleRelations(args[1]);
		}
	}
}
