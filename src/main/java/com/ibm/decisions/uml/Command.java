package com.ibm.decisions.uml;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.ibm.decisions.uml.classdiagram.ClassDiagramWriter;
import ilog.rules.bom.IlrObjectModel;
import ilog.rules.bom.dynamic.IlrDynamicObjectModel;
import ilog.rules.bom.mutable.IlrMutableObjectModel;
import ilog.rules.bom.serializer.IlrJavaSerializer;
import ilog.rules.bom.serializer.IlrSyntaxError;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ardoint on 11/04/2018.
 */
public class Command {

  @Parameter(names = {"-bom", "-model"}, description = "a BOM file")
  private String bomFile;


  @Parameter(names = {"-output"}, description = "an output file")
  private String output;


  public static void main(String[] args) {

    Command command = new Command();
    JCommander jc = new JCommander(command, args);

    command.run();

  }

  private void run() {
    try {
    Reader reader;
    boolean closeReader = false;
    if (bomFile != null) {
      reader = getReader(bomFile);
      closeReader = true;
    } else {
      reader = new BufferedReader(new InputStreamReader(System.in));
    }

    PrintWriter writer;
    boolean closeWriter = false;
    if (output != null) {
      writer = new PrintWriter(getWriter(output));
      closeWriter =true;
    } else {
      writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
    }

      ClassDiagramWriter classDiagramWriter = new ClassDiagramWriter(writer);
      classDiagramWriter.writeModel(readBOM(reader));
      if (closeReader)
        reader.close();

      if (closeWriter)
        writer.close();
      else
        writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }


  private IlrMutableObjectModel readBOM(Reader reader) throws IOException {
    IlrDynamicObjectModel bom = new IlrDynamicObjectModel(IlrObjectModel.Kind.BUSINESS);
    try {
      IlrJavaSerializer javaSerializer = new IlrJavaSerializer();
      javaSerializer.readObjectModel(bom, reader);
    } catch (IlrSyntaxError e) {
      writeError(e);
    }
    return bom;
  }

  private Reader getReader(String name) throws IOException {
    Path path = Paths.get(name);
    return Files.newBufferedReader(path, StandardCharsets.UTF_8);
  }


  private Writer getWriter(String name) throws IOException {
    Path path = Paths.get(name);
    return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
  }

  public static void writeError(IlrSyntaxError error) {
    String[] messages = error.getErrorMessages();
    for (String message : messages)
      System.err.println(message);
    System.exit(1);
  }

}
