package it.polimi.testing.lifecycle_lint.detectors;

import com.android.annotations.NonNull;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polimi.testing.lifecycle_lint.Utils;
import lombok.ast.AstVisitor;
import lombok.ast.Catch;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Try;
import lombok.ast.TypeReference;

import static com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import static com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import static com.android.tools.lint.detector.api.JavaContext.getParentOfType;

public class BroadcastReceiverDetector extends Detector implements Detector.JavaScanner
{
    // Issue implementation
    private static final Class<? extends Detector> DETECTOR_CLASS = BroadcastReceiverDetector.class;
    private static final EnumSet<Scope> DETECTOR_SCOPE = Scope.JAVA_FILE_SCOPE;
    private static final Implementation IMPLEMENTATION = new Implementation(
        DETECTOR_CLASS,
        DETECTOR_SCOPE
    );

    // Issue description
    private static final String ISSUE_ID = "BroadcastReceiverLifecycle";
    private static final String ISSUE_DESCRIPTION = "Incorrect `BroadcastReceiver` lifecycle handling";
    private static final String ISSUE_EXPLANATION = "Calls to register and unregister of BroadcastReceiver components should be done carefully, "+
                                                    "i.e. you should avoid unregistering twice (otherwise you'll receive an exception), always "+
                                                    "unregister it to avoid leaks, etc.";
    private static final String MORE_INFO_URL = "https://developer.android.com/reference/android/content/BroadcastReceiver.html";

    // Issue category
    private static final Category ISSUE_CATEGORY = Category.PERFORMANCE;
    private static final int ISSUE_PRIORITY = 5;
    private static final Severity ISSUE_SEVERITY = Severity.WARNING;

    // Issue
    public static final Issue ISSUE = Issue.create
    (
        ISSUE_ID,
        ISSUE_DESCRIPTION,
        ISSUE_EXPLANATION,
        ISSUE_CATEGORY,
        ISSUE_PRIORITY,
        ISSUE_SEVERITY,
        IMPLEMENTATION
    ).addMoreInfo(MORE_INFO_URL);

    // Methods and classes related to the issue
    public static final String LOCAL_BROADCAST_MANAGER = "android.support.v4.content.LocalBroadcastManager";
    private static final String REGISTER_METHOD = "registerReceiver";
    private static final String UNREGISTER_METHOD = "unregisterReceiver";

