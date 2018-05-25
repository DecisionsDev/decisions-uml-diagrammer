/*
* Copyright IBM Corp. 2018
*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*
**/
package com.ibm.decisions.uml.classdiagram;

import ilog.rules.bom.*;
import ilog.rules.bom.util.IlrModelUtilities;
import ilog.rules.util.IlrVisitable;
import ilog.rules.util.IlrVisitor;

import java.io.PrintWriter;
import java.util.*;

import static ilog.rules.bom.util.IlrClassUtilities.*;

/**
 * Class diagram writer from a BOM
 * @author Jean-Louis Ardoint
 */
public class ClassDiagramWriter {

  final PrintWriter writer;

  public ClassDiagramWriter(PrintWriter writer) {
    this.writer = writer;
  }

  public void writeModel(IlrObjectModel model) {
    model.getStringClass(); // just to avoid missing reference for string
    Visitor v = new Visitor(model);

  }

  public class Visitor extends IlrVisitor {
    Set<String> excludedNamespaces = new HashSet<>();
    Set<String> inlinedAttributeTypes = new HashSet<>();
    Map<String, String> type2inheritedNode = new HashMap<>();
    Map<IlrClass, String> visitedClasses = new HashMap<>();
    final IlrObjectModel model;

    Visitor(IlrObjectModel model) {
      this.model = model;
      excludedNamespaces.add("java");
      excludedNamespaces.add("ilog.rules.xml");
      inlinedAttributeTypes.add("java.lang.Double");
      inlinedAttributeTypes.add("java.util.Float");
      inlinedAttributeTypes.add("java.util.Boolean");
      inlinedAttributeTypes.add("java.util.Integer");
      inlinedAttributeTypes.add("java.util.Short");
      inlinedAttributeTypes.add("java.util.Byte");
      inlinedAttributeTypes.add("java.util.Character");
      inlinedAttributeTypes.add("java.time.ZonedDateTime");

      type2inheritedNode.put("com.ibm.ia.model.Event", "<< (V,#ff704d) Event >>");
      type2inheritedNode.put("com.ibm.ia.model.Entity", "<< (N,#b3ffd9) Entity >>");

      visit(model.getDefaultPackage());
      writer.println();
      writer.flush();
    }


    public void inspect(IlrPackage pkg) {
      if (acceptNamespace(pkg.getDisplayName())) {
        iterateVisit(sort(pkg.getClasses()));
        iterateVisit(sort(pkg.getNestedPackages()));
      }
    }

    protected void callAccept(IlrVisitable visitable) {
      // visitable.accept(this);
    }


    private boolean acceptNamespace(String namespace) {
      if (namespace != null)
        for (String excluded : excludedNamespaces) {
          if (namespace.startsWith(excluded))
            return false;
        }
      return true;
    }

    private boolean acceptClass(IlrClass clazz) {
      return !clazz.isMissingReference()
          && acceptNamespace(clazz.getEnclosingNamespace().getDisplayName());
    }


    public void inspect(IlrClass clazz) {
      if (acceptClass(clazz)) {
        if (!visitedClasses.containsKey(clazz)) {
          visitedClasses.put(clazz, null);
          // visit super classes first
          iterateVisit(clazz.getSuperclasses());
          writeInheritance(clazz);
          writeClassHeader(clazz);
          writer.println('{');
          List<IlrAttribute> rel = writeAttributes(clazz);

          final List ctors = sortWithParameters(clazz.getConstructors());
          final List methods = sortWithParameters(clazz.getMethods());
          if (!ctors.isEmpty()) {
            writer.println("__");
            iterateVisit(ctors);
            if (!methods.isEmpty())
              writer.println("__");
          } else if (!methods.isEmpty()) {

            writer.println("__");
          }
          iterateVisit(methods);
          writer.println("}");
          writeRelations(clazz, rel);
        }
      }

    }

