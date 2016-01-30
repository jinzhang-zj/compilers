import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Hashtable;

import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;

public class BaliCompiler
{
	Hashtable<String, Integer> methodname;
	static String compiler(String fileName) 
	{
		//System.out.println("compiler");
		//returns SaM code for program in file
		try 
		{
			SamTokenizer f = new SamTokenizer (fileName);
			String pgm = getProgram(f);
			return pgm;
		} 
		catch (Exception e) 
		{
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}
	static String getProgram(SamTokenizer f)
	{
		//System.out.println("getProgram");
		try
		{
			String pgm="";
			while(f.peekAtKind()!=TokenType.EOF)
			{
				pgm+= getMethod(f);
			}
			return pgm;
		}
		catch(Exception e)
		{
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}		
	}
	static String getMethod(SamTokenizer f)
	{
		//System.out.println("getMethod");
		//TODO: add code to convert a method declaration to SaM code.
		//Since the only data type is an int, you can safely check for int 
		//in the tokenizer.
		//TODO: add appropriate exception handlers to generate useful error msgs.
		if (!f.check("int")) //must match at begining
		{
			throw new TokenierException("Invalid Method Type");
		}
	
		String methodName;
		switch(f.peekAtKind())
		{
			case WORD:
				methodName = f.getWord();
				break;
			case STRING:
				methodName = f.getString();
				break;			
			default:
				throw new TokenierException("Invalid Method Name");
		}

		f.check ("("); // must be an opening parenthesis
		String formals = parseFp(f); //getFormals(f);
		//f.check(")");  // must be an closing parenthesis
		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		return null;
	}
	static String parseFp(SamTokenier f)
	{
		if (f.check(")"))
		{
			return "";
		}
		else
		{
			parseF(f);
		}
	}
	static String getExp(SamTokenizer f) 
	{
	}
	static String parseF(SamTokenizer f){
		if (!f.check("int"))
		{
			throw new TokenierException("Invalid Formal Type");
		}
		String id;
		switch(f.peekAtKind())
		{
			case WORD:
				id = f.getWord();
				break;
			case STRING:
				id = f.getString();
				break;
			default:
				throw new TokenierException("Invalid ID Type");
		}
		parseTIDp(f);
	}
	static String parseTIDp(SameTonkenier f){
		if (f.check(")"))
		{
			return "";
		}
		else
		{
			parseTID(f);
			parseTIDp(f);
		}
	}
	static String parseTID(SameTokenier f){
		if (!f.check(","))
	}	


	public static void main(String []args){
		// First argument is input file
		// Second argument is output file
		System.out.println(args[0]);
		String result = compiler (args[0]);
		try (Writer writer = 
		new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "utf-8"))) {
			writer.write(result);
		} catch (IOException ex) {
			System.out.println ("Error in writing to output");
		}
	}
}
