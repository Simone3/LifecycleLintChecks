package it.polimi.testing.lifecycle_lint.test_utils;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.utils.SdkUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;

/**
 * Abstract class that contains common stuff
 */
public abstract class AbstractDetectorTest extends LintDetectorTest
{
    /**
     * Our relative path to the test-resources directory, which is used in the getTestResource()
     * and getTestDataRootDir() methods. Their purpose is to identify the test resource file to be used in
     * a given test case.
     */
    protected static final String PATH_TEST_RESOURCES = "/src/test/resources/";

    /**
     * The default Lint message when nothing is wrong, which we'll need for test comparisons.
     */
    protected static final String NO_WARNINGS = "No warnings.";

    protected abstract String getTestResourceDirectory();

    @Override
    protected InputStream getTestResource(String relativePath, boolean expectExists)
    {
        String path = (PATH_TEST_RESOURCES + getTestResourceDirectory() + File.separatorChar + relativePath).replace('/', File.separatorChar);
        File file = new File(getTestDataRootDir(), path);
        if(file.exists())
        {
            try
            {
                return new BufferedInputStream(new FileInputStream(file));
            }
            catch(FileNotFoundException e)
            {
                if(expectExists)
                {
                    fail("Could not find file " + relativePath);
                }
            }
        }
        return null;
    }

    private File getTestDataRootDir()
    {
        CodeSource source = getClass().getProtectionDomain().getCodeSource();
        if(source!=null)
        {
            URL location = source.getLocation();
            try
            {
                File classesDir = SdkUtils.urlToFile(location);
                return classesDir.getParentFile().getAbsoluteFile().getParentFile().getParentFile();
            }
            catch(MalformedURLException e)
            {
                fail(e.getLocalizedMessage());
            }
        }
        return null;
    }
}
