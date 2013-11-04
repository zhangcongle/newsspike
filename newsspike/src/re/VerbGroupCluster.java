package re;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class VerbGroupCluster extends VerbGroup {
	static Gson gson = new Gson();
	List<String> clustering = new ArrayList<String>();

	public static VerbGroupCluster fromJsonVerbGroup(String json_verbgroup) {
		VerbGroupCluster vgc = gson.fromJson(json_verbgroup,
				VerbGroupCluster.class);
		return vgc;
	}

	public boolean inSameCluster(int i, int j) {
		boolean ret = false;
		if (clustering.get(i).equals("NA") || clustering.get(j).equals("NA")) {
			ret = false;
		} else if (clustering.get(i).equals(clustering.get(j))) {
			ret = true;
		}
		return ret;
	}
}