    // Data used during the search
    private static Map<String, MethodInvocation> registrations = new HashMap<>();
    private static Map<String, List<MethodInvocation>> unregistrations = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnumSet<Scope> getApplicableFiles()
    {
        return Scope.JAVA_FILE_SCOPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes()
    {
        return Collections.singletonList(
            MethodInvocation.class
        );
    }

    /**
     * {@inheritDoc}
     *
     * Here, for every file, we check that registrations and unregistrations are consistent
     */
    @Override
    public void afterCheckFile(@NonNull Context c)
    {
        if(!(c instanceof JavaContext)) return;
        JavaContext context = (JavaContext) c;

        // Create issue if we found a register but no unregister for a given variable
        for(Map.Entry<String, MethodInvocation> entry: registrations.entrySet())
        {
            if(!unregistrations.keySet().contains(entry.getKey()))
            {
                context.report(ISSUE, entry.getValue(), context.getLocation(entry.getValue().astName()), "Found a `BroadcastReceiver` `"+REGISTER_METHOD+"()` but no `"+UNREGISTER_METHOD+"()` calls in the class");
            }
        }

        // For each variable unregistered...
        for(Map.Entry<String, List<MethodInvocation>> entry: unregistrations.entrySet())
        {
            // If we found more than one unregister...
            if(entry.getValue()!=null && entry.getValue().size()>1)
            {
                // Issue for those that are not inside a try/catch
                for(MethodInvocation methodInvocation: entry.getValue())
                {
                    if(methodInvocation!=null)
                    {
                        context.report(ISSUE, methodInvocation, context.getLocation(methodInvocation.astName()), "Multiple `"+UNREGISTER_METHOD+"()` detected: it is advisable to catch `IllegalArgumentException` in each of them, otherwise if they are called in sequence the application will crash");
                    }
                }
            }
        }

        // Reset variables for next files
        registrations.clear();
        unregistrations.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context)
    {
        return new BroadcastReceiverVisitor(context);
    }

    /**
     * Custom AST Visitor that receives method invocation calls
     */
    private static class BroadcastReceiverVisitor extends ForwardingAstVisitor
    {
        private final JavaContext context;

        /**
         * Constructor
         * @param context the context of the lint request
         */
        public BroadcastReceiverVisitor(JavaContext context)
        {
            this.context = context;
        }

        /**
         * Getter
         * @return the names of the methods we are interested in
         */
        private List<String> getApplicableMethodNames()
        {
            return Arrays.asList(REGISTER_METHOD, UNREGISTER_METHOD);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean visitMethodInvocation(MethodInvocation methodInvocation)
        {
            // If this is a library project not being analyzed, ignore it
            if(!context.getProject().getReportIssues())
            {
                return false;
            }

            // Only applicable methods (filter before resolving, for performance)
            if(!getApplicableMethodNames().contains(methodInvocation.astName().astValue()))
            {
                return false;
            }

            // Resolve node
            ResolvedNode resolved = context.resolve(methodInvocation);
            if(resolved==null || !(resolved instanceof ResolvedMethod))
            {
                return false;
            }

            // Check if we are interested in the class that contains this method
            ResolvedMethod method = (ResolvedMethod) resolved;
            if(!isContainingClassValid(method))
            {
                return false;
            }

            // Set flag and save some data if it's the register method
            String name = method.getName();
            if(REGISTER_METHOD.equals(name))
            {
                registrations.put(Utils.getMethodInvocationArgumentName(methodInvocation, 0), methodInvocation);
            }

            // If it's the unregister method...
            else if(UNREGISTER_METHOD.equals(name))
            {
                String broadcastReceiverVariable = Utils.getMethodInvocationArgumentName(methodInvocation, 0);

                // Check if the unregistration is inside a try/catch block
                boolean isInTryCatch;
                Node parent = methodInvocation;
                whileLoop: while(true)
                {
                    Try tryCatch = getParentOfType(parent, Try.class);
                    if(tryCatch==null)
                    {
                        isInTryCatch = false;
                        break;
                    }
                    else
                    {
                        for(Catch aCatch: tryCatch.astCatches())
                        {
                            TypeReference typeReference = aCatch.astExceptionDeclaration().astTypeReference();
                            JavaParser.TypeDescriptor typeDescriptor = context.getType(typeReference);
                            if(typeDescriptor!=null &&
                                    (typeDescriptor.matchesSignature("java.lang.IllegalArgumentException") ||
                                    typeDescriptor.matchesSignature("java.lang.RuntimeException") ||
                                    typeDescriptor.matchesSignature("java.lang.Exception") ||
                                    typeDescriptor.matchesSignature("java.lang.Throwable")))
                            {
                                isInTryCatch = true;
                                break whileLoop;
                            }
                        }
                        parent = tryCatch;
                    }
                }

                // Save unregistration in global field (need node handle only if it's not in a try/catch)
                List<MethodInvocation> list = null;
                if(unregistrations.containsKey(broadcastReceiverVariable))
                {
                    list = unregistrations.get(broadcastReceiverVariable);
                }
                if(list==null)
                {
                    list = new ArrayList<>();
                    unregistrations.put(broadcastReceiverVariable, list);
                }
                list.add(isInTryCatch ? null : methodInvocation);

                // Issue if this is called during onSaveInstanceState
                if(isCalledDuringOnSaveInstanceState(methodInvocation))
                {
                    context.report(ISSUE, methodInvocation, context.getLocation(methodInvocation.astName()), "You should not call `"+UNREGISTER_METHOD+"()` during `"+Utils.ON_SAVE_INSTANCE_STATE_METHOD+"()` because it won't be called if the user moves back in the history stack");
                }
            }

            return super.visitMethodInvocation(methodInvocation);
        }

        /**
         * Checks if we are analyzing the "onSaveInstance" method
         * @param methodInvocation the method invocation we are interested in
         * @return true if we are analyzing the "onSaveInstance" method
         */
        private boolean isCalledDuringOnSaveInstanceState(MethodInvocation methodInvocation)
        {
            return Utils.ON_SAVE_INSTANCE_STATE_METHOD.equals(Utils.getCallerMethodName(methodInvocation));
        }

        /**
         * Checks if we are interested in the containing class of the method
         * @param method the method to check
         * @return true if we are interested in the containing class of the method
         */
        private boolean isContainingClassValid(ResolvedMethod method)
        {
            return
            /* Global BroadcastReceiver (method of ContextWrapper, e.g. Activity) */
            Utils.isMethodContainedInSubclassOf(method, Utils.CONTEXT_WRAPPER) ||
            /* Local BroadcastReceiver (method of LocalBroadcastManager) */
            Utils.isMethodContainedInSubclassOf(method, LOCAL_BROADCAST_MANAGER);
        }
    }
}
