/**
 * 
 */
package io.oneclicklabs.logger.plugin.log4j;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * @author karthy
 *
 * Sep 23, 2016
 */
public class Log4jUtility
{
  private static final String LOGGER_TYPE = "Logger";
  private static final String LOGGER_NAME = "logger";
  private static final String DEBUG_CHECK = "isDebugEnabled";
  private static final String DEBUG_METHOD = "debug";
  private ASTParser parser = ASTParser.newParser(3);
  private ICompilationUnit icu;
  private Document document;
  private CompilationUnit cu;
  
  public Log4jUtility(ICompilationUnit icu)
  {
    this.icu = icu;
  }
  
  public void generateMethodLogging(TextSelection selection)
    throws JavaModelException, MalformedTreeException, BadLocationException
  {
    updateSource();
    
    ASTNode node = getSelectedNode(selection, true);
    
    generateLog4j((node instanceof MethodDeclaration) ? node : null);
  }
  
  public void generateSelectionLogging(TextSelection selection)
    throws JavaModelException, MalformedTreeException, BadLocationException
  {
    updateSource();
    
    ASTNode node = getSelectedNode(selection, false);
    if (node == null) {
      return;
    }
    generateLog4j(node);
  }
  
  private void updateSource()
    throws JavaModelException
  {
    String source = this.icu.getSource();
    this.document = new Document(source);
    
    this.parser.setSource(this.icu);
    this.cu = ((CompilationUnit)this.parser.createAST(null));
  }
  
  private void generateLog4j(ASTNode node)
    throws JavaModelException, MalformedTreeException, BadLocationException
  {
    this.cu.recordModifications();
    
    String loggerName = createLogger();
    if ((this.cu.equals(node)) || (node == null))
    {
      parseMethods(getMethods(), loggerName);
    }
    else if ((node instanceof MethodDeclaration))
    {
      List<MethodDeclaration> methods = new ArrayList();
      methods.add((MethodDeclaration)node);
      parseMethods(methods, loggerName);
    }
    else if ((node instanceof Expression))
    {
      parseExpression((Expression)node, loggerName);
    }
    TextEdit edits = this.cu.rewrite(this.document, this.icu.getJavaProject().getOptions(true));
    
    this.icu.applyTextEdit(edits, null);
  }
  
  private ASTNode getSelectedNode(TextSelection text, boolean noNames)
  {
    if (text != null)
    {
      ASTNode node = NodeFinder.perform(this.cu, text.getOffset(), text.getLength());
      if (noNames) {
        while ((node != null) && ((node instanceof Name))) {
          node = node.getParent();
        }
      }
      return node;
    }
    return null;
  }
  
  private Block getSurroundingBlock(ASTNode node)
  {
    if (node != null)
    {
      while ((node != null) && (!(node instanceof Block))) {
        node = node.getParent();
      }
      return node == null ? null : (Block)node;
    }
    return null;
  }
  
  private String createLogger()
    throws JavaModelException
  {
    TypeDeclaration td = (TypeDeclaration)this.cu.types().get(0);
    
    FieldDeclaration[] fields = td.getFields();
    FieldDeclaration[] arrayOfFieldDeclaration1;
    int j = (arrayOfFieldDeclaration1 = fields).length;
    for (int i = 0; i < j; i++)
    {
      FieldDeclaration field = arrayOfFieldDeclaration1[i];
      
      String type = field.getType().toString();
      if ((type != null) && (type.equals("Logger")))
      {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment)field.fragments().get(0);
        return fragment.getName().toString();
      }
    }
    AST ast = this.cu.getAST();
    
    QualifiedName name = ast.newQualifiedName(ast.newQualifiedName(ast.newQualifiedName(ast.newSimpleName("org"), ast.newSimpleName("apache")), ast.newSimpleName("log4j")), ast.newSimpleName("Logger"));
    createImport(ast, name);
    
    VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
    vdf.setName(ast.newSimpleName("logger"));
    FieldDeclaration fd = ast.newFieldDeclaration(vdf);
    fd.modifiers().addAll(ASTNodeFactory.newModifiers(ast, 26));
    fd.setType(ast.newSimpleType(ast.newName("Logger")));
    
