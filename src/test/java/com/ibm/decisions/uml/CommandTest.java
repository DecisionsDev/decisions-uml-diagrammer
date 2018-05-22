package com.ibm.decisions.uml;

import com.ibm.decisions.uml.classdiagram.ClassDiagramTest;
import junit.framework.TestCase;

import java.io.*;
import java.net.URL;


/**
 * Created by ardoint on 11/04/2018.
 */
public class CommandTest extends TestCase {
  String loanValidation = "com/ibm/decisions/uml/classdiagram/loanvalidation.bom";

  public void testLoanValidation() throws Exception {
    String direct = ClassDiagramTest.getClassDiagramFromBOM(loanValidation);

    URL url = CommandTest.class.getClassLoader().getResource(loanValidation);

    PrintStream previousOut = System.out;
    ByteArrayOutputStream newOut = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(newOut));
      Command.main(new String[]{"-bom", url.getFile()});
      System.setOut(previousOut);
    } catch (Exception e) {
      throw e;
    }
    assertEquals(direct, newOut.toString());
  }

}
