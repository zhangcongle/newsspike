package util;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javatools.administrative.D;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class FreebaseSearch {

	public static Properties properties = new Properties();

	String dir = "";
	Gson gson = new Gson();
	HashMap<String, FreebaseEntitySearchResult> entitystr2results = new HashMap<String, FreebaseEntitySearchResult>();

	public FreebaseSearch(String dir) {
		this.dir = dir;
		if ((new File(this.dir)).exists()) {
			(new File(this.dir)).mkdir();
		}
		String[] files = (new File(this.dir)).list();
		for (String f : files) {
			DR dr = new DR(this.dir + "/" + f);
			String[] l;
			while ((l = dr.read()) != null) {
				String entitystr = l[0];
				FreebaseEntitySearchResult searchresult = gson.fromJson(l[1],
						FreebaseEntitySearchResult.class);
				entitystr2results.put(entitystr, searchresult);
			}
			dr.close();
		}

	}

	public void searchListEntities(String input, String output) {
		DR dr = new DR(input);
		DW dwdb = new DW(dir + "/" + (new Date()).getTime());
		DW dw = new DW(output);
		String[] l;
		while ((l = dr.read()) != null) {
			String str = l[0];
			if (entitystr2results.containsKey(str)) {
				FreebaseEntitySearchResult ret = entitystr2results.get(str);
				dw.write(str, gson.toJson(ret));
			} else {
				FreebaseEntitySearchResult ret = search(str);
				dw.write(str, gson.toJson(ret));
				dwdb.write(str, gson.toJson(ret));
			}
		}
		dw.close();
		dwdb.close();
		dr.close();
	}

	public FreebaseEntitySearchResult search(String str) {

		FreebaseEntitySearchResult ret = new FreebaseEntitySearchResult();
		//		List<FreebaseEntity> listFreebaseEntity = new ArrayList<FreebaseEntity>();
		try {
			//			properties.load(new FileInputStream("freebase.properties"));
			HttpTransport httpTransport = new NetHttpTransport();
			HttpRequestFactory requestFactory = httpTransport
					.createRequestFactory();
			JSONParser parser = new JSONParser();
			GenericUrl url = new GenericUrl(
					"https://www.googleapis.com/freebase/v1/search");
			url.put("query", str);
			//			url.put("filter",
			//					"(all type:/music/artist created:\"The Lady Killer\")");
			//			url.put("limit", "10");
			url.put("indent", "true");
			url.put("key", "AIzaSyC-7EH7YlY5VDGKtww7vzVpL-wmqi82y0Q");
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			JSONObject response = (JSONObject) parser.parse(httpResponse
					.parseAsString());
			JSONArray results = (JSONArray) response.get("result");
			for (Object result : results) {
				//				System.out.println(result.toString());
				FreebaseEntity entity = gson.fromJson(result.toString(),
						FreebaseEntity.class);
				if (entity.mid != null && entity.mid.startsWith("/m")) {
					ret.list.add(entity);
				}
				//				if (entity.notable != null)
				//					D.p(entity.id, entity.mid, entity.notable.id,
				//							entity.notable.name, entity.score);
			}
			//			for (Object result : results) {
			//				System.out.println(JsonPath.read(result, "$.name").toString());
			//			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.err
				.println("Freebase Api search " + str + " " + ret.list.size());
		return ret;
	}

	public static void main(String[] args) {
		String dir = "/projects/pardosa/data17/clzhang/re/";
		FreebaseSearch fs = new FreebaseSearch(dir + "/fbsearch");
		fs.searchListEntities(dir + "/exp2/rc2.arguments", dir
				+ "/exp2/rc2.arguments.fbsearch");
	}

	public static void main_test(String[] args) {
		D.p("bac");
		Gson gson = new Gson();
		try {
			//			properties.load(new FileInputStream("freebase.properties"));
			HttpTransport httpTransport = new NetHttpTransport();
			HttpRequestFactory requestFactory = httpTransport
					.createRequestFactory();
			JSONParser parser = new JSONParser();
			GenericUrl url = new GenericUrl(
					"https://www.googleapis.com/freebase/v1/search");
			url.put("query", "Republicans");
			//			url.put("filter",
			//					"(all type:/music/artist created:\"The Lady Killer\")");
			//			url.put("limit", "10");
			url.put("indent", "true");
			url.put("key", "AIzaSyC-7EH7YlY5VDGKtww7vzVpL-wmqi82y0Q");
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			JSONObject response = (JSONObject) parser.parse(httpResponse
					.parseAsString());
			JSONArray results = (JSONArray) response.get("result");
			for (Object result : results) {
				//				System.out.println(result.toString());
				FreebaseEntity entity = gson.fromJson(result.toString(),
						FreebaseEntity.class);
				//				if (entity.notable != null)
				//					D.p(entity.id, entity.mid, entity.notable.id,
				//							entity.notable.name, entity.score);
			}
			//			for (Object result : results) {
			//				System.out.println(JsonPath.read(result, "$.name").toString());
			//			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}