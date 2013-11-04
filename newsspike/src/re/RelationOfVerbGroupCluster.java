package re;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javatools.filehandlers.DW;
import javatools.string.StringUtil;

public class RelationOfVerbGroupCluster {
	VerbGroup vg;

	String pivotVerb;
	String pivotHead;
	//	Set<String> pivotHeads = new HashSet<String>();

	List<String> relations = new ArrayList<String>();
	List<Boolean> relationclusters;

	public RelationOfVerbGroupCluster(VerbGroup vg) {
		this.vg = vg;
		this.pivotVerb = vg.pivotVerb;

		Set<String> temp = new HashSet<String>();
		for (Eec eec : vg.eecs) {
			for (Tuple t : eec.getTuples()) {
				String r = t.getRel();
				if (r.equals(pivotVerb)) {
					pivotHead = t.getRelHead();
				} else {
					temp.add(t.getRel());
				}
			}
		}
		relations.addAll(temp);
	}

	public List<String[]> getToLabel() {
		List<String[]> tolabel = new ArrayList<String[]>();
		for (Eec eec : vg.eecs) {
			String arg1 = eec.getArg1();
			String arg2 = eec.getArg2();
			Tuple pivotVerbTuple = null;
			for (Tuple t : eec.getTuples()) {
				if (t.getRel().equals(this.pivotVerb)) {
					pivotVerbTuple = t;
				}
			}
			if (pivotVerbTuple != null) {
				for (Tuple t : eec.getTuples()) {
					tolabel.add(DW.tow(
							t.getRel(),
							pivotVerb,
							arg1,
							arg2,
							t.getRelHead(),
							pivotHead,
							StringUtil.join(t.tkn, " "),
							StringUtil.join(pivotVerbTuple.tkn, " ")));
				}
			}
		}
		return tolabel;
	}
}
