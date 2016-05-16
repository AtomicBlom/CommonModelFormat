package com.github.atomicblom.client.model.cmf.obj;

import com.google.common.base.Optional;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Created by codew on 16/05/2016.
 */
public class TokenizingIterator implements Iterator<Token>
{
    private static final Pattern seperatorPattern = Pattern.compile("[ \t]+");
    private final BufferedReader reader;

    public TokenizingIterator(BufferedReader reader)
    {

        this.reader = reader;
    }

    @Override
    public boolean hasNext()
    {
        try
        {
            return reader.ready();
        } catch (IOException e)
        {
            return false;
        }
    }

    @Override
    public Token next()
    {

        final String line;
        try
        {
            line = reader.readLine().trim();
            if ("".equals(line)) {
                return new BlankLineToken();
            }
            final String[] elements = seperatorPattern.split(line);
            if (elements.length == 0) {
                //skip element
            }

            final OBJToken objToken = OBJToken.valueOf(elements[0].toUpperCase());
            return objToken.apply(Arrays.copyOf(elements, 1));

            reader.close();
        } catch (IOException e)
        {
            return new OBJToken.ENDOFFILE.apply(new String[0]);
        } catch (ModelLoaderRegistry.LoaderException e)
        {
            return new OBJToken.ENDOFFILE.apply(new String[0]);
        }
    }

    @Override
    public void remove()
    {

    }
}
