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
	static int ilabelCounter, wlabelCounter, retCounter, envCounter;
	static String mname;
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
		//returns SaM code for program in file
		try 
		{
			SamTokenizer f = new SamTokenizer (fileName);
			String pgm = getProgram(f);
			return pgm;
		} 
		catch (Exception e) 
		{
			return "STOP\n";
		}
	}
	static String getProgram(SamTokenizer f)
	{
		String pgm="";
		pgm += "\tPUSHIMM 0\n";
		pgm += "\tLINK\n";
		pgm += "\tJSR main\n";
		pgm += "\tPOPFBR\n";
		pgm += "\tSTOP\n";
		try
		{
			while(f.peekAtKind()!=TokenType.EOF)
			{
				pgm += "\n";
				pgm += getMethod(f);
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
		String pgm = "";
		Hashtable<String, Integer> sbt = new Hashtable<String, Integer>();
		if (!f.check("int")) 
		{
			throw new TokenizerException("Line number " + f.lineNo() + " : Invalid Method Type");
		}

		retCounter = 0;		
		String methodName = f.getWord();
		mname = methodName;
		
		methodname.put(methodName, 1);

		pgm += methodName + ":\n";

		if (!f.check ('(')) 
			throw new TokenizerException("Line number " + f.lineNo() + " : Missing open parenthesis");

		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		String formals = parseFp(f, sbt); 
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

		if (retCounter == 0)
			throw new TokenizerException("Missing return statement for method " + methodName);

		return pgm;
	}
	static String parseFp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (f.check(')'))
			return "";
		else
			return parseF(f, sbt);
	}
	static String parseF(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (!f.check("int"))
			throw new TokenizerException("Line number " + f.lineNo() + " : Invalid Formal Type");
		String id = f.getWord();
		int idx = sbt.size();
		sbt.put(id, idx);
		return id + parseTIDp(f, sbt);
	}
	static String parseTIDp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (f.check(')'))
			return "";
		else
			return parseTID(f, sbt) + parseTIDp(f, sbt);
	}
	static String parseTID(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (!f.check(','))
			throw new TokenizerException("Line number " + f.lineNo() + " : Missing Comma");
		if (!f.check("int"))
			throw new TokenizerException("Line number " + f.lineNo() + " : Missing Formal Type");
		String s = f.getWord();
		int idx = sbt.size();
		sbt.put(s, idx);
		return null;
	}	
	
	
	
	
	static String parseB(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (!f.check('{'))
			throw new TokenizerException("Line number " + f.lineNo() + "Missing curly brackets");
		return parseVp(f, sbt);
	}
	
	
	
	
	static String parseVp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (f.check("int"))
			return parseV(f, sbt) + parseVp(f, sbt);
		else
			return parseSp(f, sbt);
	}
	
	
	
	
	static String parseV(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		int arg_num = sbt.size();
		if(!checkType(f).equals("word"))
			throw new TokenizerException("Line number "+f.lineNo() + " : Valid variable name expected");
		
		String var = f.getWord();
		if(methodname.containsKey(var))
		    throw new TokenizerException("Variable name already used");
		else 
		{
			int s = sbt.size();
			sbt.put(var, sbt.size() - arg_num + 2);
			return "\tPUSHIMM 0\n" + parseEp(f, sbt, var, arg_num) + parseIDEp(f, sbt, arg_num);
		}
	}
	
	
	
	 
	
	
	
	
	static String parseEp(SamTokenizer f, Hashtable<String, Integer> sbt, String str, int arg_num)
	{
		if(f.test(','))
			return ""; 
		else if(f.test('='))
			return parseE(f, sbt, str);
		else if(f.test(';'))
			return "";
		else 
			throw new TokenizerException("Line number " + f.lineNo() + " : Invalid statement");
	}
	
	
	
	
	static String parseE(SamTokenizer f, Hashtable<String, Integer> sbt, String str)
	{
		if(f.check('='))
			return parseEXP(f, sbt) + "\tSTOREOFF "+sbt.get(str)+"\n";
		else
			throw new TokenizerException("Line number " + f.lineNo()+ " : Invalid statement");
	}
	
	
	
	
	static String parseIDEp(SamTokenizer f, Hashtable<String, Integer> sbt, int arg_num)
	{
		if(f.test(','))
			return parseIDE(f, sbt, arg_num) + parseIDEp(f, sbt, arg_num);
		else if(f.check(';'))
			return "";
		else
			throw new TokenizerException("Line number " + f.lineNo() + " : " + "Invalid Statement IDEp");
	}

	
	
	
	static String parseIDE(SamTokenizer f, Hashtable<String, Integer> sbt, int arg_num)
	{
		if(f.check(','))
		{
			String var = f.getWord();
			int s = sbt.size();
			sbt.put(var, s - arg_num + 1);
			
			return "\tPUSHIMM 0\n" + parseEp(f, sbt, var, arg_num);
		}
		else
			throw new TokenizerException("Line number " + f.lineNo() + " : " + "Invalid Statement IDE");
	}

	
	
	
	static String parseSp(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (f.check('}'))
			return "";
		return parseS(f, sbt) + parseSp(f, sbt);
	}
	static String parseS(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if (f.check('{'))
			return parseSp(f, sbt);
		else if (f.check(';'))
			return "";
		else if (f.check("return"))
		{
			retCounter++;
			String parsedEXP = parseEXP(f, sbt);
			if(!f.check(';'))
				throw new TokenizerException("Line number " + f.lineNo() + " : Semicolon expected");
			return parsedEXP + "\tJUMP " + mname + "End\n";

		}
		else if (f.check("if"))
		{
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
						String parsedS2 = parseS(f, sbt);
						ifs += parsedS2;
						ifs += "\tJUMP newLabel" + l2 + "\n";
						ifs += "newLabel" + l1 + ":\n";
						ifs += parsedS;
						ifs += "newLabel" + l2 + ":\n";
						return ifs;
					}
					else
						throw new TokenizerException("Line number " + f.lineNo() + ": Missing else statment");
				}
				else
					throw new TokenizerException("Line number " + f.lineNo() + ": Missing close parenthesis");
			}
			else
				throw new TokenizerException("Line number " + f.lineNo() + ": Missing open parenthesis");
		}
		else if (f.check("while"))
		{
			loop++;
			int l1 = wlabelCounter;
			int l2 = wlabelCounter + 1;
			int temp = envCounter;
			envCounter = l2;
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
					envCounter = temp;
					return whs;
				}
				else
					throw new TokenizerException("Line number " + f.lineNo() + ": Missing close parenthesis");
			}
			else
				throw new TokenizerException("Line number " + f.lineNo() + ": Missing open parenthesis");
		}
		else if (f.check("break"))
		{
			if (loop <= 0)
				throw new TokenizerException("Line number " + f.lineNo() + ": Break outside the loop");
			else if(f.check(';'))
				return "\tJUMP Label" + envCounter + "\n";
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
				String mcall = "";
				mcall += "\tPUSHIMM 0\n";
				if(f.check('('))
				{
					int [] arg_num = {0};
					mcall += parseA(f, sbt, arg_num);
					mcall += "\tLINK\n";
					mcall += "\tJSR " + ID + "\n";
					mcall += "\tPOPFBR\n";
					if (arg_num[0] != 0)
						mcall += "\tADDSP -" + arg_num[0] + "\n";
					return mcall;
				}
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
			throw new TokenizerException("Line number " + f.lineNo() + " : Invalid Expression"); 

		}
	}
	
	
	
	
	static String parseX(SamTokenizer f, Hashtable<String, Integer> sbt)
	{
		if(f.check('-'))
		{
			String s = parseEXP(f, sbt);

			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else
				return s + "\tPUSHIMM -1\n" + "\tTIMES\n";
		}
		if(f.check('!'))
		{
			String s = parseEXP(f, sbt);

			if(!f.check(')'))
				throw new TokenizerException("Line number " + f.lineNo() + " : ) expected");
			else
				return s + "\tNOT\n";
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
			return "";
		else throw new TokenizerException("Line number " + f.lineNo() + " : Invalid Expression");
	}

	
	
	
	static String parseA(SamTokenizer f, Hashtable<String, Integer> sbt, int [] arg_num)
	{
		if(f.check(')'))
			return "";
		else
		{
			arg_num[0]++;
			return parseEXP(f, sbt) + parseAp(f, sbt, arg_num);
		}
	}

	
	
	
	static String parseAp(SamTokenizer f, Hashtable<String, Integer> sbt, int [] arg_num)
	{
		if(f.check(')'))
			return "";
		else if(f.check(','))
		{
			arg_num[0]++;
			return parseEXP(f, sbt) + parseAp(f, sbt, arg_num);
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
		retCounter = 0;
		envCounter = 0;
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
