package re;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.gson.Gson;

import javatools.administrative.D;
import javatools.stanford.SentDep;
import javatools.string.RemoveStopwords;
import javatools.string.StringUtil;
import library.ollie.OllieSentence;
import library.ollie.OneOllieExtr;
import reverbstruct.ReverbExtraction;
import reverbstruct.ReverbSentence;
import util.UtilMath;
import util.WordGram;

public class Tuple {
	public Date date;
	public long articleId;
	public int artOffset;

	public String[] tkn;
	public String[] pos;
	public String[] ner;
	public String[] lmma;
	public List<SentDep> deps;

	HashMap<Integer, HashMap<Integer, String>> indexdep;
	public int a1[], a2[], v[];//a1start, a1end, a1head

	private String a1str;
	private String a2str;
	private String relstr;
	String tense;

	String relollie;

	//	public HashBasedTable<Integer, Integer, String> indexdep;

	public Tuple(Date date, ReverbExtraction re, ReverbSentence rs,
			long articleId) {
		this.date = date;
		this.articleId = articleId;
		artOffset = rs.artOffset;
		tkn = rs.tkn;
		pos = rs.pos;
		ner = rs.ner;
		lmma = rs.lmma;
		deps = rs.deps;
		for (SentDep sd : rs.deps) {
			if (!indexdep.containsKey(sd.g)) {
				indexdep.put(sd.g, new HashMap<Integer, String>());
			}
			indexdep.get(sd.g).put(sd.d, sd.t);
		}
		a1 = new int[] { re.arg1.start, re.arg1.end, re.arg1.head };
		a2 = new int[] { re.arg2.start, re.arg2.end, re.arg2.head };
		v = new int[] { re.verb.start, re.verb.end, re.verb.head };
		setRelHead();
		a1str = StringUtil.join(tkn, " ", a1[0], a1[1]);
		a2str = StringUtil.join(tkn, " ", a2[0], a2[1]);
		relstr = getLmmaRel();
	}

	void setRelHead() {
		int start = v[0];
		int end = v[1];
		int head = -1;
		for (SentDep sd : deps) {
			if (pos[sd.g].startsWith("V") && (sd.g < end && sd.g >= start)
					&& (sd.d >= end || sd.d < start)) {
				int h = sd.g;
				//				String v = lmma[h].toLowerCase();
				if (h > head) {
					head = h;
				}
			}
			if (pos[sd.d].startsWith("V") && (sd.d < end && sd.d >= start)
					&& (sd.g >= end || sd.g < start)) {
				int h = sd.d;
				//				String v = lmma[h].toLowerCase();
				//				if (!RemoveStopwords.isStopVerb(v))
				if (h > head)
					head = h;
				//					heads.add(v);
			}
		}
		if (head > 0) {
			v[2] = head;
		} else {
			v[2] = start;
		}
	}

	void setRelHead_newbutdontwork() {
		//		if (this.relstr.equals("will be in")) {
		//			D.p(this.relstr);
		//		}
		int start = v[0];
		int end = v[1];
		int[] count = new int[tkn.length];
		for (SentDep sd : deps) {
			if (pos[sd.g].startsWith("V") && (sd.g < end && sd.g >= start)
					&& (sd.d >= end || sd.d < start)) {
				int h = sd.g;
				count[h]++;

			}
			if (pos[sd.d].startsWith("V") && (sd.d < end && sd.d >= start)
					&& (sd.g >= end || sd.g < start)) {
				int h = sd.d;
				count[h]++;
			}
		}
		int head = end - 1;
		int max = 0;
		for (int i = v[1] - 1; i >= v[0]; i--) {
			if (!RemoveStopwords.isStopVerb(lmma[i])
					&& !RemoveStopwords.isStop(lmma[i])
					&& count[i] > max) {
				head = i;
				max = count[i];
			}
		}
		int lastverb = end - 1;
		for (int i = v[1] - 1; i >= v[0]; i--) {
			if (pos[i].startsWith("V")) {
				lastverb = i;
				break;
			}
		}
		if (max > 0) {
			v[2] = head;
		} else {
			//get the last verb
			v[2] = lastverb;
		}
	}

	private void setArgHead(int[] a) {
		int start = a[0];
		int end = a[1];
		int head = -1;
		for (SentDep sd : deps) {
			if ((sd.g < end && sd.g >= start) && (sd.d >= end || sd.d < start)) {
				int h = sd.g;
				if (h > head) {
					head = h;
				}
			}
			if ((sd.d < end && sd.d >= start) && (sd.g >= end || sd.g < start)) {
				int h = sd.d;
				//				if (!RemoveStopwords.isStopVerb(v))
				if (h > head)
					head = h;
				//					heads.add(v);
			}
		}
		if (head > 0) {
			a[2] = head;
		} else {
			a[2] = end - 1;
		}
	}

	//	public void setIndexDeps() {
	//		indexdep = HashBasedTable.create();
	//		for (SentDep sd : deps) {
	//			indexdep.put(sd.g, sd.d, sd.t);
	//		}
	//	}

