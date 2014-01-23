/*
 *  TextJustification.java  - Text width justification utilities
 *  Copyright (C) 2013 Dmytro Ryabko
 *  http://ua.linkedin.com/in/dmytroryabko/
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package dim.utils.yandex;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This is utility to split input text into paragraphs and justify it, e.g.
 * format it to become text with width N (20 - 120) characters aligned to the
 * right and the left edge. Line width should be increased by adding spaces -
 * one by one, starting from the right. The end of the paragraph is a string
 * that ends in the corresponding punctuation. New paragraphs should indent four
 * spaces. Paragraph of a single line less than half of N - is the title and its
 * justify not needed.
 * <p>
 * To compile this class use the following command: (JAVA_HOME - path to JDK installation root)
 * <br>
 * {@code %JAVA_HOME%\bin\javac TextJustify.java}
 * <p>
 * To run this utility use the following command:
 * <br>
 * {@code %JAVA_HOME%\bin\java dim.utils.yandex.TextJustify [in_file] [out_file]}
 * <br>
 * 		&emsp;<b>[in_file]</b> - path to input file
 * <br>
 * 		&emsp;<b>[out_file]</b>	- path to output file
 * 
 * @author Dmytro Ryabko
 * @since 7.0
 */
public class TextJustification {
	/**
	 * Text width constant
	 */
	private static final int N	= 100;
	
	/**
	 * Name of input file
	 */
	private String fileNameIn;
	/**
	 * Name of output file
	 */
	private String fileNameOut;
	/**
	 * Star point of a program.
	 * @param args [0] in_file
	 * @param args [1] out_file
	 */
	public static void main(String[] args){
		if(args.length < 2 || N < 60 || N > 120) 
			printUsage();
		TextJustification justify = new TextJustification(args[0], args[1]);
		justify.processText();
	}
	
	/**
	 * Constructor
	 * @param in input file name
	 * @param out output file name
	 */
	public TextJustification(String in, String out) {
		this.fileNameIn = in;
		this.fileNameOut = out;
	}
	
	/**
	 * Do needed load and save and start internal {@link #process(String)}
	 */
	public void processText(){
		try {
			String sIn = readFile(fileNameIn, Charset.defaultCharset());
			String sOut = process(sIn);
			writeFile(fileNameOut, Charset.defaultCharset(), sOut);
		} catch (IOException e) {
			System.err.println("Get exception: "+e.getMessage());
			printUsage();
		}
	}
	
	/**
	 * Main routine doing text justification
	 * @param in input text
	 * @return justified text
	 */
	private String process(String in){
		/**
		 * This class is needed to store and handle internal work for justification
		 * @author Dmytro Ryabko
		 */
		class Justificator{
			/**
			 * Store justified text here
			 */
			StringBuilder res = new StringBuilder();
			/**
			 * A list of words to be compiled to a one line
			 */
			ArrayList<String> arr = new ArrayList<String>();
			/**
			 * Length of a line compiled from words from {@code arr} list
			 */
			int arrLength = 0;
			/**
			 * Recursively add whitespaces between words from right
			 * @return Justified line
			 */
			String doJustify(){
				if(arr.size() > 1){
					for (int i = arr.size()-1; i > 0; i--) {
						if(i % 2 == 0) //on even places we have whitespaces, on odd - words
							continue;
						arr.set(i, arr.get(i) + " ");
						arrLength++;
						if(arrLength >= N)
							return toString();
					}
					doJustify(); //need one more loop of doJustify method
				}
				return toString();
			}
			/**
			 * Do justification or just compile a string from words from a line list
			 * @param flag if {@code true} just compile a string, if {@code false} do justification
			 */
			void doJustify(boolean flag){
				if(flag)
					res.append(toString());
				else
					res.append(doJustify());
				clear();
			}
			/**
			 * Add a word to a line list
			 * @param s word to be added
			 * @param wordLength length of added s 
			 */
			void add(String s, int wordLength){
				arr.add(s);
				arrLength += wordLength;
			}
			/**
			 * Append a word to a justified text
			 * @param s word to be added 
			 */
			void append(String s){
				res.append(s);
			}
			/**
			 * Returns {@code true} if line list has no elements
			 * @return {@code ture} if line list has no elements
			 */
			boolean isEmpty(){
				return arr.isEmpty();
			}
			/**
			 * Empty line list
			 */
			void clear(){
				arr.clear();
				arrLength = 0;
			}
			/**
			 * Get justified text
			 * @return justified text
			 */
			String get(){
				return res.toString();
			}
			
			/**
			 * Compile a line string from line list
			 * @return line string from line list
			 */
			@Override
			public String toString(){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < arr.size(); i++) {
					sb.append(arr.get(i));
				}
				return sb.toString();
			}
		}
		
