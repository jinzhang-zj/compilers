import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Set;

import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliCompiler
{
	static Hashtable<String, Integer> methodname;
	static int loop;
	static int labelCounter;
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
		    System.out.println(e.toString());
			return "STOP\n";
		}		
	}
	static String getMethod(SamTokenizer f)
	{
		System.out.println("getMethod");
		String pgm = "";
		Hashtable<String, Integer> sbt = new Hashtable<String, Integer> ();
		//TODO: add code to convert a method declaration to SaM code.t 
		//Since the only data type is an int, you can safely check for int 
		//in the tokenizer.
		//TODO: add appropriate exception handlers to generate useful error msgs.
		if (!f.check("int")) //must match at begining
		{
			throw new TokenizerException("line " + f.lineNo() + " invalid Method Type");
		}

		
		String methodName = f.getWord();
		methodname.put(methodName, 1);

		pgm += methodName + ":PUSHIMM 0\n";

		if (!f.check ('(')) 
			throw new TokenizerException("line " + f.lineNo() + " missing open parenthesis");

		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		String formals = parseFp(f, sbt); //getFormals(f);
		int arg_num = sbt.size();
		Set<String> keys = sbt.keySet();
		for (String key: keys)
			sbt.put(key, sbt.get(key) - arg_num);

		pgm += "\tLINK\n";
		pgm += parseB(f, sbt);
		pgm += methodname + "End:\n";
		pgm += "STOREOFF -" + (arg_num + 1) + "\n";
		int local = 0;
		for (String key: keys)
			if (sbt.get(key) > 0)
				local++;
		if (local != 0)
			pgm += "ADD SP -" + local;
		pgm += "JUMPIND\n";
		return pgm;
	}
	static String parseFp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseFp");
		if (f.check(')'))
		{
			return null;
		}
		else
		{
			return parseF(f, sbt);
		}
	}
	static String parseF(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseF");
		if (!f.check("int"))
		{
			throw new TokenizerException("line " + f.lineNo() + "Invalid Formal Type");
		}
		String id = f.getWord();
		int idx = sbt.size();
		sbt.put(id, idx);
		return id + parseTIDp(f, sbt);
	}
	static String parseTIDp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseTIDp");
		if (f.check(')'))
			
			return null;
		else
			return parseTID(f, sbt) + parseTIDp(f, sbt);
	}
	static String parseTID(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseTID");
		if (!f.check(','))
		{
			System.out.println("line " + f.lineNo() + ": Missing Comma");
			throw new TokenizerException("line " + f.lineNo() + ": Missing Comma");
		}
		if (!f.check("int"))
		{
			System.out.println("line " + f.lineNo() + ": Missing Formal Type");
			throw new TokenizerException("line " + f.lineNo() + ": Missing Formal Type");
		}
		String s = f.getWord();
		int idx = sbt.size();
		sbt.put(s, idx);
		return null;
	}	
	static String parseB(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseB");
		if (!f.check('{'))
		{
			System.out.println("Missing curly brackets in parseB");
			throw new TokenizerException("Missing curly brackets");
		}
		return parseVp(f, sbt);
	}
	static String parseVp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseVp");
		if (f.check("int"))
		{
			return parseV(f, sbt) + parseVp(f, sbt);
		}
		else
		{
			return parseSp(f, sbt);
		}
	}
	static String parseV(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseV");
		String var = f.getWord();
		if(methodname.containsKey(var))
		    throw new TokenizerException("Variable name already used");
		else return parseEp(f, sbt);

	}
	static String parseEp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseEp");
		if(f.check(','))
			return parseIDEp(f, sbt);
		else if(f.test('='))
			return parseE(f, sbt) + parseIDEp(f, sbt);
		else if(f.check(';'))
		{
			System.out.println("Reached variable declaration");
			return null;
		}
		else throw new TokenizerException("Invalid Statement");
	}
	static String parseE(SamTokenizer f, Hashtable<String, Integer> sbt)
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
			return parseEXP(f, sbt);
		}
		else throw new TokenizerException("Invalid Statement when parsing E");

	}
	static String parseIDEp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if(f.check(','))
			return parseIDE(f, sbt) + parseIDEp(f, sbt);
		else if(f.check(';'))
			return null;
		else throw new TokenizerException("Invalid Statement");
	}
	static String parseIDE(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if(f.check(','))
		{
			String word = f.getWord();
			return parseEp(f, sbt);
		}
		else throw new TokenizerException("Invalid Statement");
	}
	static String parseSp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseSp");
		if (f.check('}'))
		{
			return null;
		}
		return parseS(f, sbt) + parseSp(f, sbt);
	}
	static String parseS(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseS");
		if (f.check('{'))
		{
			String s = parseSp(f, sbt);
			//if (!f.check('}'))
			//{
			//	System.out.println("Missing curly bracket");
			//	throw new TokenizerException("Missing curly bracket");
			//}
			return null;
		}
		else if (f.check(';'))
		{
			return null;
		}
		else if (f.check("return"))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(';'))
				System.out.println("Semicolon expected");
			return parsedEXP;

		}
		else if (f.check("if"))
		{
			System.out.println("If part");
			String ifs = "";
			if(f.check('('))
			{
				String parsedEXP = parseEXP(f, sbt);
				ifs += parsedEXP;
				ifs += "\tJUMPC newLabel" + labelCounter + "\n";
				if(f.check(')'))
				{
					String parsedS = parseS(f, sbt);
					if(f.check("else"))
					{
						System.out.println("Else part");
						String parsedS2 = parseS(f, sbt);
						ifs += parsedS2;
						ifs += "\tJUMP newLabel" + (labelCounter + 1) + "\n";
						ifs += "newLabel" + labelCounter + ":\n";
						ifs += parsedS;
						ifs += "newLabel" + (labelCounter + 1) + ":\n";
						labelCounter += 2;
						return ifs;
					}
					else
						throw new TokenizerException("line " + f.lineNo() + ": missing else statment");
				}
				else
					throw new TokenizerException("line " + f.lineNo() + ": missing close parenthesis");
			}
			else
				throw new TokenizerException("line " + f.lineNo() + ": missing open parenthesis");
		}
		else if (f.check("while"))
		{
			loop++;
			if(f.check('('))
			{
				String parsedEXP = parseEXP(f, sbt);
				if(f.check(')'))
				{
					String parsedS = parseS(f, sbt);
					loop--;
					return null;
				}
				else
					throw new TokenizerException("line " + f.lineNo() + ": missing close parenthesis");
			}
			else
				throw new TokenizerException("line " + f.lineNo() + ": missing open parenthesis");
		}
		else if (f.check("break"))
		{
			if (loop <= 0)
				throw new TokenizerException("line " + f.lineNo() + ": break outside the loop");
			else if(f.check(';'))
				return null;
			else throw new TokenizerException("Semicolon expected");
		}

		String word = f.getWord();
		System.out.println(word);
		if(f.check('='))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(f.check(';'))
				return null;
			else
				throw new TokenizerException("Semicolon expected");
		}

		return null;
	}
	static String parseEXP(SamTokenizer f, Hashtable<String, Integer> sbt)
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
			return parseX(f, sbt);
		}

		if(f.check('-'))
		{
			throw new TokenizerException("Negative integers must be within parenthesis"); //Need to give line number also?
		}
		switch(f.peekAtKind())
		{

		case INTEGER:
			int n = f.getInt();
			System.out.println(n);
			return null;
		case WORD:
			String ID = f.getWord();

			if(methodname.containsKey(ID))
			{
				System.out.println("Method call");
				if(f.check('('))
					return parseA(f, sbt);
				else
				{
					throw new TokenizerException("Variable name cannot be same as method name");
				}
			}
			else
			{
				System.out.println("Variable name");

				//Here we need to add code for the case when ID is a variable
				return null;
			}
		default:
			throw new TokenizerException("Invalid Expression"); //Need a more detailed error description

		}

	}
	static String parseX(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseX");
		if(f.check('-'))
		{
			String s = parseEXP(f, sbt);

			if(!f.check(')'))
			{
				throw new TokenizerException(") expected");
			}
			else return s;
		}
		if(f.check('!'))
		{
			String s = parseEXP(f, sbt);

			if(!f.check(')'))
			{
				throw new TokenizerException(") expected");
			}
			else return s;
		}

		return parseEXP(f, sbt) + parseOPp(f, sbt);
	}
	static String parseOPp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseOPp");
		if(f.check('+'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException(") expected");
			else return parsedEXP;

		}
		else if(f.check('-'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException(") expected");
			else return parsedEXP;

		}
		else if(f.check('*'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException(") expected");
			else return parsedEXP;

		}
		else if(f.check('/'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException(") expected");
			else return parsedEXP;

		}
		else if(f.check('>'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException(") expected");
			else return parsedEXP;

		}
		else if(f.check('<'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException(") expected");
			else return parsedEXP;

		}
		else if(f.check('='))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException(") expected");
			else return parsedEXP;

		}
		else if(f.check('|'))
		{
			String parsedEXP = parseEXP(f, sbt);
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

	static String parseA(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if(f.check(')'))
			return null;
		else
		{
			return parseEXP(f, sbt) + parseAp(f, sbt);
		}
	}

	static String parseAp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if(f.check(')'))
			return null;
		else if(f.check(','))
		{
			return parseEXP(f, sbt) + parseAp(f, sbt);
		}
		else throw new TokenizerException("Invalid Expression");
	}

	public static void main(String []args){
		// First argument is input file
		// Second argument is output file
		loop = 0;
		labelCounter = 0;
		System.out.println(args[0]);
		methodname = new Hashtable<String, Integer>();
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