    MethodInvocation mi = ast.newMethodInvocation();
    mi.setName(ast.newSimpleName("getLogger"));
    mi.setExpression(ast.newSimpleName("Logger"));
    TypeLiteral tl = ast.newTypeLiteral();
    tl.setType(ast.newSimpleType(ast.newName(td.getName().toString())));
    mi.arguments().add(tl);
    vdf.setInitializer(mi);
    
    td.bodyDeclarations().add(0, fd);
    
    return "logger";
  }
  
  private void parseMethods(List<MethodDeclaration> methods, String loggerName)
  {
    AST ast = this.cu.getAST();
    for (MethodDeclaration md : methods)
    {
      String methodName = md.getName().toString();
      if (!md.isConstructor()) {
        if (!Modifier.isAbstract(md.getModifiers())) {
          if (!isFirstLineLogger(md, loggerName))
          {
            List statements = md.getBody().statements();
            
            Statement start = createStartBlock(ast, loggerName, md);
            
            addToBlock(statements, start, ast, loggerName, Integer.valueOf(0));
            for (ReturnStatement rs : getReturns(md)) {
              handleReturn(rs, ast, loggerName, methodName);
            }
            if (!isLastLineReturn(md))
            {
              Statement finish = createFinishBlock(ast, loggerName, methodName, null, null);
              addToBlock(statements, finish, ast, loggerName, null);
            }
          }
        }
      }
    }
  }
  
  private void parseExpression(Expression e, String loggerName)
  {
    Block b = getSurroundingBlock(e);
    if (b == null) {
      return;
    }
    AST ast = this.cu.getAST();
    IfStatement ifs = createDebugIf(ast, loggerName);
    ifs.setThenStatement(createVariableLoggingStatement(ast, loggerName, e));
    for (int i = 0; i < b.statements().size(); i++)
    {
      Statement s = (Statement)b.statements().get(i);
      if (containsNode(s, e))
      {
        addToBlock(b.statements(), ifs, ast, loggerName, Integer.valueOf(++i));
        break;
      }
    }
  }
  
  private Statement createStartBlock(AST ast, String loggerName, MethodDeclaration md)
  {
    IfStatement ifs = createDebugIf(ast, loggerName);
    
    Block b = ast.newBlock();
    
    List parameters = md.parameters();
    
    String methodName = md.getName().toString();
    
    b.statements().add(createMethodLoggingStatement(ast, loggerName, methodName, parameters));
    for (Object o : parameters) {
      b.statements().add(createVariableLoggingStatement(ast, loggerName, (SingleVariableDeclaration)o));
    }
    ifs.setThenStatement(b);
    
    return ifs;
  }
  
  private IfStatement createDebugIf(AST ast, String loggerName)
  {
    MethodInvocation mi = ast.newMethodInvocation();
    mi.setName(ast.newSimpleName("isDebugEnabled"));
    mi.setExpression(ast.newSimpleName(loggerName));
    IfStatement ifs = ast.newIfStatement();
    ifs.setExpression(mi);
    return ifs;
  }
  
  private Statement createMethodLoggingStatement(AST ast, String loggerName, String methodName, List parameters)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(parameters != null ? "entering " : "exiting ");
    sb.append(methodName);
    sb.append("(");
    sb.append(createParamString(parameters));
    sb.append(")");
    
    return createStringLoggingStatement(ast, loggerName, sb.toString());
  }
  
  private Statement createStringLoggingStatement(AST ast, String loggerName, String value)
  {
    MethodInvocation mi = createDebugCall(ast, loggerName);
    
    StringLiteral sl = ast.newStringLiteral();
    sl.setLiteralValue(value);
    mi.arguments().add(sl);
    
    return ast.newExpressionStatement(mi);
  }
  
  private String createParamString(List parameters)
  {
    if ((parameters == null) || (parameters.size() == 0)) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parameters.size(); i++)
    {
      SingleVariableDeclaration svd = (SingleVariableDeclaration)parameters.get(i);
      sb.append(svd.getType());
      if (i < parameters.size() - 1) {
        sb.append(",");
      }
    }
    return sb.toString();
  }
  
  private Statement createVariableLoggingStatement(AST ast, String loggerName, VariableDeclaration vd)
  {
    MethodInvocation mi = createDebugCall(ast, loggerName);
    
    StringLiteral sl = ast.newStringLiteral();
    StringBuilder sb = new StringBuilder();
    sb.append(vd.getName());
    sb.append(": ");
    sl.setLiteralValue(sb.toString());
    
    SimpleName sn = ast.newSimpleName(vd.getName().toString());
    
    InfixExpression ie = ast.newInfixExpression();
    ie.setOperator(InfixExpression.Operator.PLUS);
    ie.setLeftOperand(sl);
    if ((vd instanceof SingleVariableDeclaration))
    {
      SingleVariableDeclaration svd = (SingleVariableDeclaration)vd;
      if (svd.isVarargs())
      {
        QualifiedName name = ast.newQualifiedName(ast.newQualifiedName(ast.newSimpleName("java"), ast.newSimpleName("util")), ast.newSimpleName("Arrays"));
        createImport(ast, name);
        
        MethodInvocation mi2 = ast.newMethodInvocation();
        mi2.setName(ast.newSimpleName("toString"));
        mi2.setExpression(ast.newSimpleName("Arrays"));
        mi2.arguments().add(sn);
        
        ie.setRightOperand(mi2);
        mi.arguments().add(ie);
      }
      else if (svd.getType().toString().equals("String"))
      {
        sb.append("\"");
        sl.setLiteralValue(sb.toString());
        
        ie.setRightOperand(sn);
        
        InfixExpression ie2 = ast.newInfixExpression();
        ie2.setOperator(InfixExpression.Operator.PLUS);
        ie2.setLeftOperand(ie);
        
        sl = ast.newStringLiteral();
        sl.setLiteralValue("\"");
        ie2.setRightOperand(sl);
        
        mi.arguments().add(ie2);
      }
      else
      {
        ie.setRightOperand(sn);
        mi.arguments().add(ie);
      }
    }
    else
    {
      ie.setRightOperand(sn);
      mi.arguments().add(ie);
    }
    return ast.newExpressionStatement(mi);
  }
  
  private Statement createVariableLoggingStatement(AST ast, String loggerName, Expression e)
  {
    MethodInvocation mi = createDebugCall(ast, loggerName);
    
    StringLiteral sl = ast.newStringLiteral();
    StringBuilder sb = new StringBuilder();
    sb.append(e.toString());
    sb.append(": ");
    sl.setLiteralValue(sb.toString());
    
    InfixExpression ie = ast.newInfixExpression();
    ie.setOperator(InfixExpression.Operator.PLUS);
    ie.setLeftOperand(sl);
    Expression copy = (Expression)ASTNode.copySubtree(ast, e);
    if (((copy instanceof Name)) || ((copy instanceof FieldAccess)))
    {
      ie.setRightOperand(copy);
    }
    else
    {
      ParenthesizedExpression parenthesis = ast.newParenthesizedExpression();
      parenthesis.setExpression(copy);
      ie.setRightOperand(parenthesis);
    }
    mi.arguments().add(ie);
    
    return ast.newExpressionStatement(mi);
  }
  
  private Statement createFinishBlock(AST ast, String loggerName, String methodName, ReturnStatement rs, String extraLine)
  {
    IfStatement ifs = createDebugIf(ast, loggerName);
    
    Block b = ast.newBlock();
    if (extraLine != null) {
      b.statements().add(createStringLoggingStatement(ast, loggerName, extraLine));
    }
    b.statements().add(createMethodLoggingStatement(ast, loggerName, methodName, null));
    if ((rs != null) && (rs.getExpression() != null) && (isSafeReturn(rs))) {
      b.statements().add(createReturnLoggingStatement(ast, loggerName, rs));
    }
    ifs.setThenStatement(b);
    
    return ifs;
  }
  
  private Statement createReturnLoggingStatement(AST ast, String loggerName, ReturnStatement rs)
  {
    MethodInvocation mi = createDebugCall(ast, loggerName);
    
    StringLiteral sl = ast.newStringLiteral();
    sl.setLiteralValue("returning: ");
    
    Expression e = (Expression)ASTNode.copySubtree(ast, rs.getExpression());
    if ((e instanceof InfixExpression))
    {
      Expression temp = e;
      e = ast.newParenthesizedExpression();
      ((ParenthesizedExpression)e).setExpression(temp);
    }
    InfixExpression ie = ast.newInfixExpression();
    ie.setOperator(InfixExpression.Operator.PLUS);
    ie.setLeftOperand(sl);
    ie.setRightOperand(e);
    
    mi.arguments().add(ie);
    
    return ast.newExpressionStatement(mi);
  }
  
  private void handleReturn(ReturnStatement rs, AST ast, String loggerName, String methodName)
  {
    ASTNode node = rs.getParent();
    if ((node instanceof Block))
    {
      Block rBlock = (Block)node;
      List rStatements = rBlock.statements();
      
      int index = rStatements.indexOf(rs);
      
      Statement finish = createFinishBlock(ast, loggerName, methodName, rs, null);
      
      addToBlock(rStatements, finish, ast, loggerName, Integer.valueOf(index));
    }
    else if ((node instanceof IfStatement))
    {
      IfStatement rIf = (IfStatement)node;
      
      boolean then = rIf.getThenStatement().equals(rs);
      Statement s = then ? rIf.getThenStatement() : rIf.getElseStatement();
      
      Block rB = ast.newBlock();
      
      String condition = then ? rIf.getExpression().toString() : "else condition";
      
      rB.statements().add(createFinishBlock(ast, loggerName, methodName, rs, condition));
      
      rB.statements().add(ASTNode.copySubtree(ast, s));
      if (then) {
        rIf.setThenStatement(rB);
      } else {
        rIf.setElseStatement(rB);
      }
    }
  }
  
  private MethodInvocation createDebugCall(AST ast, String loggerName)
  {
    MethodInvocation mi = ast.newMethodInvocation();
    mi.setName(ast.newSimpleName("debug"));
    mi.setExpression(ast.newSimpleName(loggerName));
    return mi;
  }
  
  private boolean isFirstLineLogger(MethodDeclaration md, final String loggerName)
  {
    final FinalType<Boolean> isFirst = new FinalType();
    
    isFirst.setValue(Boolean.valueOf(false));
    
    md.getBody().accept(new ASTVisitor()
    {
      private Boolean first = null;
      
      public void preVisit(ASTNode node)
      {
        if ((this.first == null) && (!(node instanceof Block))) {
          this.first = Boolean.valueOf(node instanceof IfStatement);
        }
      }
      
      public boolean visit(IfStatement node)
      {
        if (this.first.booleanValue()) {
          if (Log4jUtility.this.isDebugStatement(node, loggerName)) {
            isFirst.setValue(Boolean.valueOf(true));
          }
        }
        return false;
      }
    });
    return ((Boolean)isFirst.getValue()).booleanValue();
  }
  
  private boolean isLastLineReturn(MethodDeclaration md)
  {
    final FinalType<Boolean> isLast = new FinalType();
    
    isLast.setValue(Boolean.valueOf(false));
    
    md.getBody().accept(new ASTVisitor()
    {
      private List<Block> blocks = new ArrayList();
      
      public void preVisit(ASTNode node)
      {
        if ((this.blocks.contains(node)) || ((node instanceof CatchClause))) {
          return;
        }
        isLast.setValue(Boolean.valueOf(((node instanceof ReturnStatement)) || ((node instanceof ThrowStatement))));
      }
      
      public boolean visit(ReturnStatement node)
      {
        return false;
      }
      
      public boolean visit(ThrowStatement node)
      {
        return false;
      }
      
      public boolean visit(CatchClause node)
      {
        return false;
      }
      
      public boolean visit(TryStatement node)
      {
        if (node.getFinally() != null) {
          this.blocks.add(node.getFinally());
        }
        return true;
      }
      
      public boolean visit(Block node)
      {
        if (this.blocks.contains(node)) {
          return false;
        }
        return true;
      }
      
      public void endVisit(TryStatement node)
      {
        this.blocks.remove(node);
      }
    });
    return ((Boolean)isLast.getValue()).booleanValue();
  }
  
  private boolean isSafeReturn(ReturnStatement rs)
  {
    final FinalType<Boolean> isSafe = new FinalType();
    
    isSafe.setValue(Boolean.valueOf(true));
    
    rs.getExpression().accept(new ASTVisitor()
    {
      public void preVisit(ASTNode node)
      {
        if (((node instanceof MethodInvocation)) || 
          ((node instanceof PostfixExpression)) || 
          ((node instanceof PrefixExpression))) {
          isSafe.setValue(Boolean.valueOf(false));
        }
      }
    });
    return ((Boolean)isSafe.getValue()).booleanValue();
  }
  
  private List<MethodDeclaration> getMethods()
  {
    final List<MethodDeclaration> methods = new ArrayList();
    
    this.cu.accept(new ASTVisitor()
    {
      public boolean visit(MethodDeclaration node)
      {
        methods.add(node);
        
        return false;
      }
    });
    return methods;
  }
  
  private List<ReturnStatement> getReturns(MethodDeclaration md)
  {
    final List<ReturnStatement> returns = new ArrayList();
    
    md.getBody().accept(new ASTVisitor()
    {
      public boolean visit(ReturnStatement node)
      {
        returns.add(node);
        
        return false;
      }
    });
    return returns;
  }
  
  private boolean containsNode(Statement s, final ASTNode n)
  {
    final FinalType<Boolean> containsNode = new FinalType();
    
    containsNode.setValue(Boolean.valueOf(false));
    
    s.accept(new ASTVisitor()
    {
      public void preVisit(ASTNode node)
      {
        if (node.equals(n)) {
          containsNode.setValue(Boolean.valueOf(true));
        }
      }
    });
    return ((Boolean)containsNode.getValue()).booleanValue();
  }
  
  private static void joinStatements(IfStatement target, IfStatement addition, AST ast)
  {
    Statement s1 = target.getThenStatement();
    Block combined;
    if ((s1 instanceof Block))
    {
      combined = (Block)s1;
    }
    else
    {
      combined = ast.newBlock();
      combined.statements().add(ASTNode.copySubtree(ast, s1));
      target.setThenStatement(combined);
    }
    Statement s2 = addition.getThenStatement();
    if ((s2 instanceof Block))
    {
      Block b = (Block)s2;
      for (Object o : b.statements()) {
        combined.statements().add(ASTNode.copySubtree(ast, (ASTNode)o));
      }
    }
    else
    {
      combined.statements().add(ASTNode.copySubtree(ast, addition.getThenStatement()));
    }
  }
  
  private boolean isDebugStatement(IfStatement ifs, String loggerName)
  {
    return (ifs != null) && (ifs.getExpression().toString().equals(loggerName + "." + "isDebugEnabled" + "()"));
  }
  
  private void addToBlock(List statements, Statement newStatement, AST ast, String loggerName, Integer index)
  {
    if (statements.size() > 0)
    {
      //int i = index.intValue() == 0 ? 1 : index == null ? statements.size() : index.intValue();
    	int i;
		if(index==null)
			i= statements.size();
    	else
    		i = index.intValue() == 0 ? 1 : index == null ? statements.size() : index.intValue();
    	
      Statement s = (Statement)statements.get(i - 1);
      if (((s instanceof IfStatement)) && (isDebugStatement((IfStatement)s, loggerName)))
      {
        joinStatements((IfStatement)s, (IfStatement)newStatement, ast);
        return;
      }
      if ((index != null) && (statements.size() > index.intValue()))
      {
        s = (Statement)statements.get(index.intValue());
        if (((s instanceof IfStatement)) && (isDebugStatement((IfStatement)s, loggerName)))
        {
          joinStatements((IfStatement)s, (IfStatement)newStatement, ast);
          return;
        }
      }
    }
    if (index == null) {
      statements.add(newStatement);
    } else {
      statements.add(index.intValue(), newStatement);
    }
  }
  
  private void createImport(AST ast, QualifiedName importName)
  {
    ImportDeclaration id = ast.newImportDeclaration();
    id.setName(importName);
    this.cu.imports().add(id);
  }
}
