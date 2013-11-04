package util;

import java.util.ArrayList;
import java.util.List;

public class FreebaseEntitySearchResult {
	List<FreebaseEntity> list = new ArrayList<FreebaseEntity>();

	public String getMostLikelyNotableType() {
		StringBuilder sb = new StringBuilder();
		int k = 0;
		for (FreebaseEntity e : list) {
			if (e.notable != null) {
				sb.append(e.notable.name.replaceAll(" ", "_") + " ");
				if (k++ > 2)
					break;
			}
		}
		return sb.toString();
	}
}
