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
	static int ilabelCounter, wlabelCounter;
	static String mname;
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
		Hashtable<String, Integer> sbt = new Hashtable<String, Integer>();
		//TODO: add code to convert a method declaration to SaM code.t 
		//Since the only data type is an int, you can safely check for int 
		//in the tokenizer.
		//TODO: add appropriate exception handlers to generate useful error msgs.
		if (!f.check("int")) //must match at begining
		{
			throw new TokenizerException("line " + f.lineNo() + " invalid Method Type");
		}

		
		String methodName = f.getWord();
		mname = methodName;
		
		methodname.put(methodName, 1);

		pgm += methodName + ":\n\tPUSHIMM 0\n";

		if (!f.check ('(')) 
			throw new TokenizerException("line " + f.lineNo() + " missing open parenthesis");

		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		String formals = parseFp(f, sbt); //getFormals(f);
		int arg_num = sbt.size();
		Set<String> keys = sbt.keySet();
		for (String key: keys)
			sbt.put(key, sbt.get(key) - arg_num);

		pgm += parseB(f, sbt);
		pgm += methodName + "End:\n";
		pgm += "\tSTOREOFF -" + (arg_num + 1) + "\n";
		int local = 0;
		for (String key: keys)
			if (sbt.get(key) > 0)
				local++;
		if (local != 0)
			pgm += "\tADDSP -" + local + "\n";
		pgm += "\tJUMPIND\n";
		return pgm;
		
		//TODO The main method needs to be handled differently, since thats where execution starts.
		
		//f.check ('('); // must be an opening parenthesis
		
		//String formals = parseFp(f, sbt); //getFormals(f, sbt);
		
		//String body = parseB(f, sbt);
		//f.check(")");  // must be an closing parenthesis
		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		//return formals+body;
	}
	static String parseFp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseFp");
		if (f.check(')'))
		{
			return "";
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
			return "";
		else
			return parseTID(f, sbt) + parseTIDp(f, sbt);
		//{
		//	System.out.println("Reaching )");
		//	return "";
		//}
		//else
		//{
		//	return parseTID(f, sbt) + parseTIDp(f, sbt);
		//}
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
		int arg_num = sbt.size();
		if(!checkType(f).equals("word"))
			throw new TokenizerException("Line number "+f.lineNo() + " : Valid variable name expected");
		
		String var = f.getWord();
		if(methodname.containsKey(var))
		    throw new TokenizerException("Variable name already used");
	//	else return parseEp(f, sbt, arg_num);

	//}
	//static String parseEp(SamTokenizer f, Hashtable<String, Integer> sbt)
		else 
		{
			int s = sbt.size();
			
			//We want to store the offset for var. 
			//This will be (# of local variables already defined + 2)
			
			sbt.put(var, sbt.size() - arg_num + 1);
			
			//return "PUSHIMM 0\n" + parseDp(f, sbt, var);
			return "\tPUSHIMM 0\n" + parseEp(f, sbt, var, arg_num);
		}
	}
	
	
	
	 
	
	
	
	
	static String parseEp(SamTokenizer f, Hashtable<String, Integer> sbt, String str, int arg_num)
	{
		System.out.println("parseEp");
		if(f.test(','))
			return parseIDEp(f, sbt, arg_num);
		else if(f.test('='))
			return parseE(f, sbt, str);
		else if(f.test(';'))
		{
			return parseIDEp(f, sbt, arg_num);
		}
		else throw new TokenizerException("Line number " + f.lineNo() + " : " + "Invalid statement");
	}
	
	
	
	
	static String parseE(SamTokenizer f, Hashtable<String, Integer> sbt, String str)
	{
		System.out.println("parseE");

		if(f.check('='))
		{
			System.out.println("Reached variable declaration");
			//return parseEXP(f, sbt);
			return parseEXP(f, sbt) + "\tSTOREOFF "+sbt.get(str)+"\n" + parseIDEp(f, sbt, 0);
		}
		else throw new TokenizerException("Line number " + f.lineNo()+ " : Invalid statement");
	}
	//static String parseIDEp(SamTokenizer f, Hashtable<String, Integer> sbt)
	//{
	//	if(f.check(','))
	
	
	
	
	static String parseIDEp(SamTokenizer f, Hashtable<String, Integer> sbt, int arg_num)
	{
		System.out.println("parseIDEp");

		if(f.test(','))
			return parseIDE(f, sbt, arg_num) + parseIDEp(f, sbt, arg_num);
		else if(f.check(';'))
			return "";
		else throw new TokenizerException("Line number " + f.lineNo() + " : " + "Invalid Statement IDEp");
	}

	
	
	
	static String parseIDE(SamTokenizer f, Hashtable<String, Integer> sbt, int arg_num)
	{
		System.out.println("parseIDE");

		if(f.check(','))
		{
			//String word = f.getWord();
			//return parseEp(f, sbt);
			String var = f.getWord();
			int s = sbt.size();
			sbt.put(var, s - arg_num + 1);
			
			return "\tPUSHIMM 0\n" + parseEp(f, sbt, var, arg_num);
		}
		else throw new TokenizerException("Line number " + f.lineNo() + " : " + "Invalid Statement IDE");
	}

	
	
	
	static String parseSp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseSp");
		if (f.check('}'))
		{
			return "";
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
			return s;
		}
		else if (f.check(';'))
		{
			return "";
		}
		else if (f.check("return"))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(';'))
				System.out.println("Semicolon expected");
			return parsedEXP + "\tJUMP " + mname + "End\n";

		}
		else if (f.check("if"))
		{
			System.out.println("If part");
			String ifs = "";
			if(f.check('('))
			{
				int l1 = ilabelCounter;
				int l2 = ilabelCounter + 1;
				ilabelCounter += 2;
				String parsedEXP = parseEXP(f, sbt);
				ifs += parsedEXP;
				ifs += "\tJUMPC newLabel" + l1 + "\n";
				if(f.check(')'))
				{
					String parsedS = parseS(f, sbt);
					if(f.check("else"))
					{
						System.out.println("Else part");
						String parsedS2 = parseS(f, sbt);
						ifs += parsedS2;
						ifs += "\tJUMP newLabel" + l2 + "\n";
						ifs += "newLabel" + l1 + ":\n";
						ifs += parsedS;
						ifs += "newLabel" + l2 + ":\n";
						return ifs;
						//return "";
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
			int l1 = wlabelCounter;
			int l2 = wlabelCounter + 1;
			wlabelCounter += 2;
			String whs = "Label" + l1 + ":\n";
			if(f.check('('))
			{
				String parsedEXP = parseEXP(f, sbt);
				whs += parsedEXP;
				whs += "\tISNIL\n";
				whs += "\tJUMPC Label" + l2 + "\n";
				if(f.check(')'))
				{
					String parsedS = parseS(f, sbt);
					whs += parsedS;
					whs += "\tJUMP Label" + l1 + "\n";
					whs += "Label" + l2 + ":\n";
					loop--;
					return whs;
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
				return "\tJUMP Label" + (wlabelCounter - 1) + "\n";
			else throw new TokenizerException("Semicolon expected");
		}

		String var = f.getWord();
		if(!sbt.containsKey(var))
			throw new TokenizerException("Line number "+f.lineNo()+ " : Variable undefined");

		if(f.check('='))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(f.check(';'))
				return parsedEXP + "\tSTOREOFF "+sbt.get(var)+"\n";
			else
				throw new TokenizerException("Line number " + f.lineNo() + " : Semicolon expected");
		}
		else throw new TokenizerException("Line number "+f.lineNo()+ " : Assignment expected");
	}
	
	
	
	
	static String parseEXP(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		System.out.println("parseEXP");
		if(f.check("true"))
		{
			return "\tPUSHIMM 1\n";
		}

		if(f.check("false"))
		{
			return "\tPUSHIMM 0\n";
		}

		if(f.check('('))
		{
			return parseX(f, sbt);
		}

		if(f.check('-'))
		{
			throw new TokenizerException("Line number " + f.lineNo()+" : Negative integers must be within parenthesis");
		}
		
		
		switch(f.peekAtKind())
		{

		case INTEGER:
			int n = f.getInt();
			return "\tPUSHIMM "+ n + "\n";
			
		case WORD:
			String ID = f.getWord();

			if(methodname.containsKey(ID))
			{
				//TODO Handle method call here
				if(f.check('('))
					return parseA(f, sbt);
				else
					throw new TokenizerException("Line number " + f.lineNo() + " : " + ID + "is already used as method name");
			}

			else if(sbt.containsKey(ID))
			{
				return "\tPUSHOFF " + sbt.get(ID) + "\n"; 
				
			}
			
			else 
				throw new TokenizerException("Line number " + f.lineNo() + " : " + ID + " is undefined");
			
		default:
			throw new TokenizerException("Line number " + f.lineNo() + " : Invalid Expression"); //Need a more detailed error description

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
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			}
			else return s + "\tPUSHIMM -1\n" + "MULT\n";
		}
		if(f.check('!'))
		{
			String s = parseEXP(f, sbt);

			if(!f.check(')'))
			{
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			}
			else return s + "\tNOT\n";
		}

		return parseEXP(f, sbt) + parseOPp(f, sbt);
	}
	
	
	
	
	static String parseOPp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{

		if(f.check('+'))
		{
			String parsedEXP = parseEXP(f, sbt);
			
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else 
				return parsedEXP + "\tADD\n";

		}
		else if(f.check('-'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else 
				return parsedEXP + "\tSUB\n";

		}
		else if(f.check('*'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else 
				return parsedEXP + "\tTIMES\n";

		}
		else if(f.check('/'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else 
				return parsedEXP + "\tDIV\n";

		}
		else if(f.check('>'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else 
				return parsedEXP + "\tGREATER\n";

		}
		else if(f.check('<'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else return parsedEXP + "\tLESS\n";

		}
		else if(f.check('='))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else return parsedEXP + "\tEQUAL\n";

		}
		else if(f.check('|'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else return parsedEXP + "\tOR\n";

		}
		else if(f.check('&'))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else return parsedEXP + "\tAND\n";

		}
		else if(f.check(')'))
		{
			return "";

		}
		else throw new TokenizerException("Line number " + f.lineNo() + " : Invalid Expression");
	}

	
	
	
	static String parseA(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if(f.check(')'))
			return "";
		else
		{
			//TODO: Handle actuals for method calls here
			return parseEXP(f, sbt) + parseAp(f, sbt);
		}
	}

	
	
	
	static String parseAp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		//TODO Handle actuals for method calls here
		if(f.check(')'))
			return "";
		else if(f.check(','))
		{
			return parseEXP(f, sbt) + parseAp(f, sbt);
		}
		else throw new TokenizerException("Invalid Expression");
	}

	
	
	
	public static void main(String []args)
	{
		// First argument is input file
		// Second argument is output file
		loop = 0;
		ilabelCounter = 0;
		wlabelCounter = 0;
		System.out.println(args[0]);
		methodname = new Hashtable<String, Integer>();
		String result = compiler (args[0]);
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(args[1]), "utf-8"));
			writer.write(result);
			writer.close();
		} catch (IOException ex) {
			System.out.println ("Error in writing to output");
		}
	}
}



