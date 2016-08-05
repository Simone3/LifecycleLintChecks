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

public class GoogleApiClientDetector extends Detector implements Detector.JavaScanner
{
    // Issue implementation
    private static final Class<? extends Detector> DETECTOR_CLASS = GoogleApiClientDetector.class;
    private static final EnumSet<Scope> DETECTOR_SCOPE = Scope.JAVA_FILE_SCOPE;
    private static final Implementation IMPLEMENTATION = new Implementation(
        DETECTOR_CLASS,
        DETECTOR_SCOPE
    );

    // Issue description
    private static final String ISSUE_ID = "GoogleApiClientLifecycle";
    private static final String ISSUE_DESCRIPTION = "Incorrect `GoogleApiClient` lifecycle handling";
    private static final String ISSUE_EXPLANATION = "You should always disconnect a GoogleApiClient when you are done with it. "+
                                                    "For activities and fragments in most cases connection is done during onStart and "+
                                                    "disconnection during onStop().";
    private static final String MORE_INFO_URL = "https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient#nested-class-summary";

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
    private static final String GOOGLE_API_CLIENT = "com.google.android.gms.common.api.GoogleApiClient";
    private static final String CONNECT_METHOD = "connect";
    private static final String DISCONNECT_METHOD = "disconnect";
    private static final String ON_CONNECTION_FAILED_METHOD = "onConnectionFailed";

    // Flags and data used in the search
    private static boolean foundConnect = false;
    private static MethodInvocation connectNode;
    private static boolean foundDisconnect = false;

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
     * Here, for every file, we check that connections and disconnections are consistent
     */
    @Override
    public void afterCheckFile(@NonNull Context c)
    {
        if(!(c instanceof JavaContext)) return;
        JavaContext context = (JavaContext) c;

        // Create issue if we found a connection but no disconnection
        if(foundConnect && !foundDisconnect)
        {
            context.report(ISSUE, connectNode, context.getLocation(connectNode.astName()), "Found a `GoogleApiClient` `"+CONNECT_METHOD+"()` but no `"+DISCONNECT_METHOD+"()` calls in the class");
        }

        // Reset variables for next files
        foundConnect = false;
        foundDisconnect = false;
        connectNode = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context)
    {
        return new GoogleApiClientVisitor(context);
    }

    /**
     * Custom AST Visitor that receives method invocation calls
     */
    private static class GoogleApiClientVisitor extends ForwardingAstVisitor
    {
        private final JavaContext context;

        /**
         * Constructor
         * @param context the context of the lint request
         */
        public GoogleApiClientVisitor(JavaContext context)
        {
            this.context = context;
        }

        /**
         * Getter
         * @return the names of the methods we are interested in
         */
        private List<String> getApplicableMethodNames()
        {
            return Arrays.asList(CONNECT_METHOD, DISCONNECT_METHOD);
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
            if(!Utils.isMethodContainedInSubclassOf(method, GOOGLE_API_CLIENT))
            {
                return false;
            }

            // If it's the connection method...
            String name = method.getName();
            if(CONNECT_METHOD.equals(name))
            {
                foundConnect = true;
                connectNode = methodInvocation;

                // Issue if we are in an activity or fragment and this is not called during onStart
                if(Utils.isCalledInActivityOrFragment(context, methodInvocation))
                {
                    String callerMethod = Utils.getCallerMethodName(methodInvocation);

                    if(!Utils.ON_START_METHOD.equals(callerMethod) && !ON_CONNECTION_FAILED_METHOD.equals(callerMethod))
                    {
                        context.report(ISSUE, methodInvocation, context.getLocation(methodInvocation.astName()), "The best practice is to call the `GoogleApiClient` `"+CONNECT_METHOD+"()` during `"+Utils.ON_START_METHOD+"()`");
                    }
                }
            }

            // If it's the disconnect method...
            else if(DISCONNECT_METHOD.equals(name))
            {
                foundDisconnect = true;

                // Issue if we are in an activity or a fragment and this is not called during onStart
                if(Utils.isCalledInActivityOrFragment(context, methodInvocation) && !Utils.ON_STOP_METHOD.equals(Utils.getCallerMethodName(methodInvocation)))
                {
                    context.report(ISSUE, methodInvocation, context.getLocation(methodInvocation.astName()), "The best practice is to call the `GoogleApiClient` `"+DISCONNECT_METHOD+"()` during `"+Utils.ON_STOP_METHOD+"()`");
                }
            }

            return super.visitMethodInvocation(methodInvocation);
        }
    }
}