    protected void writeClassHeader(IlrClass clazz) {
      if (isEnumClass(clazz)) {
        writer.print("enum ");
        printType(clazz);
      } else if (isUtilityClass(clazz)) {
        writer.print("class ");
        printType(clazz);
        writer.print(" << (U,#99ccff) Utilities >>");
      } else {
        if (clazz.isInterface()) {
          writer.print("interface ");
        } else
          writer.print("class ");
        printType(clazz);
        String inherited = visitedClasses.get(clazz);
        if (inherited != null) {
          writer.print(inherited);
        }
      }
    }


    private void writeInheritance(IlrClass clazz) {
      List supers = clazz.getSuperclasses();
      if (supers != null) {
        IlrObjectModel objectModel = clazz.getObjectModel();
        for (Object aSuper : supers) {
          IlrClass superClass = (IlrClass) aSuper;
          if ((!superClass.isInterface() || clazz.isInterface())
              && !objectModel.isObjectClass(superClass)) {
            String inheritedNote = type2inheritedNode.get(superClass.getFullyQualifiedName());
            if (inheritedNote != null) {
              visitedClasses.put(clazz, inheritedNote);
            } else {
              printType(superClass);
              writer.print(" <|-- ");
              printType(clazz);
              writer.println();
            }
          }
        }
      }
    }

    private List<IlrAttribute> writeAttributes(IlrClass clazz) {
      final List attributes = clazz.getAttributes();
      if (attributes != null) {
        int size = attributes.size();
        List<IlrAttribute> result = new ArrayList<>(size);
        for (Object attribute1 : attributes) {
          IlrAttribute attribute = (IlrAttribute) attribute1;
          String inlinedTypeRepresentation = getInlinedTypeRepresentation(attribute);
          if (inlinedTypeRepresentation != null) {
            writeModifiers(attribute);
            boolean bold = isBoldAttribute(attribute);
            if (bold)
              writer.print("<b>");
            writer.print(attribute.getName());
            if (bold)
              writer.print("</b>");

            writer.print(": ");
            writer.println(inlinedTypeRepresentation);
          } else {
            result.add(attribute);
          }
        }
        return result;
      }
      return Collections.emptyList();
    }

    private boolean isBoldAttribute(IlrAttribute attribute) {
      return attribute.getPropertyValue("ia.timestamp") != null
          || attribute.getPropertyValue("ia.entity.id") != null;
    }

    private void writeRelations(IlrClass clazz, List<IlrAttribute> attributes) {
      for (IlrAttribute attribute : attributes) {
        printType(clazz);
        writer.print(" --> ");
        String note = null;
        IlrDomain domain = attribute.getLocalDomain();
        if (domain instanceof IlrCollectionDomain) {
          IlrCollectionDomain collectionDomain = (IlrCollectionDomain) domain;
          writer.print("\"");
          writer.print(getMultiplicity(collectionDomain));
          writer.print("\" ");
          if (collectionDomain.getElementType() != null) {
            printType(collectionDomain.getElementType());
          } else {
            printType(model.getObjectClass());
          }
          note = attribute.getAttributeType().getFullyQualifiedName();
        } else {
          IlrType attributeType = attribute.getAttributeType();
          if (attributeType.isArray()) {
            note = attributeType.getFullyQualifiedName();
            while (attributeType.isArray()) {
              attributeType = attributeType.getComponentType();
            }
          }
          printType(attributeType);
        }
        writer.print(" : ");
        writer.print(attribute.getName());
        writer.println();

        // add note
        if (note != null) {
          writer.println("note on link:" + note);
        }
      }
    }

    String getMultiplicity(IlrCollectionDomain domain) {
      final int min = domain.getMin();
      final int max = domain.getMax();
      if (min == max)
        return "" + min;
      if (max == IlrCollectionDomain.INFINITE)
        if (min == 0)
          return "*";
        else
          return min + "..*";

      return min + ".." + max;
    }

