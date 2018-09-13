/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.metrics.impl.visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTAllocationExpression;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTLiteral;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryPrefix;
import net.sourceforge.pmd.lang.java.ast.ASTPrimarySuffix;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.ast.JavaParserVisitorReducedAdapter;
import net.sourceforge.pmd.lang.java.symboltable.ClassScope;
import net.sourceforge.pmd.lang.java.symboltable.MethodNameDeclaration;
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration;
import net.sourceforge.pmd.lang.symboltable.NameDeclaration;
import net.sourceforge.pmd.lang.symboltable.Scope;

/**
 * Returns the map of method names to the set of local attributes accessed when visiting a class.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class AtfmAttributeAccessCollector extends JavaParserVisitorReducedAdapter {

    private final ASTAnyTypeDeclaration exploredClass;


    /** The name of the current method. */
    private String currentMethodName;

    private Map<String, Map<String,Set<String>>> methodAttributeAccess;


    public AtfmAttributeAccessCollector(ASTAnyTypeDeclaration exploredClass) {
        this.exploredClass = exploredClass;
    }


    /**
     * Collects the attribute accesses by method into a map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String,Set<String>>> start() {
        return (Map<String, Map<String,Set<String>>>) this.visit(exploredClass, new HashMap<String, Set<String>>());
    }


    @Override
    public Object visit(ASTAnyTypeDeclaration node, Object data) {
        if (Objects.equals(node, exploredClass)) {
            methodAttributeAccess = new HashMap<>();
            super.visit(node, data);
        } else if (node instanceof ASTClassOrInterfaceDeclaration
                && ((ASTClassOrInterfaceDeclaration) node).isLocal()) {
            super.visit(node, data);
        }
        return methodAttributeAccess;
    }


    @Override
    public Object visit(ASTMethodDeclaration node, Object data) {

        if (!node.isAbstract()) {
            if (node.getFirstParentOfType(ASTAnyTypeDeclaration.class) == exploredClass) {
                currentMethodName = node.getQualifiedName().getOperation();
                super.visit(node, data);
                currentMethodName = null;
            } else {
                super.visit(node, data);
            }
        }

        return null;
    }


    @Override
    public Object visit(ASTConstructorDeclaration node, Object data) {
        return data; // we're only looking for method pairs
    }


    /**
     * The primary expression node is used to detect access
     * to attributes and method calls. If the access is not for a
     * foreign class, then the {@link #methodAttributeAccess}
     * map is updated for the current method.
     */
    @Override
    public Object visit(ASTPrimaryExpression node, Object data) {
        if (currentMethodName != null) {
            String variableName = getMethodOrAttributeName(node);
            if(isForeignAttributeOrMethod(node)) {
                String className = getClassName(node);
                if(!this.filterTypes(className)) {
                    Map<String, Set<String>> methodAccess = methodAttributeAccess.get(currentMethodName);
                    if (methodAccess == null)
                        methodAccess = new HashMap<>();
                    Set<String> res = methodAccess.get(className);
                    if (res == null)
                        res = new HashSet<>();
                    res.add(variableName);
                    methodAccess.put(className, res);
                    methodAttributeAccess.put(currentMethodName, methodAccess);
                }
            }
        }


        return super.visit(node, data);
    }

    private ASTCompilationUnit getTopParent(Node node){
        if(!(node instanceof ASTCompilationUnit)){
            return getTopParent(node.jjtGetParent());
        }else {
            return (ASTCompilationUnit)node;
        }
    }
    private String getClassName(ASTPrimaryExpression node) {
        ASTCompilationUnit topParent = getTopParent(node);
        Map<String,String> imported = topParent.getClassTypeResolver().getImportedClasses();
        String arr[] = topParent.getPackageDeclaration().getPackageNameImage().split("\\.");
        String filter = null;
        if(arr.length>1){
            filter = arr[0]+"."+arr[1];
        }else if(arr.length>0){
            filter = arr[0];
        }

        ASTPrimaryPrefix prefix = node.getFirstDescendantOfType(ASTPrimaryPrefix.class);
        ASTName name = prefix.getFirstDescendantOfType(ASTName.class);
        if(name == null) return null;
        NameDeclaration nd = name.getNameDeclaration();
        String className = null;

        if (nd != null && nd instanceof VariableNameDeclaration) {
            Class t = ((VariableNameDeclaration) nd).getType();
            String ti = ((VariableNameDeclaration) nd).getTypeImage();
            if(t != null){
                className = t.getName();
            }else if(imported.get(ti)!=null){
                className = imported.get(ti);
            }else {
                className = ((VariableNameDeclaration) nd).getTypeImage();
            }
        }else if(nd!=null && nd instanceof MethodNameDeclaration){
            return className;
        }
        else{
            return className;
        }

        if(className.contains(filter))
        {
            return className;
        }else{
            return null;
        }
//        return className;
    }
    private String getMethodOrAttributeName(ASTPrimaryExpression node) {
        ASTPrimaryPrefix prefix = node.getFirstDescendantOfType(ASTPrimaryPrefix.class);
        ASTName name = prefix.getFirstDescendantOfType(ASTName.class);

        String methodOrAttributeName = null;

        if (name != null) {
            int dotIndex = name.getImage().indexOf(".");
            if (dotIndex > -1) {
                methodOrAttributeName = name.getImage().substring(dotIndex + 1);
            }
        }

        return methodOrAttributeName;
    }

    private String getVariableName(ASTPrimaryExpression node) {
        ASTPrimaryPrefix prefix = node.getFirstDescendantOfType(ASTPrimaryPrefix.class);

        if (prefix.usesThisModifier()) {
            List<ASTPrimarySuffix> suffixes = node.findChildrenOfType(ASTPrimarySuffix.class);
            if (suffixes.size() > 1) {
                if (!suffixes.get(1).isArguments()) { // not a method call
                    return suffixes.get(0).getImage();
                }
            }
        }

        ASTName name = prefix.getFirstDescendantOfType(ASTName.class);

        String variableName = null;

        if (name != null) {
            int dotIndex = name.getImage().indexOf(".");
            if (dotIndex == -1) {
                variableName = name.getImage();
            } else {
                variableName = name.getImage().substring(0, dotIndex);
            }
        }

        return variableName;
    }


    private String getClassName(String varName, Scope scope) {
        Scope currentScope = scope;

        while (currentScope != null) {
            for (VariableNameDeclaration decl : currentScope.getDeclarations(VariableNameDeclaration.class).keySet()) {
                if (decl.getImage().equals(varName)) {
                    if (currentScope instanceof ClassScope){
                        return ((ClassScope) currentScope).getClassDeclaration().getName();
                    }
                }
            }
            currentScope = currentScope.getParent(); // WARNING doesn't consider inherited fields or static imports
        }
        return null;
    }


    private boolean isMethodCall(ASTPrimaryExpression node) {
        boolean result = false;
        List<ASTPrimarySuffix> suffixes = node.findDescendantsOfType(ASTPrimarySuffix.class);
        if (suffixes.size() == 1) {
            result = suffixes.get(0).isArguments();
        }
        return result;
    }


    private boolean isForeignAttributeOrMethod(ASTPrimaryExpression node) {
        boolean result;
        String nameImage = getNameImage(node);

        if (nameImage != null && (!nameImage.contains(".") || nameImage.startsWith("this."))) {
            result = false;
        } else if (nameImage == null && node.getFirstDescendantOfType(ASTPrimaryPrefix.class).usesThisModifier()) {
            result = false;
        } else if (nameImage == null && node.hasDescendantOfAnyType(ASTLiteral.class, ASTAllocationExpression.class)) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }


    private String getNameImage(ASTPrimaryExpression node) {
        ASTPrimaryPrefix prefix = node.getFirstDescendantOfType(ASTPrimaryPrefix.class);
        ASTName name = prefix.getFirstDescendantOfType(ASTName.class);

        String image = null;
        if (name != null) {
            image = name.getImage();
        }
        return image;
    }



    private boolean isAttributeAccess(ASTPrimaryExpression node) {
        return !node.hasDescendantOfType(ASTPrimarySuffix.class);
    }

    /**
     * Filters variable type - we don't want primitives, wrappers, strings, etc.
     * This needs more work. I'd like to filter out super types and perhaps
     * interfaces
     *
     * @param variableType
     *            The variable type.
     * @return boolean true if variableType is not what we care about
     */
    private boolean filterTypes(String variableType) {
        if(variableType == null) return true;
        variableType = variableType.replace("[L","").replace("[C","").replace("[B","").replace("[I","");

        return variableType.equals("") || variableType.startsWith("java.") || variableType.startsWith("javax.") || variableType.startsWith("org.apache.commons") || "String".equals(variableType)
                || filterPrimitivesAndWrappers(variableType);
    }

    /**
     * @param variableType
     *            The variable type.
     * @return boolean true if variableType is a primitive or wrapper
     */
    private boolean filterPrimitivesAndWrappers(String variableType) {
        return "Logger".equals(variableType) ||  "int".equals(variableType) || "Integer".equals(variableType) || "char".equals(variableType)
                || "Character".equals(variableType) || "double".equals(variableType) || "long".equals(variableType)
                || "short".equals(variableType) || "float".equals(variableType) || "byte".equals(variableType)
                || "boolean".equals(variableType);
    }


}
