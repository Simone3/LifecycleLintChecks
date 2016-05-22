package it.polimi.testing.lifecycle_lint.detectors;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import it.polimi.testing.lifecycle_lint.Utils;
import lombok.ast.AstVisitor;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;

import static com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import static com.android.tools.lint.client.api.JavaParser.ResolvedNode;

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
                                                    "i.e. you should avoid registering twice (otherwise you'll receive the events twice), always "+
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
    private static final String CONTEXT_WRAPPER = "android.content.ContextWrapper";
    public static final String LOCAL_BROADCAST_MANAGER = "android.support.v4.content.LocalBroadcastManager";
    private static final String REGISTER_METHOD = "registerReceiver";
    private static final String UNREGISTER_METHOD = "unregisterReceiver";

    // Flags and data used in the search
    private static boolean foundRegister = false;
    private static MethodInvocation registerNode;
    private static boolean foundUnregister = false;

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
     */
    @Override
    public List<String> getApplicableMethodNames()
    {
        return Arrays.asList(REGISTER_METHOD, UNREGISTER_METHOD);
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

        // Create issue if we found a register but no unregister
        if(foundRegister && !foundUnregister)
        {
            context.report(ISSUE, registerNode, context.getLocation(registerNode.astName()), "Found a BroadcastReceiver `"+REGISTER_METHOD+"()` but no `"+UNREGISTER_METHOD+"()` calls in the class");
        }

        // Reset variables for next files
        foundRegister = false;
        foundUnregister = false;
        registerNode = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context)
    {
        return new Checker(context);
    }

    /**
     * Custom AST Visitor that receives method invocation calls
     */
    private static class Checker extends ForwardingAstVisitor
    {
        private final JavaContext context;

        /**
         * Constructor
         * @param context the context of the lint request
         */
        public Checker(JavaContext context)
        {
            this.context = context;
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
                if(!foundRegister)
                {
                    foundRegister = true;
                    registerNode = methodInvocation;
                }
                else
                {
                    context.report(ISSUE, methodInvocation, context.getLocation(methodInvocation.astName()), "You may be calling "+REGISTER_METHOD+"() more than once: in that case you'll receive each broadcast multiple times");
                }
            }

            // If it's the unregister method...
            else if(UNREGISTER_METHOD.equals(name))
            {
                // Set flag
                foundUnregister = true;

                // Issue if this is called during onSaveInstanceState
                if(isCalledDuringOnSaveInstanceState(methodInvocation))
                {
                    context.report(ISSUE, methodInvocation, context.getLocation(methodInvocation.astName()), "You should not call "+UNREGISTER_METHOD+"() during "+Utils.ON_SAVE_INSTANCE_STATE_METHOD+"() because it won't be called if the user moves back in the history stack");
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
            return Utils.ON_SAVE_INSTANCE_STATE_METHOD.equals(Utils.getCallerMethod(methodInvocation));
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
            Utils.isMethodContainedInSubclassOf(method, CONTEXT_WRAPPER) ||
            /* Local BroadcastReceiver (method of LocalBroadcastManager) */
            Utils.isMethodContainedInSubclassOf(method, LOCAL_BROADCAST_MANAGER);
        }
    }
}
