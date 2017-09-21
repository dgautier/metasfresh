/**
 * 
 */
package org.compiere.apps.form.fileimport;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/*
 * #%L
 * de.metas.adempiere.adempiere.client
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@UtilityClass
public class FileImportReader
{
	private static final char TEXT_DELIMITER = '"';
	private static final int MAX_LOADED_LINES = 100;

	final private static class MultiLineProcessor implements LineProcessor<List<String>>
	{
		private boolean openQuote = false;
		private boolean closedQuote = false;
		private final List<String> loadedDataLines = new ArrayList<>();

		@Override
		public boolean processLine(final String line) throws IOException
		{
			// if previous line had a " which is not closed, then add all to the previous line, until we meet next "
			if (CharMatcher.anyOf(line).matches(TEXT_DELIMITER))
			{
				// if we already had a delimiter, the next one is closing delimiter
				if (openQuote)
				{
					closedQuote = true;
				}
				else
				{
					openQuote = true;
				}
			}
			//
			// if open quote , add this line to the previous
			if (openQuote && !loadedDataLines.isEmpty())
			{
				final StringBuilder previousLine = new StringBuilder();
				final int index = loadedDataLines.size() - 1;
				previousLine.append(loadedDataLines.get(index));
				// append the new line, because the char exists
				previousLine.append("\n");
				previousLine.append(line);
				//
				// now remove the line and add the new line
				loadedDataLines.remove(index);
				loadedDataLines.add(previousLine.toString());
			}
			else
			{
				loadedDataLines.add(line);
			}

			//
			// reset
			if (closedQuote)
			{
				openQuote = false;
				closedQuote = false;
			}
			return true;
		}

		@Override
		public List<String> getResult()
		{
			return loadedDataLines;
		}
	}

	/**
	 * Read file that has at least on filed with multiline text
	 * <br>
	 * Assumes the <code>TEXT_DELIMITER</code> is not encountered in the field
	 * 
	 * @param file
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public List<String> readMultiLines(@NonNull final File file, @NonNull final Charset charset) throws IOException
	{
		final List<String> loadedDataLines = Files.readLines(file, charset, new MultiLineProcessor());
		return loadedDataLines;
	}

	/**
	 * Read file that has not any multi-line text
	 * 
	 * @param file
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public List<String> readRegularLines(@NonNull final File file, @NonNull final Charset charset) throws IOException
	{
		final List<String> loadedDataLines = new ArrayList<>();

		Files.readLines(file, charset, new LineProcessor<Void>()
		{
			@Override
			public boolean processLine(final String line) throws IOException
			{
				loadedDataLines.add(line);
				return true;
			}

			@Override
			public Void getResult()
			{
				return null;
			}
		});

		return loadedDataLines;
	}

	/**
	 * Build the preview lines from the loaded lines
	 * 
	 * @param lines
	 * @return
	 */
	public String buildDataPreview(final List<String> lines)
	{
		if (lines.size() > MAX_LOADED_LINES)
		{
			return buildLimitedPreview(lines);
		}
		else
		{
			final StringBuilder loadedDataPreview = new StringBuilder();
			lines.stream().forEach(item -> loadedDataPreview.append(item).append("\n"));
			return loadedDataPreview.toString();
		}
	}
	
	private String buildLimitedPreview(final List<String> lines)
	{
		final StringBuilder loadedDataPreview = new StringBuilder();
		final List<String> copyOfLoadedDataLines = Collections.unmodifiableList(lines);
		lines.forEach(item -> {
			if (copyOfLoadedDataLines.indexOf(item) < copyOfLoadedDataLines.size())
			{
				loadedDataPreview.append(item);
				loadedDataPreview.append("\n");
			}
		});
		loadedDataPreview.append("......................................................\n");
		return loadedDataPreview.toString();
	}
}
