package LDAModel;

import PreProcess.Corpus;
import FollowProcess.FollowWork;
//import weka.clusterers.SimpleKMeans;

/*
 *  Created on March 15 14:02:14 2015
 * author: Xiangfu Song 
 * email : bintasong@gmail.com
 * 
 * 
 * */
public class LDASampling{
	public static int topicNumOfAux = 25;
	public static int topicNumOfTar = 10;
	public static int iteration = 200;
	public static double beta = 0.01;
	public static double gama_small = 0.2;
	public static double gama_big = 0.5;
	public static int auxFlag = 0;
	public static int tarFlag = 1;
	public static void main(String[] argv){
		
		Corpus corpus =new Corpus(topicNumOfAux,topicNumOfAux,50.0/(topicNumOfAux+topicNumOfTar),beta,gama_big,gama_small);
		System.out.println("add Auxiliary document to memory begin ...");
		corpus.addDoc("C:/Users/binta/Desktop/lda_java/gama-DLDA/Data/Auxiliary",auxFlag);
		System.out.println("add Auxiliary document to memory done.");
		corpus.MA = corpus.M;//记录当前的辅助文档数目！
		System.out.println("add Target document to memory begin ...");
		corpus.addDoc("C:/Users/binta/Desktop/lda_java/gama-DLDA/Data/Target",tarFlag);
		System.out.println("add Target document to memory done.");
		System.out.printf("the number of words: %d\n",corpus.V);
		
		System.out.println("begin init ...");
		corpus.init();
		System.out.println("init done");
		System.out.println("iteration...");
		corpus.sampling(iteration);
		System.out.println("iteration done.");
		FollowWork fw = new FollowWork(corpus,"C:/Users/binta/Desktop/lda_java/gama-DLDA/Data/LDAResults/document-topic.txt","C:/Users/binta/Desktop/lda_java/gama-DLDA/Data/LDAResults/topic-word.txt","C:/Users/binta/Desktop/lda_java/gama-DLDA/Data/LDAResults/theta.txt");
		fw.sortMKandSave();
		fw.sortKWandSave();
		fw.writeTheta();
		System.out.println("gamma-dlda done");
	}
}
