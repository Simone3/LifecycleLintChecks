package it.polimi.testing.lifecycle_lint;

import com.android.tools.lint.client.api.JavaParser;

import lombok.ast.Expression;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.StrictListAccessor;

/**
 * Some utilities for the lint detectors
 */
public class Utils
{
    public final static String ON_SAVE_INSTANCE_STATE_METHOD = "onSaveInstanceState";

    /**
     * Checks if a method belongs to the given class (or one of its subclasses)
     * @param method the method to check
     * @param className the expected class name
     * @return true if the method is defined in a subclass of the class
     */
    public static boolean isMethodContainedInSubclassOf(JavaParser.ResolvedMethod method, String className)
    {
        return method.getContainingClass().isSubclassOf(className, false);
    }

    /**
     * Gets the method in the class that originated the call to the given method
     * @param methodInvocation the method invocation we are interested in
     * @return the name of the method that called the given method
     */
    public static String getCallerMethod(MethodInvocation methodInvocation)
    {
        MethodDeclaration methodDeclaration = null;
        Node parent = methodInvocation;
        while(true)
        {
            parent = parent.getParent();

            if(parent==null) break;

            if(parent instanceof MethodDeclaration)
            {
                methodDeclaration = (MethodDeclaration) parent;
                break;
            }
        }
        if(methodDeclaration==null) return "";
        else return methodDeclaration.astMethodName().astValue();
    }

    /**
     * Gets the i-th variable/constant name passed as the method parameters
     * @param methodInvocation the method invocation
     * @param i the argument index
     * @return i-th variable/constant name passed as the method parameters
     */
    public static String getMethodInvocationArgumentName(MethodInvocation methodInvocation, int i)
    {
        StrictListAccessor<Expression, MethodInvocation> nodes = methodInvocation.astArguments();
        if(nodes!=null)
        {
            int c = 0;
            for(Node node : nodes)
            {
                if(i==c) return node.toString();
                c++;
            }
        }
        return "";
    }
}
