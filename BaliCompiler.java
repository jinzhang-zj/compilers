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
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliCompiler
{
	Hashtable<String, Integer> methodname;
	// help function to determine next token type
	static String checkType(SamTokenizer f)
	{
		switch (f.peekAtKind())
		{
			case CHARACTER:
				return "char";
			case COMMENT:
				return "comment";
			case EOF:
				return "eof";
			case FLOAT:
				return "float";
			case INTEGER:
				return "integer";
			case OPERATOR:
				return "operator";
			case STRING:
				return "string";
			case WORD:
				return "word";
			default:
				return "unknown";
		}
	}
	static String compiler(String fileName) 
	{
		System.out.println("compiler");
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
		System.out.println("getProgram");
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
		System.out.println("getMethod");
		//TODO: add code to convert a method declaration to SaM code.
		//Since the only data type is an int, you can safely check for int 
		//in the tokenizer.
		//TODO: add appropriate exception handlers to generate useful error msgs.
		if (!f.check("int")) //must match at begining
		{
			throw new TokenizerException("Invalid Method Type");
		}
	
		String methodName = f.getWord();;

		f.check ('('); // must be an opening parenthesis
		String formals = parseFp(f); //getFormals(f);
		String body = parseB(f);
		//f.check(")");  // must be an closing parenthesis
		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		return null;
	}
	static String parseFp(SamTokenizer f)
	{
		System.out.println("parseFp");
		if (f.check(')'))
		{
			return null;
		}
		else
		{
			return parseF(f);
		}
	}
	static String getExp(SamTokenizer f) 
	{
		System.out.println("getExp");
                return null;
	}
	static String parseF(SamTokenizer f)
	{
		System.out.println("parseF");
		if (!f.check("int"))
		{
			throw new TokenizerException("Invalid Formal Type");
		}
		String id = f.getWord();
		return id + parseTIDp(f);
	}
	static String parseTIDp(SamTokenizer f)
	{
		System.out.println("parseTIDp");
		if (f.check(')'))
		{
			System.out.println("Reaching )");
			return null;
		}
		else
		{
			return parseTID(f) + parseTID(f);
		}
	}
	static String parseTID(SamTokenizer f)
	{
		System.out.println("parseTID");
		if (!f.check(','))
		{
			System.out.println("Missing Comma");
			throw new TokenizerException("Missing Comma");
		}
		if (!f.check("int"))
		{
			System.out.println("Missing Formal Type");
			throw new TokenizerException("Missing Formal Type");
		}
        	return f.getWord();
	}	
	static String parseB(SamTokenizer f)
	{
		System.out.println("parseB");
		if (!f.check('{'))
		{
			System.out.println("Missing curly brackets");
			throw new TokenizerException("Missing curly brackets");
		}
		return parseVp(f);
	}
	static String parseVp(SamTokenizer f)
	{
		System.out.println("parseVp");
		if (f.check("int"))
		{
			return parseV(f) + parseVp(f);
		}
		else
		{
			return parseSp(f);
		}
	}
	static String parseV(SamTokenizer f)
	{
		System.out.println("parseV");
		String var = f.getWord();
		return parseEp(f);
	}
	static String parseEp(SamTokenizer f)
	{
		System.out.println("parseFp");
		return null;
	}
	static String parseSp(SamTokenizer f)
	{
		System.out.println("parseSp");
		if (f.check('}'))
		{
			return null;
		}
		return parseS(f) + parseSp(f);
	}
	static String parseS(SamTokenizer f)
	{
		System.out.println("parseS");
		if (f.check('{'))
		{
			String s = parseSp(f);
			if (!f.check('}'))
			{
				System.out.println("Missing curly bracket");
				throw new TokenizerException("Missing curly bracket");
			}
			return null;
		}
		else if (f.check(';'))
		{
			return null;
		}
		String word = f.getWord();
		return word;
	}
	public static void main(String []args){
		// First argument is input file
		// Second argument is output file
		System.out.println(args[0]);
		String result = compiler (args[0]);
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(args[1]), "utf-8"));
			writer.write(result);
		} catch (IOException ex) {
			System.out.println ("Error in writing to output");
		}
	}
}
