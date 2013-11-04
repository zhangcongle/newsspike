package re;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javatools.filehandlers.DW;
import javatools.ui.HtmlVisual;

public class EvaluateClustering {
	static String NA = "NA";
	List<String> golds;
	List<String> predicts;
	List<String> names;

	public EvaluateClustering(List<String> golds, List<String> predicts) {
		this.golds = golds;
		this.predicts = predicts;
		// this.names = names;
		if (golds.size() != predicts.size()) {
			System.err
					.println("EvaluateClustering: golds.size() != predicts.size()");
		}
	}

	public void setNames(List<String> names) {
		this.names = names;
		if (names.size() != golds.size()) {
			System.err
					.println("EvaluateClustering: names.size() != golds.size()");
		}
	}

	public void pairwiseEvaluation(String prefix, StringBuilder sbdebug, boolean verbose, int[] pr) {
		HashSet<String> goldsPairwiseYes = loadPairwiseYes(golds);
		HashSet<String> predictsPairwiseYes = loadPairwiseYes(predicts);
		{
			List<String[]> prnumbers = new ArrayList<String[]>();
			// if (goldsPairwiseYes.size() == 0) {
			// //unlabeled
			// return;
			// }
			int truepos = 0, falsepos = 0, falseneg = 0;
			for (String x : goldsPairwiseYes) {
				if (predictsPairwiseYes.contains(x)) {
					truepos++;
				} else {
					falseneg++;
				}
			}
			for (String x : predictsPairwiseYes) {
				if (!goldsPairwiseYes.contains(x)) {
					falsepos++;
				}
			}
			pr[0] = truepos;
			pr[1] = falsepos;
			pr[2] = falseneg;

			prnumbers.add(DW.tow("Pairwise precision", truepos * 1.0 / (truepos + falsepos), truepos, falsepos));
			prnumbers.add(DW.tow("Pairwise Recall", truepos * 1.0 / (truepos + falseneg), truepos, falseneg));
			HtmlVisual.json2htmlStrTable("<b>" + prefix + "</b>", prnumbers, sbdebug);
		}
		if (names != null && verbose) {
			List<String[]> debug = new ArrayList<String[]>();
			for (String x : predictsPairwiseYes) {
				if (!goldsPairwiseYes.contains(x)) {
					String[] ab = x.split("\t");
					int a = Integer.parseInt(ab[0]);
					int b = Integer.parseInt(ab[1]);
					debug.add(DW.tow("FalsePos", golds.get(a), predicts.get(a), names.get(a), golds.get(b),
							predicts.get(b), names.get(b)));
				}
			}
			for (String x : goldsPairwiseYes) {
				if (!predictsPairwiseYes.contains(x)) {
					String[] ab = x.split("\t");
					int a = Integer.parseInt(ab[0]);
					int b = Integer.parseInt(ab[1]);
					debug.add(DW.tow("FalseNeg", golds.get(a), predicts.get(a), names.get(a), golds.get(b),
							predicts.get(b), names.get(b)));
				}
			}
			HtmlVisual.json2htmlStrTable("<b>" + prefix + "</b>", debug, sbdebug);
		}
	}

	private HashSet<String> loadPairwiseYes(List<String> tags) {
		HashSet<String> ret = new HashSet<String>();
		for (int i = 0; i < tags.size(); i++)
			for (int j = i + 1; j < tags.size(); j++) {
				if (tags.get(i).equals(NA)
						|| tags.get(j).equals(NA)) {
					continue;
				}
				if (tags.get(i).equals(tags.get(j))) {
					ret.add(i + "\t" + j);
				}
			}
		return ret;
	}

	public static void main(String[] args) {

	}
}
