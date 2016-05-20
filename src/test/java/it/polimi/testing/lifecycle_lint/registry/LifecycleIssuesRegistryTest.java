package it.polimi.testing.lifecycle_lint.registry;

import com.android.tools.lint.detector.api.Issue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import it.polimi.testing.lifecycle_lint.detectors.BroadcastReceiverDetector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LifecycleIssuesRegistryTest
{
    private LifecycleIssuesRegistry lifecycleIssuesRegistry;

    @Before
    public void setUp() throws Exception
    {
        lifecycleIssuesRegistry = new LifecycleIssuesRegistry();
    }

    @Test
    public void testNumberOfIssues() throws Exception
    {
        int size = lifecycleIssuesRegistry.getIssues().size();
        assertEquals(1, size);
    }

    @Test
    public void testGetIssues() throws Exception
    {
        List<Issue> actual = lifecycleIssuesRegistry.getIssues();
        assertTrue(actual.contains(BroadcastReceiverDetector.ISSUE));
    }
}
