package it.polimi.testing.lifecycle_lint.detectors;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
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

import lombok.ast.AstVisitor;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;

public class BroadcastReceiverDetector extends Detector implements Detector.JavaScanner
{
    private static final Class<? extends Detector> DETECTOR_CLASS = BroadcastReceiverDetector.class;

    private static final EnumSet<Scope> DETECTOR_SCOPE = Scope.JAVA_FILE_SCOPE;

    private static final Implementation IMPLEMENTATION = new Implementation(
        DETECTOR_CLASS,
        DETECTOR_SCOPE
    );

    private static final String ISSUE_ID = "BroadcastReceiver";
    private static final String ISSUE_DESCRIPTION = "The description";
    private static final String ISSUE_EXPLANATION = "The explanation";
    private static final Category ISSUE_CATEGORY = Category.PERFORMANCE;
    private static final int ISSUE_PRIORITY = 5;
    private static final Severity ISSUE_SEVERITY = Severity.WARNING;
    public static final Issue ISSUE = Issue.create
    (
        ISSUE_ID,
        ISSUE_DESCRIPTION,
        ISSUE_EXPLANATION,
        ISSUE_CATEGORY,
        ISSUE_PRIORITY,
        ISSUE_SEVERITY,
        IMPLEMENTATION
    );

    // Classes and methods names
    private static final String CONTEXT = "android/content/Context";
    private static final String UNREGISTER_METHOD = "unregisterReceiver";
    private static final String REGISTER_METHOD = "registerReceiver";

    public BroadcastReceiverDetector()
    {

    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file)
    {
        return true;
    }

    @Override
    public EnumSet<Scope> getApplicableFiles()
    {
        return Scope.JAVA_FILE_SCOPE;
    }

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes()
    {
        return Collections.<Class<? extends Node>>singletonList(
                MethodInvocation.class
        );
    }

    @Override
    public List<String> getApplicableMethodNames()
    {
        return Arrays.asList(REGISTER_METHOD, UNREGISTER_METHOD);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor, @NonNull MethodInvocation node)
    {


        System.out.println("[METHOD] "+node);

        //context.report(ISSUE, Location.create(context.file), ISSUE.getBriefDescription(TextFormat.TEXT));
    }
}
