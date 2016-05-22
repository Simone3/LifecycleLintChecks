package it.polimi.testing.lifecycle_lint.detectors;

import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.TextFormat;

import java.util.Collections;
import java.util.List;

import it.polimi.testing.lifecycle_lint.test_utils.AbstractDetectorTest;

public class BroadcastReceiverDetectorTest extends AbstractDetectorTest
{
    @Override
    protected Detector getDetector()
    {
        return new BroadcastReceiverDetector();
    }

    @Override
    protected List<Issue> getIssues()
    {
        return Collections.singletonList(BroadcastReceiverDetector.ISSUE);
    }

    @Override
    protected String getTestResourceDirectory()
    {
        return "broadcast_receiver";
    }

    public void IGNOREtestEmptyCase() throws Exception
    {
        String file = "EmptyTestCase.java";
        //assertEquals(NO_WARNINGS, lintFiles(file));
    }

    public void testCorrectUsageCase() throws Exception
    {
        String file = "CorrectUsageTestCase.java";
        //assertEquals(NO_WARNINGS, lintFiles(file));
    }

    public void testNoUnregisterCase() throws Exception
    {
        String file = "NoUnregisterTestCase.java";
        String warningMessage = file
                + ": Warning: "
                + BroadcastReceiverDetector.ISSUE.getBriefDescription(TextFormat.TEXT)
                + " ["
                + BroadcastReceiverDetector.ISSUE.getId()
                + "]\n"
                + "0 errors, 1 warnings\n";
        //assertEquals(warningMessage, lintFiles(file));
    }
}
