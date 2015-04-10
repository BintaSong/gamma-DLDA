package PreProcess;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
//import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

/* 
 * Created on March 12 14:02:14 2015
 * author: Xiangfu Song 
 * email : bintasong@gmail.com
 * 
 * */

public class Corpus{
	public static ArrayList<String> vacabulary;//文本集字典列表
	public ArrayList<Document> docs;//文档集合
	public static HashMap<String,Integer> wordtoindex;//词语到索引的hash表
	public static ArrayList<String> noiselist;//噪声单词列表
	
	public Pattern pattern ; 
	
	public double alpha ; //通常情况是 (50/K) 
	public double beta ;//通常是 0.1
	public int V;//字典词汇量
	public int MA;//文档数量
	public int M;//代表辅助文档的数目
	public int K;//主题数目
	public int KA;//辅助主题数目
	public int KT;//目标主题数目
	public double [][] gama_tran;
	public int [][] z;//对每个词语的主题标注,z表示主题标注，
    //public int [][] x; //x表示词语来源（aux or tar）!
	public int [][] nmk;//每行对应某个文档上主题的个数的分布 M*K
	public int [][] nkw;//每行对应某个主题上词语的个数分布. K*V
	public int [] nmkSum;//文档-主题 每行的求和
	public int [] nkwSum;//主题-词语 每行的总和
    public int [][] nwx;//V*2,每行代表单词来源于aux和tar中的单词数目
	//public double[][] theta;//对应 词语-主题 分布 ，V*K,在我的程序中这个不重要，所以不需要也可以
	public double[][] phi; //重要的是 文档-主题 分布，M*K
	
	public Corpus(int topicNumOfAux,int topicNumOfTar,double alpha,double beta,double gama_big,double gama_small){
		vacabulary = new ArrayList<String>();
		docs = new ArrayList<Document>();
		wordtoindex = new HashMap<String,Integer>();
		noiselist = new ArrayList<String>();
		this.alpha = alpha;
		this.beta = beta;
		gama_tran = new double[2][2];
		gama_tran[0][0] = gama_tran[1][1] = gama_big;
		gama_tran[0][1] = gama_tran[1][0] = gama_small;
		
		KA = topicNumOfAux;
		KT = topicNumOfTar;
		K = KA + KT;
		V = 0;
		M = 0;
		pattern = Pattern.compile(".*[0-9]{1,}.*");
		addNoise("./Data/StopWords/stopword-ch.txt");
	}
	
