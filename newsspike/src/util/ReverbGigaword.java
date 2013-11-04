package util;

import java.util.Date;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;

import GetInDomainText.ReVerbExtractorWrap;
import GetInDomainText.ReVerbResult;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import edu.washington.cs.knowitall.commonlib.Range;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedArgumentExtraction;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedExtraction;
import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer;
import edu.washington.cs.knowitall.normalization.HeadNounExtractor;
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction;
import edu.washington.cs.knowitall.normalization.NormalizedField;
import edu.washington.cs.knowitall.util.Morpha;

import javatools.administrative.D;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;
import javatools.stanford.ParsedSentence;
import javatools.stanford.ParsedText;
import javatools.string.StringUtil;

public class ReverbGigaword {
	public static void convet2ascii(String input, String output) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input), "utf-8"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "ascii"));
		String line;
		//		String[] l;
		int k = 0;
		while ((line = br.readLine()) != null) {
			k++;
			if (k > 10000)
				break;
			bw.write(line + "\n");
		}
		br.close();
		bw.close();
	}

	public static void test() throws IOException {
		Gson gson = new Gson();
		String a = gson.toJson("''");

		{
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("temp"), "utf-8"));
			bw.write(a);
			bw.close();
		}
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("temp"), "utf-8"));
			String s = br.readLine();
			String b = gson.fromJson(s, String.class);
			D.p(b);
			br.close();
		}
		String b = gson.fromJson(a, String.class);
	}

	public static void getreverbfromjson_one(ReVerbExtractorWrap rvew,
			HeadNounExtractor headnoun_extractor,
			BinaryExtractionNormalizer normalizer,
			Gson gson,
			String[] l_input, List<String[]> w1outs, List<String[]> w2outs) {
		String[] l = l_input;
		int sectionId = Integer.parseInt(l[0]);
		ParsedText ps = gson.fromJson(l[1], ParsedText.class);
		for (ParsedSentence sentence : ps.parsedsentence) {
			ReVerbResult rvr = rvew.parse(sentence.tkn, sentence.pos);
			ChunkedSentence sent = rvr.chunk_sent;
			{
				// #####NP chunks
				for (Range r : sent.getNpChunkRanges()) {
					ChunkedSentence sub = sent.getSubSequence(r);

					String[] w1 = DW.tow(sentence.sectId, sentence.sentId, r.getStart(),
							r.getEnd(), "NP", sub.getTokensAsString(),
							sub.getTokenNormAsString());
					w1outs.add(w1);
				}
				// #####VP chunks
				for (Range r : sent.getVpChunkRanges()) {
					ChunkedSentence sub = sent.getSubSequence(r);
					String[] w1 = DW.tow(sentence.sectId, sentence.sentId, r.getStart(),
							r.getEnd(), "VP", sub.getTokensAsString(),
							sub.getTokenNormAsString());
					w1outs.add(w1);
				}
			}
			{
				for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
					NormalizedBinaryExtraction ne = normalizer
							.normalize(extr);
					int arg1s = extr.getArgument1().getRange().getStart();
					int arg1e = extr.getArgument1().getRange().getEnd();
					int arg2s = extr.getArgument2().getRange().getStart();
					int arg2e = extr.getArgument2().getRange().getEnd();
					int rels = extr.getRelation().getRange().getStart();
					int rele = extr.getRelation().getRange().getEnd();
					int[] arg1h = new int[1];
					int[] arg2h = new int[1];

					ChunkedArgumentExtraction arg1field = extr.getArgument1();
					ChunkedArgumentExtraction arg2field = extr.getArgument2();
					ChunkedExtraction relfield = extr.getRelation();
					NormalizedField arg1headfield = headnoun_extractor.normalizeField(ne.getArgument1(), arg1h);
					NormalizedField arg2headfield = headnoun_extractor.normalizeField(ne.getArgument2(), arg2h);
					arg1h[0] += arg1s;
					arg2h[0] += arg2s;

					String[] w2 = DW.tow(sentence.sectId, sentence.sentId,
							arg1s, arg1e,
							rels, rele,
							arg2s, arg2e,
							arg1h[0], arg2h[0],
							arg1field.toString(), relfield.toString(), arg2field.toString(),
							arg1headfield.toString(), ne.getRelationNorm(), arg2headfield.toString()
							);
					w2outs.add(w2);
				}
			}
		}
	}

	public static void getreverbfromjson(String input, String output) throws IOException {
		DW dw1 = new DW(output + ".rvphrase");
		DW dw2 = new DW(output + ".rvrel");
		Gson gson = new Gson();
		ReVerbExtractorWrap rvew = new ReVerbExtractorWrap();
		Morpha lexer = new Morpha(new ByteArrayInputStream("".getBytes()));
		HeadNounExtractor headnoun_extractor = new HeadNounExtractor();
		BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
		//		String[] l;
		String line;
		System.err.println("load complete");
		int numParsed = 0;
		long startTime = (new Date()).getTime();
		while ((line = br.readLine()) != null) {
			String[] l = line.split("\t");
			int sectionId = Integer.parseInt(l[0]);
			if (sectionId > 10000) {
				//				break;
			}
			ParsedText ps = gson.fromJson(l[1], ParsedText.class);

			for (ParsedSentence sentence : ps.parsedsentence) {
				ReVerbResult rvr = rvew.parse(sentence.tkn, sentence.pos);
				ChunkedSentence sent = rvr.chunk_sent;
				{
					// #####NP chunks
					for (Range r : sent.getNpChunkRanges()) {
						ChunkedSentence sub = sent.getSubSequence(r);
						dw1.write(sentence.sectId, sentence.sentId, r.getStart(),
								r.getEnd(), "NP", sub.getTokensAsString(),
								sub.getTokenNormAsString());
					}
					// #####VP chunks
					for (Range r : sent.getVpChunkRanges()) {
						ChunkedSentence sub = sent.getSubSequence(r);
						dw1.write(sentence.sectId, sentence.sentId, r.getStart(),
								r.getEnd(), "VP", sub.getTokensAsString(),
								sub.getTokenNormAsString());
					}
				}
				{
					for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
						NormalizedBinaryExtraction ne = normalizer
								.normalize(extr);
						int arg1s = extr.getArgument1().getRange().getStart();
						int arg1e = extr.getArgument1().getRange().getEnd();
						int arg2s = extr.getArgument2().getRange().getStart();
						int arg2e = extr.getArgument2().getRange().getEnd();
						int rels = extr.getRelation().getRange().getStart();
						int rele = extr.getRelation().getRange().getEnd();
						int[] arg1h = new int[1];
						int[] arg2h = new int[1];

						ChunkedArgumentExtraction arg1field = extr.getArgument1();
						ChunkedArgumentExtraction arg2field = extr.getArgument2();
						ChunkedExtraction relfield = extr.getRelation();
						NormalizedField arg1headfield = headnoun_extractor.normalizeField(ne.getArgument1(), arg1h);
						NormalizedField arg2headfield = headnoun_extractor.normalizeField(ne.getArgument2(), arg2h);
						arg1h[0] += arg1s;
						arg2h[0] += arg2s;

						dw2.write(sentence.sectId, sentence.sentId,
								arg1s, arg1e,
								rels, rele,
								arg2s, arg2e,
								arg1h[0], arg2h[0],
								arg1field.toString(), relfield.toString(), arg2field.toString(),
								arg1headfield.toString(), ne.getRelationNorm(), arg2headfield.toString()
								);
						//						dw2.write(sentence.sectId, sentence.sentId, extr.getArgument1()
						//								.getRange().getStart(), extr.getArgument1()
						//								.getRange().getEnd(), extr.getRelation()
						//								.getRange().getStart(), extr.getRelation()
						//								.getRange().getEnd(), extr.getArgument2()
						//								.getRange().getStart(), extr.getArgument2()
						//								.getRange().getEnd(), extr.getArgument1(), extr
						//								.getRelation(), extr.getArgument2(),
						//								//								ne.getArgument1Norm(),
						//								//								ne.getRelationNorm(),
						//								//								ne.getArgument2Norm()
						//								headnoun_extractor.normalizeField(ne.getArgument1()),
						//								ne.getRelationNorm(),
						//									headnoun_extractor.normalizeField(ne
						//																		.getArgument2())
						//								);
					}
				}
			}
			{
				numParsed++;
				long currentTime = (new Date()).getTime();
				if (numParsed % 100 == 0) {
					D.p("Avg speed", numParsed, (currentTime - startTime) * 1.0 / numParsed);
				}
			}
		}
		dw1.close();
		dw2.close();
		br.close();
	}

	public static void main(String[] args) throws IOException {
		//		test();
		//		convet2ascii(args[0], args[1]);
		getreverbfromjson(args[0], args[1]);

	}
}