    private String getInlinedTypeRepresentation(IlrAttribute attribute) {
      String rep = getInlinedTypeRepresentation(attribute.getAttributeType());

      if (rep == null) {
        IlrDomain domain = attribute.getLocalDomain();
        if (domain instanceof IlrCollectionDomain) {
          IlrCollectionDomain collectionDomain = (IlrCollectionDomain) domain;
          if (collectionDomain.getElementType() != null) {
            rep =  getInlinedTypeRepresentation(collectionDomain.getElementType());
            if (rep != null) {
              return rep + " [" + getMultiplicity(collectionDomain) + "]";
            }
          }
        }
      }
      return rep;
    }

    private String getInlinedTypeRepresentation(IlrType attributeType) {

      if (attributeType.isPrimitiveType()
          || attributeType == model.getStringClass()
          || inlinedAttributeTypes.contains(attributeType.getFullyQualifiedName())
          || isEnumClass(attributeType))
          return getShortName(attributeType);

       if(attributeType.isArray()) {
         String rep = getInlinedTypeRepresentation(attributeType.getComponentType());
         if (rep != null)
           return rep+"[]";
       };
       return null;
    }



    void printType(IlrType type) {
      String FQN = type.getFullyQualifiedName();
      String shortName = model.getShortname(FQN);
      writer.print(shortName);
    }

    void printShortType(IlrType type) {
        writer.print(getShortName(type));
    }

    String getShortName(IlrType type) {

      String FQN = type.getFullyQualifiedName();
      String shortName = model.getShortname(FQN);
      if (shortName != FQN)
        return shortName;
      else
        return type.getShortDisplayName();
    }

    List sort(List list) {
      if (list != null) {
        ArrayList newList = new ArrayList(list);
        Collections.sort(newList, IlrModelUtilities.ModelElementComparator);
        return newList;
      }
      return Collections.emptyList();
    }

    List sortWithParameters(List list) {
      if (list != null) {
        ArrayList newList = new ArrayList(list);
        Collections.sort(newList, IlrModelUtilities.MemberWithParametersComparator);
        return newList;
      }
      return Collections.emptyList();

    }

    public void inspect(IlrMethod method) {
      //if (!acceptsMethod(method))
      //  return;
      IlrType retType = method.getReturnType();
      writeModifiers(method);
      //writeMethodModifiers(method);
      String methodName = method.getName();
      if (method.getGenericInfo() != null) {
        IlrType[] typeParameters = method.getGenericInfo().getTypeParameters();
        GenericSignatureWriter gwriter = new GenericSignatureWriter(method);
        gwriter.write(typeParameters);
        writer.print(gwriter.toString());
        writer.print(' ');
      }
      if (method.isConstructor()) {
        writer.print(methodName);
      } else if (method.isOperator()) {
        if (!method.getReturnType().getFullyQualifiedName().equals(methodName))
          printType(retType);
        writer.print(' ');
        writer.print("operator ");
        writer.print(methodName);
      } else {
        // regular method
        printType(retType);

        writer.print(' ');
        writer.print(methodName);
      }
      writer.print('(');

      writeParameterList(method.getParameters(), method.isVarArgs());
      writer.println(')');

      /*
      writeThrowsClause(method.getMethodExceptions());
      printMemberFooter(method);
      */
    }

    void writeModifiers(IlrMember member) {
      if (member.isStatic())
        writer.print("{static} ");
      //if (member.)
    }

    void writeParameterList(List parameters, boolean isVarArgs) {
      if (parameters != null) {

        int count = parameters.size();
        int length = 0;
        for (int i = 0; i < count; ++i) {
          if (i > 0)
            writer.print(",\n\\t");
          IlrParameter param = (IlrParameter) parameters.get(i);
          if (isVarArgs && i == count - 1) {
            printShortType(param.getParameterType().getComponentType());
            writer.print("...");
          } else
            printShortType(param.getParameterType());
          writer.print(' ');
          writer.print(param.getName());
          /* todo
          if (param.getParameterDomain() != null) {
            space().printsp(DOMAIN);
            visit(param.getParameterDomain());
          }
          */
        }
      }
    }

  }


}
