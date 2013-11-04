package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ConnectedComponent {
	BiMap<String, Integer> nodes = HashBiMap.create();
	List<int[]> edges_raw = new ArrayList<int[]>();

	public ConnectedComponent() {

	}

	public int addNode(String n) {
		if (!nodes.containsKey(n)) {
			nodes.put(n, nodes.size());
		}
		int nid = nodes.get(n);
		return nid;
	}

	public void addEdge(String n1, String n2) {
		int nid1 = addNode(n1);
		int nid2 = addNode(n2);
		edges_raw.add(new int[] { nid1, nid2 });
	}

	public HashMap<String, Integer> cc() {
		HashMap<String, Integer> ccret = new HashMap<String, Integer>();
		int N = nodes.size();
		SortedSet<Integer> edges[] = new SortedSet[N];
		for (int i = 0; i < N; i++) {
			edges[i] = new TreeSet<Integer>();
		}
		for (int e[] : edges_raw) {
			edges[e[0]].add(e[1]);
			edges[e[1]].add(e[0]);
		}
		//		boolean[] marked = new boolean[N];
		int[] cc = new int[N];
		int CID = 1;
		for (int i = 0; i < N; i++) {
			//bfs from node[i]
			if (cc[i] > 0) {
				continue;
			}
			Queue<Integer> q = new LinkedList<Integer>();
			q.add(i);
			do {
				int top = q.poll();
				cc[top] = CID;
				for (int a : edges[top]) {
					if (cc[a] == 0)
						q.add(a);
				}
			} while (q.size() > 0);
			CID++;
		}
		for (int i = 0; i < N; i++) {
			String nstr = nodes.inverse().get(i);
			int cid = cc[i];
			ccret.put(nstr, cid);
		}
		return ccret;
	}

}