	public void addNoise(String noisefilepath){
		//从nlist向noiselist添加噪声词语
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new FileReader(new File(noisefilepath))); 
			String line = null;
			while( (line = reader.readLine()) != null){
				//我们的噪声文件每行存储一个词语
				line = line.trim();
				noiselist.add(line);
			}
		}catch(FileNotFoundException e){
			System.out.println("噪声文件没有找到！");
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("文件IO错误！");
			e.printStackTrace();
		}finally{
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					System.out.println("关闭噪声文件错误！");
					e.printStackTrace();
				}
			}
		}
	}
	
	public List<Term> splitWords(String words){
		//返回分词后的List<Term>列表
		List<Term> termlist= ToAnalysis.parse(words);
		removeNoise(termlist);
		return termlist;
	}
	
	public void removeNoise(List<Term> termlist){
		//将分词后的List<Term>移除掉noise
		for(int i = 0;i < termlist.size();i++ ){
			if(noiselist.contains(termlist.get(i).getName())||termlist.get(i).getName().length() < 2||pattern.matcher(termlist.get(i).getName()).matches()){
				termlist.remove(i);
				i--;
			}
		}
	}	
	
	public void addDoc(String docspath,int AorT){
		for(File docFile : new File(docspath).listFiles()){
			if(docFile.isDirectory()){
				addDoc(docFile.getAbsolutePath(),AorT);
			}
			else{
				BufferedReader reader = null;
				String words = "";
				String line = "";
				try{
					reader = new BufferedReader(new FileReader(docFile)); 
					while( (line = reader.readLine()) != null ){
						//我们的文件每行存储一个词语
						words = words + line.trim();
					}
				}catch(IOException e){
					System.out.println("读取文档错误");
					e.printStackTrace();				
				}finally{
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e) {
							System.out.println("关闭文档错误!");
							e.printStackTrace();
						}
					}
				}
				String[] tmpArray =  words.trim().split(" ");
				Document doc = new Document(docFile.getName(),tmpArray,AorT);//tmpArray需要进行无效词清理
				docs.add(doc);
				M += 1;
			}	
		}
	}
	
	public void init(){
		nmk = new int [M][K];
		nkw = new int[K][V];
		nmkSum = new int[M];
		nkwSum = new int[K];
		nwx = new int[V][2];
		phi = new double[M][K];
		
		z = new int[M][];
		//随机主题赋值
		for(int i = 0;i < M;i++ ){
			int N = docs.get(i).doc_len;
			z[i] = new int[N];
			for (int j = 0;j < N;j++){
				int randtopic = (int)(Math.random()*K);
				z[i][j] = randtopic;
				
				if(z[i][j] < KA){
					nwx[docs.get(i).docwords[j]][0] += 1;
				}
				else{
					nwx[docs.get(i).docwords[j]][1] += 1;
				}
				
				nmk[i][randtopic] += 1;
				nkw[randtopic][docs.get(i).docwords[j]] += 1;
				nkwSum[randtopic] += 1;
			}
			nmkSum[i] += N;
		}	
		/*for(int k = 0;k < K;k++){
			System.out.print(nkwSum[k]+"--> ");
			int count = 0;
			for(int v = 0;v < V;v++){
				System.out.print(nkw[k][v]+" ");
				count += nkw[k][v];
			}
			if(count != nkwSum[k])
				System.out.println("\n"+k+"fuck");
			else{
				System.out.println("\n"+"good");
			}
			System.out.println(this.getWordByIndex(1));
		}*/
		/*
		for(int m = 0;m < M;m++){
			//System.out.print(nkwSum[k]+"--> ");
			int count = 0;
			for(int w = 0;w < docs.get(m).docwords.length;w++){
				System.out.print(this.getWordByIndex(docs.get(m).docwords[w])+" ");
			}
			System.out.println();
		}
		*/
	}

	public int choose(double p[],int topicnum){
		double a = Math.random();
		int k = 0;
		for( k = 0;k < p.length;k++ ){
			a -= p[k];
			if(a <= 0){
				break;
			}
		}
		return k;
	}
	
	public int newTopic(int m,int n){
		int oldtopic = z[m][n];
		nmk[m][oldtopic] -= 1;
		nkw[oldtopic][docs.get(m).docwords[n]] -= 1;
		nmkSum[m] -= 1;
		nkwSum[oldtopic] -= 1;
		if(oldtopic < KA){
			nwx[docs.get(m).docwords[n]][0] -= 1;
		}
		else{
			nwx[docs.get(m).docwords[n]][1] -= 1;
		}		
		double p[] = new double[K];
		double sum = 0.0;
		for(int k = 0;k < K;k++){
			if(k < KA){
				p[k] = (nwx[docs.get(m).docwords[n]][0] + gama_tran[docs.get(m).AorT][0])*(nkw[k][docs.get(m).docwords[n]] + beta) / (nkwSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
			}
			else{
				p[k] = (nwx[docs.get(m).docwords[n]][1] + gama_tran[docs.get(m).AorT][1])*(nkw[k][docs.get(m).docwords[n]] + beta) / (nkwSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
			}
			sum += p[k];
		}
		//System.out.printf("before sum of p[k] is:%f\n",sum);
		for(int k =0;k < K;k++){
			p[k] = p[k]/sum;
		}
		sum = 0;
		for(int k = 0;k < K;k++){
			sum += p[k];
		}
		//System.out.printf("after sum of p[k] is:%f\n",sum);
		int newtopic = choose(p,K);
		nmk[m][newtopic] += 1;
		nkw[newtopic][docs.get(m).docwords[n]] += 1;
		nmkSum[m] += 1;
		nkwSum[newtopic] += 1;
 		
		if(newtopic < KA){
			nwx[docs.get(m).docwords[n]][0] += 1;
		}
		else{
			nwx[docs.get(m).docwords[n]][1] += 1;
		}		
		return newtopic;
	}
	
	public void sampling(int iteration){
		for(int i = 0;i < iteration;i++){
			//System.out.println("iteration # " + i );
			//System.out.println(this.vacabulary);
			for(int m = 0;m < M;m++ ){
				int N = docs.get(m).doc_len; 
				for(int n = 0;n < N;n++){
					z[m][n] = newTopic(m,n);
				}
			}
		}
		
		for(int k = 0;k < K;k++){
			for(int v = 0;v < 100;v++){
				System.out.print(nkw[k][v]+" ");
			}		
			System.out.println();
		}
	}
	

	public String getWordByIndex(int index){
		//通过索引获取词语
		return vacabulary.get(index);
	}
	
	public class Document{
		public String docname;//文档名字
		public int AorT;
		public int[] docwords;//文档词语列表，存储词语在字典的索引值
		public int doc_len;//标记文档中单词的数目
		
		public Document(String dname,String[] words,int AorT){
			//words:已经将噪声词语删除的字符列表
			this.docname = dname;
			this.AorT = AorT;
			docwords = new int[words.length];
			doc_len = 0;
			
			for(int i = 0;i < words.length;i++ ){
				String word = words[i];
				//if(pattern.matcher(word).matches()) continue;
				if(word.length() < 2||pattern.matcher(word).matches()||noiselist.contains(word)) 
					continue ;
				if(wordtoindex.containsKey(word)){
					//hash表中已经包含了该词语
					docwords[doc_len++] = wordtoindex.get(word);
				}
				else{//否则，将新词汇加入词汇字典和hash表
					int newindex = wordtoindex.size();
					vacabulary.add(word);
					wordtoindex.put(word, newindex);
					docwords[doc_len++] = newindex;
					V += 1;
				}
			}			
		}
	
	}

}