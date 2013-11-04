package util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import GetInDomainText.ReVerbExtractorWrap;

import com.google.gson.Gson;

import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer;
import edu.washington.cs.knowitall.normalization.HeadNounExtractor;
import edu.washington.cs.knowitall.util.Morpha;

import javatools.administrative.D;
import javatools.filehandlers.DR;
import javatools.filehandlers.DW;

class InData {
	final String[] l;
	final boolean eof;

	public InData(String[] l) {
		this.l = l;
		eof = false;
	}

	public InData(boolean eof) {
		this.eof = eof;
		l = null;
	}
}

class OutData {
	int pid;
	List<String[]> w1;
	List<String[]> w2;
	boolean eof;

	public OutData(int pid, List<String[]> w1, List<String[]> w2) {
		this.pid = pid;
		this.w1 = w1;
		this.w2 = w2;
	}

	public OutData(boolean eof) {
		this.eof = eof;
	}
}

public class ReverbGigawordMultithread {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String input = args[0];
		String output = args[1];
		int NUMP = Integer.parseInt(args[2]);
		int MIN_QUEUE_SIZE = 100;
		int BLOCK_SIZE = 100;
		DR dr = new DR(input);
		boolean eof = false;
		LinkedBlockingQueue<InData> queues[] = new LinkedBlockingQueue[NUMP];
		LinkedBlockingQueue<OutData> outQueue = new LinkedBlockingQueue<OutData>();
		Thread tout = new Thread(new Writer(outQueue, NUMP, output));
		tout.start();
		for (int i = 0; i < NUMP; i++) {
			queues[i] = new LinkedBlockingQueue<InData>();
			Thread t1 = new Thread(new Worker(i, queues[i], outQueue));
			t1.start();
		}
		String[] l;
		try {
			EOF_INPUT_STREAM: while (!eof) {
				int total = 0;
				for (int k = 0; k < NUMP; k++) {
					if (queues[k].size() < MIN_QUEUE_SIZE) {
						for (int i = 0; i < BLOCK_SIZE; i++) {
							if (!eof && (l = dr.read()) != null) {
								if (queues[k].offer(new InData(l)))
									total++;
								else {
									//TODO error processing
								}
							} else {
								eof = true;
								break EOF_INPUT_STREAM;
							}
						}
					}
				}
				if (total == 0) {
					Thread.sleep(1000);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//write eof to queues
		for (int k = 0; k < NUMP; k++) {
			queues[k].offer(new InData(true));
		}
		dr.close();
	}

}

class Worker implements Runnable {
	final LinkedBlockingQueue<InData> queue;
	final LinkedBlockingQueue<OutData> out;
	int process_id;
	final ReVerbExtractorWrap rvew;
	final HeadNounExtractor headnoun_extractor;
	final BinaryExtractionNormalizer normalizer;
	Gson gson;

	public Worker(int process_id, LinkedBlockingQueue<InData> queue, LinkedBlockingQueue<OutData> out) {
		gson = new Gson();
		rvew = new ReVerbExtractorWrap();
		headnoun_extractor = new HeadNounExtractor();
		normalizer = new BinaryExtractionNormalizer();
		gson = new Gson();

		this.process_id = process_id;
		this.queue = queue;
		this.out = out;
		System.err.println("Worker " + process_id + ": starts");
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		InData d;
		try {
			while ((d = queue.take()) != null) {
				if (d.eof) {
					out.offer(new OutData(true));
					break;
				} else {
					String[] l = d.l;
					int sectionId = Integer.parseInt(l[0]);
					List<String[]> w1 = new ArrayList<String[]>();
					List<String[]> w2 = new ArrayList<String[]>();
					try {
						ReverbGigaword.getreverbfromjson_one(rvew, headnoun_extractor, normalizer, gson, l, w1, w2);
					} catch (Exception e) {
						e.printStackTrace();
					}
					out.offer(new OutData(process_id, w1, w2));
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}

class Writer implements Runnable {

	final LinkedBlockingQueue<OutData> queue;
	final int NUMP;
	int num_end_processor;
	final DW dw1;
	final DW dw2;

	public Writer(LinkedBlockingQueue<OutData> queue, int NUMP, String output) {
		this.NUMP = NUMP;
		this.queue = queue;
		dw1 = new DW(output + ".rvphrase");
		dw2 = new DW(output + ".rvrel");
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		OutData d;
		int numSections = 0;
		long startTime = (new Date()).getTime();
		try {
			while ((d = queue.poll(100, TimeUnit.SECONDS)) != null) {
				//			while ((d = queue.take()) != null) {
				if (d.eof) {
					num_end_processor++;
				} else {
					for (String[] w : d.w1) {
						dw1.write(w);
					}
					for (String[] w : d.w2) {
						dw2.write(w);
					}
					{//speed
						numSections++;
						long spend = (new Date()).getTime() - startTime;
						if (numSections % 1000 == 0)
							D.p("Avg speed", spend * 1.0 / numSections);
					}
				}
				if (num_end_processor == NUMP) {
					dw1.close();
					dw2.close();
					break;
				}
			}
			try {
				//in case the system is not closed
				dw1.close();
				dw2.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
