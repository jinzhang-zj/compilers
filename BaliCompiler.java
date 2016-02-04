import java.io.BufferedWriter;
import java.lang.*;
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
	
	//We have a hashtable called methodname, which stores the number of arguments in method definition
	//Each time a new method is defined, its name is added to this table, together with the number of arguments
	//This serves two purposes: 
	//first, it helps us distinguish between variables and method calls 
	//Secondly, it helps us catch errors where the number of arguments in function call are not same as the number of arguments in the function definition
	static Hashtable<String, Integer> methodname;
	
	//We have a variable loop which indicates whether, at any point, we are within a loop.
	//This is necessary to ensure that there are no break statements outside the loop.
	static int loop;
	
	//Next, we have variables to count the number of if/while statements. 
	//These are required to generate labels for the JUMP statements corresponding to if/while.
	//We also count the number of return statements to ensure that every method has a return statement.
	static int ilabelCounter, wlabelCounter, retCounter;
	
	
	static boolean hasReturnStmt;
	
	static boolean insideWhile;
	
	//Finally, we have a String variable mname that holds the name of the currently parsed method.
	static String mname;
	
	static boolean checkReservedKeyword(String word)
	{
		if(word.equals("int") || word.equals("true") || word.equals("false") || word.equals("if") || word.equals("break") || word.equals("else") || word.equals("while") || word.equals("return"))
			return true;
		else 
			return false;
	}
	
	
	
	
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
		mname = f.getWord();
		//mname = methodName;
		
		if(checkReservedKeyword(mname))
			throw new TokenizerException("Line number "+f.lineNo()+" : "+ mname +" is a keyword");
		
		methodname.put(mname, 1);

		pgm += mname + ":\n";

		if (!f.check ('(')) 
			throw new TokenizerException("Line number " + f.lineNo() + " : Missing open parenthesis");

		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		String formals = parseFp(f, sbt); 
		int arg_num = sbt.size();
		methodname.put(mname, arg_num);

		Set<String> keys = sbt.keySet();
		for (String key: keys)
			sbt.put(key, sbt.get(key) - arg_num);

		pgm += parseB(f, sbt);
		pgm += mname + "End:\n";
		pgm += "\tSTOREOFF -" + (arg_num + 1) + "\n";
		int local = 0;
		for (String key: keys)
			if (sbt.get(key) > 0)
				local++;
		if (local != 0)
			pgm += "\tADDSP -" + local + "\n";
		pgm += "\tJUMPIND\n";

		if (retCounter == 0)
			throw new TokenizerException("Missing return statement for method " + mname);

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
		String ID = f.getWord();
		if(checkReservedKeyword(ID))
			throw new TokenizerException("Line number "+f.lineNo()+" : "+ ID +" is a keyword");
		int idx = sbt.size();
		sbt.put(ID, idx);
		return ID + parseTIDp(f, sbt);
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
			throw new TokenizerException("Line number " + f.lineNo() + " : , missing");
		if (!f.check("int"))
			throw new TokenizerException("Line number " + f.lineNo() + " : Missing Formal Type");
		
		String ID = f.getWord();
		if(checkReservedKeyword(ID))
			throw new TokenizerException("Line number "+f.lineNo()+" : "+ ID +" is a keyword");
		else if (sbt.containsKey(ID) || methodname.containsKey(ID))
			throw new TokenizerException("Line number "+f.lineNo() + " : Variable name " + ID + " is already used");
		int idx = sbt.size();
		
		sbt.put(ID, idx);
		return "";
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
		if(checkReservedKeyword(var))
			throw new TokenizerException("Line number " + f.lineNo() + " : " + var + " is a reserved keyword");
		else if(methodname.containsKey(var) || sbt.containsKey(var))
		    throw new TokenizerException("Line number " + f.lineNo() + " : Variable name " + var + " already used");
		else 
		{
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
			sbt.put(var, s - arg_num + 2);
			
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
				throw new TokenizerException("Line number " + f.lineNo() + " : ; expected");
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
					int temp_retCounter_1 = retCounter;
					
					String parsedS1 = parseS(f, sbt);
					
					//diff_retCounter_1 is used to check if parsedS has a return statement
					int diff_retCounter_1 = retCounter - temp_retCounter_1;
					
					if(f.check("else"))
					{
						int temp_retCounter_2 = retCounter;
						
						String parsedS2 = parseS(f, sbt);
						ifs += parsedS2;
						
						//diff_retCounter_2 is used to check if parsedS2 has a return statement
						int diff_retCounter_2 = retCounter - temp_retCounter_2;
						
						
						//If either parsedS1 or parsedS2 did not contain a return statement, then this if-else also doesn't contain a return.  
						//Therefore, if either diff_retCounter1 or diff_retCounter2 are 0, then retCounter is set to the value it had before start of if-else.
						retCounter = temp_retCounter_1 + Math.min(diff_retCounter_1, diff_retCounter_2);
						
						ifs += "\tJUMP newLabel" + l2 + "\n";
						ifs += "newLabel" + l1 + ":\n";
						ifs += parsedS1;
						ifs += "newLabel" + l2 + ":\n";
						return ifs;
					}
					else
						throw new TokenizerException("Line number " + f.lineNo() + ": Missing else statment");
				}
				else
					throw new TokenizerException("Line number " + f.lineNo() + ": ) expected at end of if expression");
			}
			else
				throw new TokenizerException("Line number " + f.lineNo() + ": ( expected at start of if expression");
		}
		else if (f.check("while"))
		{
			loop++;
			int temp_retCounter = retCounter;
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
					
					//A return statement inside while should not increase the return statement count
					//since it is possible that this while never gets executed.
					//Therefore, it is set to the value it had before start of while loop.
					retCounter = temp_retCounter;
					
					return whs;
				}
				else
					throw new TokenizerException("Line number " + f.lineNo() + ": Missing )");
			}
			else
				throw new TokenizerException("Line number " + f.lineNo() + ": Missing (");
		}
		else if (f.check("break"))
		{
			if (loop <= 0)
				throw new TokenizerException("Line number " + f.lineNo() + ": Break outside the loop");
			else if(f.check(';'))
				return "\tJUMP Label" + (wlabelCounter - 1) + "\n";
			else throw new TokenizerException("Line number " + f.lineNo() + " : ; expected");
		}

		//Handling the case when S is an assignment statement
		String var = f.getWord();
		
		if(checkReservedKeyword(var))
			throw new TokenizerException("Line number "+f.lineNo()+ " : "+var + " is a reserved keyword");
		else if (methodname.containsKey(var))
			throw new TokenizerException("Line number "+f.lineNo()+ " : "+var+" is a method name");
		else if(!sbt.containsKey(var))
			throw new TokenizerException("Line number "+f.lineNo()+ " : Variable undefined");

		if(f.check('='))
		{
			String parsedEXP = parseEXP(f, sbt);
			if(f.check(';'))
				return parsedEXP + "\tSTOREOFF "+sbt.get(var)+"\n";
			else
				throw new TokenizerException("Line number " + f.lineNo() + " : ; expected");
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
			
			if(checkReservedKeyword(ID))
				throw new TokenizerException("Line number "+f.lineNo()+ " : "+ ID + " is a reserved keyword");

			else if(methodname.containsKey(ID))
			{
				String mcall = "";
				mcall += "\tPUSHIMM 0\n";
				if(f.check('('))
				{
					//@JIN why is this an array? Can't we just do with one arg_num?
					int [] arg_num = {0};
					mcall += parseA(f, sbt, arg_num);
					
					//Output error if the number of arguments in function call not same as number of arguments in function definition
					if(arg_num[0] !=methodname.get(ID))
						throw new TokenizerException("Line number " + f.lineNo() + " : Wrong number of arguments to " + ID);
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
		else throw new TokenizerException("Line number " + f.lineNo() + " : Invalid function call - perhaps a missing , or )");
	}

	
	
	
	public static void main(String []args)
	{
		// First argument is input file
		// Second argument is output file
		loop = 0;
		ilabelCounter = 0;
		wlabelCounter = 0;
		retCounter = 0;
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



// TODO An incorrect statement like e=f(5)+1; gives the wrong error message (says semicolon missing).

// Typos & Bugs: At one place, sbt.put had s-arg_num+1 instead of s-arg_num+2. Fixed that.