		Justificator just = new Justificator();
		/**
		 * Round all new lines with witespaces to pick out them to a separate word
		 */
		in = in.replaceAll("\r\n", " \r\n ");
		String[] words = in.split(" "); //separate input text to a words
		/**
		 * Store sequential new lines count here
		 */
		int newLinePrevCount = 0;
		
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			
			word = trim(word);
			int wordLength = trim(word).length();
			if(wordLength > N){
				System.err.println(String.format("Word (\"%s\"=%d) if bigger then text width N=%d, adjust N.",word, wordLength, N));
				printUsage();
			}
			/**
			 * Process first line 
			 */
			if(i==0){ 
				word = "    " + word;
				wordLength += 4;
			}
			/**
			 * Extra whitespace, ignore
			 */
			if(words[i].length() < 1)
				continue;
			/**
			 * If previous paragraph is finished, justify it and start red line
			 */
			if(newLinePrevCount > 0 && Character.isUpperCase(words[i].charAt(0))){
				just.doJustify(just.arrLength < N/2);
				
				String newLines = "";
				while (newLinePrevCount-- > 0) {
					newLines += "\r\n";
				}
				
				word = newLines + "    " + word; //Prepare red line
				wordLength += 4;
			}
			if(words[i].equals("\r\n")){
				newLinePrevCount++;
			}else
				newLinePrevCount = 0;
			/**
			 * No need of empty trimed word more
			 */
			if(word.length() < 1)
				continue;
			/**
			 * If has no space for additional word or we already have line lenght to be equal to N -> do justification
			 * If no -> just add a word to a line list 
			 */
			if(just.arrLength + 1 + wordLength > N || just.arrLength == N){
				just.doJustify(just.arrLength == N);
				just.append("\r\n"); //finish current line
			}else{
				if(!just.isEmpty()){
					just.add(" ", 1);
				}
			}
			just.add(word, wordLength);
		}
		/**
		 * Prepare the last line 
		 */
		if(!just.isEmpty()){
			just.doJustify(just.arrLength < N/2);
		}
		return just.get();
	}
	
	/**
	 * Returns a copy of the string, with \n and \r removed 
	 * @param in source string
	 * @return new string without \n and \r
	 */
	private String trim(String in){
		return in.replace("\n", "").replace("\r", "");
	}
	
	/**
	 * Read file contents to a string
	 * @param fileName file name
	 * @param encoding encoding of a file contents
	 * @return content of a file
	 * @throws IOException
	 */
	private String readFile(String fileName, Charset encoding) throws IOException{
		byte[] encoded = Files.readAllBytes(Paths.get(fileName));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
	
	/**
	 * Write string to a new file
	 * @param fileName file name
	 * @param encoding encoding of a file contents
	 * @param content string need to be written
	 * @throws IOException
	 */
	private void writeFile(String fileName, Charset encoding, String content) throws IOException{
		PrintWriter writer = new PrintWriter(fileName, encoding.name());
		writer.write(content);
		writer.close();
	}
	
	/**
	 * Prints the usage of program.
	 * <p>
	 * Incorrect program usage! Use it the following way:<br>
	 * %JAVA_HOME%\bin\java dim.utils.yandex.TextJustify [in_file] [out_file]<br>
	 * &emsp;[in_file] - path to input file<br>
	 * &emsp;[out_file] - path to output file
	 */
	private static void printUsage(){
		System.err.println();
		System.err.println("Incorrect program usage! Use it the following way:");
		System.err.println("%JAVA_HOME%\\bin\\java dim.utils.yandex.TextJustify [in_file] [out_file]");
		System.err.println("\t[in_file]\t- input file");
		System.err.println("\t[out_file]\t- output file");
		System.exit(1);
	}
	
}
