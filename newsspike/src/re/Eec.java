package re;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

import javatools.filehandlers.DW;
import javatools.string.RemoveStopwords;
import javatools.string.StringUtil;
import javatools.ui.TxtTable;

public class Eec {
	static Gson gson = new Gson();

	private int id;
	private String arg1;
	private String arg2;
	List<Tuple> tuples = new ArrayList<Tuple>();
	List<Article> articles = new ArrayList<Article>();

	public Eec(List<String[]> lines) {
		String[] l0 = lines.get(0);
		this.setId(Integer.parseInt(l0[0]));
		//		if (this.id == 5476) {
		//			D.p(this.id);
		//		}
		this.setArg1(l0[2]);
		this.setArg2(l0[3]);

		for (String[] l : lines) {
			Tuple t = Tuple.loadFromJson(l[5]);
			getTuples().add(t);
			Article a = gson.fromJson(l[6], Article.class);
			articles.add(a);
		}
		{
			//pull tuples in articles that having the same head verbs as those target relation strings
			Set<String> alreadyTuples = new HashSet<String>();
			Set<String> heads = new HashSet<String>();
			Tuple t0 = getTuples().get(0);
			String t0a1head = t0.lmma[t0.a1[2]];
			String t0a2head = t0.lmma[t0.a2[2]];
			for (Tuple t : getTuples()) {
				alreadyTuples.add(t.articleId + "_" + t.artOffset);
				String vhead = t.lmma[t.v[2]].toLowerCase();
				if (!RemoveStopwords.isStopVerb(vhead)) {
					heads.add(vhead);
				}
			}
			for (Article a : articles) {
				for (Tuple t : a.tuples) {
					if (!alreadyTuples
							.contains(t.articleId + "_" + t.artOffset)) {
						String ta1head = t.lmma[t.a1[2]];
						String ta2head = t.lmma[t.a2[2]];
						//						String vhead = t.lmma[t.v[2]].toLowerCase();
						if (//heads.contains(vhead) && 
						t0a1head.equals(ta1head) &&
								t0a2head.equals(ta2head)) {
							t.setup();
							//							aux_tuples.add(t);
							getTuples().add(t);
							alreadyTuples.add(t.articleId + "_" + t.artOffset);
						}
					}
				}
			}
		}
	}

	public String displayInTable() {
		List<String[]> table = new ArrayList<String[]>();
		//		table.add(new String[]{this.id+"",this.arg1,this.arg2});
		for (Tuple t : tuples) {
			table.add(new String[] { t.getRel(), t.articleId + "",
					t.artOffset + "", StringUtil.join(t.tkn, " ") });
		}
		return TxtTable.getTextTableDisplay(4, new int[] { 10, 5,
				5, 60 }, table);
	}

	public String getNameId() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.arg1).append("@").append(this.arg2).append("@")
				.append(new SimpleDateFormat("yyyymmdd").format(this.getDate()));
		return sb.toString();
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getArg2() {
		return arg2;
	}

	public void setArg2(String arg2) {
		this.arg2 = arg2;
	}

	public List<Tuple> getTuples() {
		return tuples;
	}

	public void setTuples(List<Tuple> tuples) {
		this.tuples = tuples;
	}

	public Date getDate() {
		return tuples.get(0).date;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAllRelations() {
		StringBuilder sb = new StringBuilder();
		Set<String> rels = new HashSet<String>();
		for (Tuple t : tuples) {
			rels.add(t.getRel());
		}
		for (String r : rels) {
			sb.append(r + "; ");
		}
		return sb.toString();
	}
}
