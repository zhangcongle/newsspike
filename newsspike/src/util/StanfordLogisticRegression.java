package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javatools.administrative.D;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;
import javatools.string.StringUtil;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LogisticClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.ErasureUtils;
import edu.stanford.nlp.util.Index;

public class StanfordLogisticRegression {

	String tmpDir;
	List<String> property;
	List<String[]> trains;
	ColumnDataClassifier cdc;
	Classifier<String, String> cl;

	public StanfordLogisticRegression(String tmpDir) {
		this.tmpDir = tmpDir;
		property = new ArrayList<String>();
		trains = new ArrayList<String[]>();
		if (!(new File(tmpDir)).exists()) {
			(new File(tmpDir)).mkdir();
		}
	}

	public void addProperty(String p) {
		property.add(p);
	}

	public void addProperties(String[] ps) {
		for (String p : ps)
			property.add(p);
	}

	public void addTrainingExample(String[] trainLine) {
		trains.add(trainLine);
	}

	//HashMap<String, HashMap<String, Double>>
	public HashMap<String, HashMap<String, Double>> trainModel() {
		HashMap<String, HashMap<String, Double>> ftr_cls_weight = new HashMap<String, HashMap<String, Double>>();
		long prefix = (new Date()).getTime();
		String file_property = tmpDir + "/" + prefix + ".prop";
		String file_train = tmpDir + "/" + prefix + ".train";
		{
			DW dw = new DW(file_property);
			for (String p : property) {
				dw.write(p);
			}
			dw.close();
		}
		{
			DW dw = new DW(file_train);
			for (String[] l : trains) {
				dw.write(l);
			}
			dw.close();
		}
		cdc = new ColumnDataClassifier(file_property);
		cl = cdc.makeClassifier(cdc.readTrainingExamples(file_train));
		LinearClassifier<String, String> lc = (LinearClassifier) cl;
		Index<String> labelindex = lc.labelIndex();
		Index<String> ftrs = lc.featureIndex();
		double[][] weights = lc.weights();
		for (int i = 0; i < ftrs.size(); i++) {
			String ftr = ftrs.get(i);
			double[] w = weights[i];
			HashMap<String, Double> cls_weight = new HashMap<String, Double>();
			for (int j = 0; j < w.length; j++) {
				String label = labelindex.get(j);
				cls_weight.put(label, w[j]);
			}
			ftr_cls_weight.put(ftr, cls_weight);
		}
		return ftr_cls_weight;
	}

	public String inference(String[] test, HashMap<String, Double> scores) {
		Datum<String, String> d = cdc.makeDatumFromLine(StringUtil.join(test, "\t"), 0);
		String cls = cl.classOf(d);
		Counter<String> c = cl.scoresOf(d);
		for (Entry<String, Double> e : c.entrySet()) {
			scores.put(e.getKey(), e.getValue());
		}
		return cls;
	}

	public String inference(String[] test) {
		Datum<String, String> d = cdc.makeDatumFromLine(StringUtil.join(test, "\t"), 0);
		String cls = cl.classOf(d);
		return cls;
	}

	public static void main(String[] args) {
		StanfordLogisticRegression slr = new StanfordLogisticRegression("tmp");
		{
			DR dr = new DR("examples/cheeseDisease.train");
			String[] l;
			while ((l = dr.read()) != null) {
				slr.addTrainingExample(l);
			}
			dr.close();
		}
		{
			slr.addProperty("useClassFeature=true");
			slr.addProperty("1.useNGrams=true");
			slr.addProperty("1.usePrefixSuffixNGrams=true");
			slr.addProperty("1.maxNGramLeng=4");
			slr.addProperty("1.minNGramLeng=1");
			slr.addProperty("1.binnedLengths=10,20,30");
			slr.addProperties(new String[] { "intern=true",
					"sigma=3",
					"useQN=true",
					"QNsize=15",
					"tolerance=1e-4", });
			slr.trainModel();
		}
		{
			int errors = 0;
			DR dr = new DR("examples/cheeseDisease.test");
			String[] l;
			while ((l = dr.read()) != null) {
				String pred = slr.inference(l);
				String gold = l[0];

				if (!pred.equals(gold)) {
					D.p(gold, pred);
					errors++;
				}
			}
			dr.close();
			System.err.println("Errors\t" + errors);
		}
	}

	public static void main3(String[] args) {
		ColumnDataClassifier cdc = new ColumnDataClassifier("examples/cheese2007.prop");

		Classifier<String, String> cl =
				cdc.makeClassifier(cdc.readTrainingExamples("examples/cheeseDisease.train"));
		//		LinearClassifier lcl = (LinearClassifier) cl;
		//		double[][] weights = lcl.weights();
		//		Index index = lcl.featureIndex();
		//		
		//		for (int k = 0; k < index.size(); k++) {
		//			Object o = index.get(k);
		//			System.err.println(o);
		//			D.p(weights[k]);
		//		}

		for (String line : ObjectBank.getLineIterator("examples/cheeseDisease.test")) {
			Datum<String, String> d = cdc.makeDatumFromLine(line, 0);
			String cls = cl.classOf(d);
			Counter<String> c = cl.scoresOf(d);
			double score = c.getCount(cls);
			System.out.println(line + "  ==>  " + cls + "\t" + score);
		}
	}

	public static void main2(String[] args) {
		ColumnDataClassifier cdc = new ColumnDataClassifier("examples/iris.prop");

		Classifier<String, String> cl =
				cdc.makeClassifier(cdc.readTrainingExamples("examples/iris.train"));
		for (String line : ObjectBank.getLineIterator("examples/iris.test")) {
			Datum<String, String> d = cdc.makeDatumFromLine(line, 0);
			System.out.println(line + "  ==>  " + cl.classOf(d));
		}
	}

	public static void demonstrateSerialization(String[] args)
			throws IOException, ClassNotFoundException {
		ColumnDataClassifier cdc = new ColumnDataClassifier("examples/cheese2007.prop");
		Classifier<String, String> cl =
				cdc.makeClassifier(cdc.readTrainingExamples("examples/cheeseDisease.train"));

		// Exhibit serialization and deserialization working. Serialized to bytes in memory for simplicity
		System.out.println();
		System.out.println();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(cl);
		oos.close();
		byte[] object = baos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(object);
		ObjectInputStream ois = new ObjectInputStream(bais);
		LinearClassifier<String, String> lc = ErasureUtils.uncheckedCast(ois.readObject());
		ois.close();
		ColumnDataClassifier cdc2 = new ColumnDataClassifier("examples/cheese2007.prop");

		for (String line : ObjectBank.getLineIterator("examples/cheeseDisease.test")) {
			Datum<String, String> d = cdc.makeDatumFromLine(line, 0);
			Datum<String, String> d2 = cdc2.makeDatumFromLine(line, 0);
			System.out.println(line + "  =origi=>  " + cl.classOf(d));
			System.out.println(line + "  =deser=>  " + lc.classOf(d2));
		}
	}
}