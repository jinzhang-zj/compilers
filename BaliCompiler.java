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
	static Hashtable<String, Integer> methodname;
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
			System.out.println(e);
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
		System.out.println("parseEp");
		if(f.check(','))
			return parseIDEp(f);
		else if(f.test('='))
			return parseE(f) + parseIDEp(f);
		else if(f.check(';'))
		    {
			System.out.println("Reached variable declaration");
			return null;
		    }
		else throw new TokenizerException("Invalid Statement");
	}
	static String parseE(SamTokenizer f)
	{
		System.out.println("parseE");

		if(f.check(';'))
		    {
			System.out.println("End of variable declaration statement");
			return null;
		    }
		else if(f.check('='))
		    {
			System.out.println("Reached variable declaration");
			return parseEXP(f);
		    }
		else throw new TokenizerException("Invalid Statement when parsing E");

	}
	static String parseIDEp(SamTokenizer f)
	{
		if(f.check(','))
			return parseIDE(f) + parseIDEp(f);
		else if(f.check(';'))
			return null;
		else throw new TokenizerException("Invalid Statement");
	}
	static String parseIDE(SamTokenizer f)
	{
		if(f.check(','))
		{
			String word = f.getWord();
			return parseEp(f);
		}
		else throw new TokenizerException("Invalid Statement");
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
		else if (f.check("return"))
		{
			return null;
		}
		else if (f.check("if"))
		{
			return null;
		}
		else if (f.check("while"))
		{
			return null;
		}
		else if (f.check("break"))
		{
			return null;
		}
		String word = f.getWord();
		if(f.check('='))
		    {
			String parsedEXP = parseEXP(f);
			if(f.check(';'))
			    return null;
			else
			    throw new TokenizerException("Semicolon expected");
		    }

		return null;
	}
	static String parseEXP(SamTokenizer f)
        {
	    System.out.println("parseEXP");
		if(f.check("true"))
		{
			return null;
		}

		if(f.check("false"))
		{
			return null;
		}

		if(f.check('('))
		{
			return parseX(f);
		}

		if(f.check('-'))
		{
			throw new TokenizerException("Negative integers must be within parenthesis"); //Need to give line number also?
		}
		switch(f.peekAtKind())
		{

		case INTEGER:
			int n = f.getInt();
			return null;
		case WORD:
		    String ID = f.getWord();
		    return null;
			// case WORD:
				//     String ID = getWord(f);
				//     if(methodname.get(ID)==1)
			//     {
			//         if(f.check('('))
			//             return parseA(f);
			//         else
			//         {
			//             throw new TokenizerException("Variable name cannot be same as method name");
			//             return null;
			//         }
			//     }
			//     else
			//     {
			//         //Here we need to add code for the case when ID is a variable
			//         return null;
			//     }
			//     break;

		default:
			throw new TokenizerException("Invalid Expression"); //Need a more detailed error description

		}

	}
	static String parseX(SamTokenizer f)
	{
	    System.out.println("parseX");
		if(f.check('-'))
		{
			String s = parseEXP(f);

			if(!f.check(')'))
				{
				throw new TokenizerException(") expected");
				}
			else return s;
		}
		if(f.check('!'))
		{
			String s = parseEXP(f);

			if(!f.check(')'))
				{
				throw new TokenizerException(") expected");
				}
			else return s;
		}

		return parseEXP(f) + parseOPp(f);
	}
	static String parseOPp(SamTokenizer f)
	{
	    System.out.println("parseOPp");
		if(f.check('+'))
		{
			String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check('-'))
		{
		    String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check('*'))
		{
		    String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check('/'))
		{
		    String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check('>'))
		{
		    String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check('<'))
		{
		    String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check('='))
		{
		    String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check('|'))
		{
		    String parsedEXP = parseEXP(f);
			if(!f.check(')'))
			    throw new TokenizerException(") expected");
			else return parsedEXP;
			 
		}
		else if(f.check(')'))
		{
		   return null;
			 
		}
		else throw new TokenizerException("Invalid Expression");
	}

	static String parseA(SamTokenizer f)
	{
		if(f.check(')'))
			return null;
		else
		{
			return parseEXP(f) + parseAp(f);
		}
	}

	static String parseAp(SamTokenizer f)
	{
		if(f.check(')'))
			return null;
		else if(f.check(','))
		{
			return parseEXP(f) + parseAp(f);
		}
		else throw new TokenizerException("Invalid Expression");
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



//Note to self: our grammar for E' is incorrect: shouldnt we have a production for ';'?
