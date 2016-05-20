package it.polimi.testing.lifecycle_lint.registry;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

import java.util.Arrays;
import java.util.List;

import it.polimi.testing.lifecycle_lint.detectors.BroadcastReceiverDetector;


public class LifecycleIssuesRegistry extends IssueRegistry
{
    private final static List<Issue> ISSUES = Arrays.asList
    (
        BroadcastReceiverDetector.ISSUE
    );

    public LifecycleIssuesRegistry()
    {

    }

    @Override
    public List<Issue> getIssues()
    {
        return ISSUES;
    }
}
