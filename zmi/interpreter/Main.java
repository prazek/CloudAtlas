/**
 * Copyright (c) 2014, University of Warsaw
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package interpreter;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.Scanner;

import interpreter.query.Yylex;
import interpreter.query.parser;
import model.PathName;
import model.ValueString;
import model.ZMI;

public class Main {
	public static void main(String[] args) throws Exception {
		if(args.length > 0)
			System.setIn(new FileInputStream(args[0]));
		ZMI root = TestHierarchy.createTestHierarchy();
		Scanner scanner = new Scanner(System.in);
		scanner.useDelimiter("\\n");
		while(scanner.hasNext())
			executeQueries(root, scanner.next());
		scanner.close();
	}
	
	private static PathName getPathName(ZMI zmi) {
		String name = ((ValueString)zmi.getAttributes().get("name")).getValue();
		return zmi.getFather() == null? PathName.ROOT : getPathName(zmi.getFather()).levelDown(name);
	}
	
	private static void executeQueries(ZMI zmi, String query) throws Exception {
		if(!zmi.getSons().isEmpty()) {
			for(ZMI son : zmi.getSons())
				executeQueries(son, query);
			Interpreter interpreter = new Interpreter(zmi);
			Yylex lex = new Yylex(new ByteArrayInputStream(query.getBytes()));
			try {
				List<QueryResult> result = interpreter.interpretProgram((new parser(lex)).pProgram());
				PathName zone = getPathName(zmi);
				for(QueryResult r : result) {
					System.out.println(zone + ": " + r);
					zmi.getAttributes().addOrChange(r.getName(), r.getValue());
				}
			} catch(InterpreterException exception) {
				System.out.println(exception);
			}
		}
	}
}