	public void setup() {

		//		setIndexDeps();
		setRelHead();
		this.tense = UtilMath.tenseOfVerbPhrase(this.tkn, this.pos, this.v[0],
				this.v[1]);
	}

	static Gson gson = new Gson();

	public static Tuple loadFromJson(String jsonstr) {
		Tuple t = gson.fromJson(jsonstr, Tuple.class);
		t.setup();
		//		t.setIndexDeps();
		//		t.setRelHead();
		//		t.tense = UtilMath.tenseOfVerbPhrase(t.tkn, t.pos, t.v[0], t.v[1]);

		return t;
	}

	public static Tuple loadFromJson(String[] l) {
		Tuple t = gson.fromJson(l[3], Tuple.class);
		t.a1str = l[0];
		t.a2str = l[1];
		t.relstr = l[2];
		t.setup();
		return t;
	}

	public String getArg1() {
		//		if (a1str == null) {
		//			StringBuilder sb = new StringBuilder();
		//			for (int i = a1[0]; i < a1[1]; i++) {
		//				sb.append(lmma[i] + " ");
		//			}
		//			a1str = sb.toString().toLowerCase().trim();
		//		}
		return a1str;
	}

	public String getArg1Ner() {
		return ner[a1[2]];
	}

	public String getArg2Ner() {
		return ner[a2[2]];
	}

	public String getNerStrOfArg1() {
		return nerStrOfArg(a1[2], a1[0], a1[1]);
	}

	public String getNerStrOfArg2() {
		return nerStrOfArg(a2[2], a2[0], a2[1]);
	}

