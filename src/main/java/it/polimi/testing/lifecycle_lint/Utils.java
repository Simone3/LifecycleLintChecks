package it.polimi.testing.lifecycle_lint;

import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.JavaContext;

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
    public final static String ON_START_METHOD = "onStart";
    public final static String ON_STOP_METHOD = "onStop";

    public static final String CONTEXT_WRAPPER = "android.content.ContextWrapper";
    public static final String FRAGMENT_APP = "android.app.Fragment";
    public static final String FRAGMENT_SUPPORT = "android.support.v4.app.Fragment";

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
    public static String getCallerMethodName(MethodInvocation methodInvocation)
    {
        MethodDeclaration methodDeclaration = getCallerMethod(methodInvocation);
        if(methodDeclaration==null) return "";
        else return methodDeclaration.astMethodName().astValue();
    }

    /**
     * Gets the method in the class that originated the call to the given method
     * @param methodInvocation the method invocation we are interested in
     * @return the method that called the given method
     */
    private static MethodDeclaration getCallerMethod(MethodInvocation methodInvocation)
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
        return methodDeclaration;
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

    /**
     * Helper that calls getCallerMethod() and then resolves the returned value
     * @param context the context of the lint request
     * @param methodInvocation the method invocation
     * @return the resolved node of the caller method
     */
    private static JavaParser.ResolvedMethod getCallerResolvedMethod(JavaContext context, MethodInvocation methodInvocation)
    {
        // Get class method that contains the given invocation
        MethodDeclaration methodDeclaration = getCallerMethod(methodInvocation);
        if(methodDeclaration==null) return null;

        // Resolve node
        JavaParser.ResolvedNode resolved = context.resolve(methodDeclaration);
        if(resolved==null || !(resolved instanceof JavaParser.ResolvedMethod))
        {
            return null;
        }
        else
        {
            return (JavaParser.ResolvedMethod) resolved;
        }
    }

    /**
     * Checks if the given method is called inside an activity
     * @param context the context of the lint request
     * @param methodInvocation the method invocation
     * @return true if the given method is called inside an activity
     */
    public static boolean isCalledInActivity(JavaContext context, MethodInvocation methodInvocation)
    {
        // Get class method that contains the given invocation
        JavaParser.ResolvedMethod method = getCallerResolvedMethod(context, methodInvocation);
        if(method==null)
        {
            return false;
        }

        // Check if it's an activity
        return Utils.isMethodContainedInSubclassOf(method, CONTEXT_WRAPPER);
    }

    /**
     * Checks if the given method is called inside a fragment
     * @param context the context of the lint request
     * @param methodInvocation the method invocation
     * @return true if the given method is called inside a fragment
     */
    public static boolean isCalledInFragment(JavaContext context, MethodInvocation methodInvocation)
    {
        // Get class method that contains the given invocation
        JavaParser.ResolvedMethod method = getCallerResolvedMethod(context, methodInvocation);
        if(method==null)
        {
            return false;
        }

        // Check if it's a fragment
        return Utils.isMethodContainedInSubclassOf(method, FRAGMENT_APP) ||
                Utils.isMethodContainedInSubclassOf(method, FRAGMENT_SUPPORT);
    }

    /**
     * Checks if the given method is called inside a fragment or an activity
     * @param context the context of the lint request
     * @param methodInvocation the method invocation
     * @return true if the given method is called inside a fragment or an activity
     */
    public static boolean isCalledInActivityOrFragment(JavaContext context, MethodInvocation methodInvocation)
    {
        // Get class method that contains the given invocation
        JavaParser.ResolvedMethod method = getCallerResolvedMethod(context, methodInvocation);
        if(method==null)
        {
            return false;
        }

        // Check if it's a fragment or an activity
        return Utils.isMethodContainedInSubclassOf(method, CONTEXT_WRAPPER) ||
                Utils.isMethodContainedInSubclassOf(method, FRAGMENT_APP) ||
                Utils.isMethodContainedInSubclassOf(method, FRAGMENT_SUPPORT);
    }
}