	public String nerStrOfArg(int pos, int argstart, int argend) {

		int start = pos;
		int end = pos + 1;
		for (int k = pos - 1; k >= argstart; k--) {
			if (ner[k].equals(ner[pos])) {
				start = k;
			} else {
				break;
			}
		}
		for (int k = pos + 1; k < argend; k++) {
			if (ner[k].equals(ner[pos])) {
				end = k + 1;
			} else {
				break;
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int k = start; k < end; k++) {
			sb.append(tkn[k] + " ");
		}
		return sb.toString().trim();
	}

	public String getArg2() {
		//		if (a2str == null) {
		//			StringBuilder sb = new StringBuilder();
		//			for (int i = a2[0]; i < a2[1]; i++) {
		//				sb.append(lmma[i] + " ");
		//			}
		//			a2str = sb.toString().toLowerCase().trim();
		//		}
		return a2str;
	}

	public String getRel() {
		//		if (relstr == null) {
		//			StringBuilder sb = new StringBuilder();
		//			for (int i = v[0]; i < v[1]; i++) {
		//				sb.append(lmma[i] + " ");
		//			}
		//			relstr = sb.toString().toLowerCase().trim();
		//		}
		return relstr;
	}

	public String getSentence() {
		return StringUtil.join(this.tkn, " ");
	}

	public String getRelForReverb() {
		StringBuilder sb = new StringBuilder();
		for (int i = v[0]; i < v[1]; i++) {
			sb.append(lmma[i] + " ");
		}
		return sb.toString().toLowerCase().trim();
	}

	public String getRelOllie() {
		if (this.relollie == null) {
			//append prep of (prep_for) into the show name
			StringBuilder sb = new StringBuilder();
			sb.append(relstr).append(" ");
			int vhead = v[2];
			if (indexdep.containsKey(vhead)) {
				for (Entry<Integer, String> e : indexdep.get(vhead).entrySet()) {
					String type = e.getValue();
					int dep = e.getKey();
					if (dep >= a2[0] && dep < a2[1] || dep >= a1[0]
							&& dep < a1[1]
							|| dep >= v[0]
							&& dep < v[1])
						continue;
					if (type.startsWith("prep-")) {
						type = type.replace("prep-", "");
						sb.append("(").append(type).append(" ");
						sb.append(lmma[dep]).append(") ");
					}
				}
				relollie = sb.toString().toLowerCase();
			}
		}
		return relollie;
	}

	public List<Integer> getRelOllieIndex() {
		//append prep of (prep_for) into the show name
		List<Integer> idx = new ArrayList<Integer>();
		int vhead = v[2];
		if (indexdep.containsKey(vhead)) {
			for (Entry<Integer, String> e : indexdep.get(vhead).entrySet()) {
				String type = e.getValue();
				int dep = e.getKey();
				if (dep >= a2[0] && dep < a2[1] || dep >= a1[0] && dep < a1[1]
						|| dep >= v[0]
						&& dep < v[1])
					continue;
				if (type.startsWith("prep-")) {
					idx.add(dep);
				}
			}
		}
		return idx;
	}

	public static String getRidOfOlliePartOfRelation(String s) {
		String r = s;
		if (r.indexOf("(") > 0) {
			r = r.substring(0, r.indexOf("("));
		}
		r = r.trim();
		return r;
	}

	public String getLmmaRel() {
		StringBuilder sb = new StringBuilder();
		for (int i = v[0]; i < v[1]; i++) {
			sb.append(lmma[i] + " ");
		}
		String lmmarel = sb.toString().toLowerCase().trim();
		return lmmarel;
	}

	//Some Ollie relation doesn't have any verb inside! (Michael added the verb to it)
	public boolean hasVerbInRel() {
		boolean ret = false;
		try {
			for (int i = v[0]; i < v[1]; i++) {
				if (this.pos[i].startsWith("V")) {
					ret = true;
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Error\t" + gson.toJson(this.pos));
		}
		return ret;
	}

	public String getRelHead() {
		return lmma[this.v[2]];
	}

	public String getRelHead(WordGram wg) {
		String keyword = null;
		int count = 0;
		if (v[1] - v[0] <= 4) {
			for (int i = v[0]; i < v[1]; i++) {
				String w = lmma[i].toLowerCase();
				//				if (pos[i].startsWith("V") || pos[i].startsWith("N")) {
				if (!w.endsWith("ly")) {
					int c = wg.getCount(lmma[i]);
					if (keyword == null || c < count) {
						keyword = w;
						count = c;
					}
				}
			}
		}
		if (keyword == null) {
			keyword = getRelHead();
		}
		//		D.p(this.getRel(), keyword);
		return keyword;
	}

	//	public String getRelHead() {
	//		int headpos = this.v[2];
	//		for (int i = v[0]; i < v[1]; i++) {
	//			if (pos[i].startsWith("V") && !lmma[i].equals("be") && !lmma[i].equals("do")) {
	//				headpos = i;
	//				break;
	//			}
	//		}
	//		return lmma[headpos];
	//	}

	public String getArg1Head() {
		return lmma[this.a1[2]];
	}

	public String getArg2Head() {
		return lmma[this.a2[2]];
	}

	static HashSet<String> rbForLightVerbs = new HashSet<String>();
	static {
		String[] rbs = new String[] { "out", "off" };
		for (String r : rbs)
			rbForLightVerbs.add(r);
	}

	public String[] getArg1Arg2ArgPairToQueryTimeSeries() {
		String[] ret = new String[3];
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for (int i = a1[1] - 2; i < a1[1]; i++) {
			if (i >= a1[0]) {
				sb1.append(lmma[i] + " ");
			}
		}
		for (int i = a2[1] - 2; i < a2[1]; i++) {
			if (i >= a2[0]) {
				sb2.append(lmma[i] + " ");
			}
		}
		ret[0] = sb1.toString().toLowerCase();
		ret[1] = sb2.toString().toLowerCase();
		ret[2] = ret[0] + "::" + ret[1];
		return ret;

	}

	public String getRelAsVariable() {
		String relOllie = getRelOllie();
		//		StringBuilder sb = new StringBuilder();
		//		for (int i = v[0]; i < v[1]; i++) {
		//			sb.append(tkn[i] + " ");
		//		}
		//		relOllie = sb.toString().toLowerCase().trim();

		//		int vhead = v[2];
		//		Map<Integer, String> map = indexdep.row(vhead);
		//		for (Entry<Integer, String> e : map.entrySet()) {
		//			String type = e.getValue();
		//			int d = e.getKey();
		//			if (d >= v[0] && d < v[1])
		//				continue;
		//			if (type.startsWith("prep_")) {
		//				String x = type.replace("prep_", "");
		//				relOllie += " " + x;
		//			} else if ((type.equals("prt") || type.equals("advmod"))
		//					&& rbForLightVerbs.contains(lmma[d])) {
		//				relOllie += " " + lmma[d];
		//			}
		//		}
		return relOllie.replaceAll(" ", "_");
	}

	boolean isMD() {
		boolean isMD = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			if (pos[i].equals("MD")) {
				isMD = true;
				break;
			}
		}
		return isMD;
	}

	boolean isHAVE() {
		boolean isHAVE = false;
		int start = v[0];
		int end = v[1];
		if (lmma[v[0]].toLowerCase().equals("have")) {
			isHAVE = true;
		}
		return isHAVE;
	}

	boolean isNeg() {
		boolean isHAVE = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			String s = lmma[i].toLowerCase();
			if (s.equals("no") || s.equals("not")) {
				isHAVE = true;
				break;
			}
		}
		return isHAVE;
	}

	boolean isPast() {
		boolean isPast = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			if (pos[i].equals("VBD")) {
				isPast = true;
				break;
			}
		}
		return isPast;
	}

	boolean isPresent() {
		boolean isPresent = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			if (pos[i].equals("VBZ") || pos[i].equals("VBP")) {
				isPresent = true;
				break;
			}
		}
		return isPresent;
	}

	public String[] lmmaExceptTuple() {
		List<String> temp = new ArrayList<String>();
		for (int i = 0; i < lmma.length; i++) {
			if (i >= a1[0] && i < a1[1] || i >= a2[0] && i < a2[1] || i >= v[0]
					&& i < v[1]) {
				continue;
			}
			temp.add(lmma[i]);
		}
		String[] ret = new String[temp.size()];
		for (int i = 0; i < temp.size(); i++) {
			ret[i] = temp.get(i);
		}
		return ret;

	}
}
