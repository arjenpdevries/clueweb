/*
 * ClueWeb Tools: Hadoop tools for manipulating ClueWeb collections
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.clueweb.clueweb12.app;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.clueweb.clueweb12.mapreduce.ClueWarcInputFormat;
import org.clueweb.data.ClueWarcRecord;

public class CountClueWarcRecordsNew extends Configured implements Tool {
  private static final Logger LOG = Logger.getLogger(CountClueWarcRecordsNew.class);

  private static enum Records { TOTAL, PAGES };

  private static class MyMapper extends Mapper<LongWritable, ClueWarcRecord, NullWritable, NullWritable> {

    @Override
    public void setup(Context context) {}

    @Override
    public void map(LongWritable key, ClueWarcRecord doc, Context context) throws IOException,
        InterruptedException {
      context.getCounter(Records.TOTAL).increment(1);

      String docid = doc.getHeaderMetadataItem("WARC-TREC-ID");
      if (docid != null) {
        context.getCounter(Records.PAGES).increment(1);
      }
    }
  }

  public CountClueWarcRecordsNew() {
  }

  public static final String INPUT_OPTION = "input";

  /**
   * Runs this tool.
   */
  @SuppressWarnings("static-access")
  public int run(String[] args) throws Exception {
    Options options = new Options();

    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("input path").create(INPUT_OPTION));

    CommandLine cmdline;
    CommandLineParser parser = new GnuParser();
    try {
      cmdline = parser.parse(options, args);
    } catch (ParseException exp) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(this.getClass().getName(), options);
      ToolRunner.printGenericCommandUsage(System.out);
      System.err.println("Error parsing command line: " + exp.getMessage());
      return -1;
    }

    if (!cmdline.hasOption(INPUT_OPTION)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(this.getClass().getName(), options);
      ToolRunner.printGenericCommandUsage(System.out);
      return -1;
    }

    String input = cmdline.getOptionValue(INPUT_OPTION);

    LOG.info("Tool name: " + CountClueWarcRecordsNew.class.getSimpleName());
    LOG.info(" - input: " + input);

    Job job = new Job(getConf(), CountClueWarcRecordsNew.class.getSimpleName() + ":" + input);
    job.setJarByClass(CountClueWarcRecordsNew.class);
    job.setNumReduceTasks(0);

    FileInputFormat.addInputPaths(job, input);

    job.setInputFormatClass(ClueWarcInputFormat.class);
    job.setOutputFormatClass(NullOutputFormat.class);
    job.setMapperClass(MyMapper.class);

    job.waitForCompletion(true);

    Counters counters = job.getCounters();
    int numDocs = (int) counters.findCounter(Records.PAGES).getValue();
    LOG.info("Read " + numDocs + " docs.");

    return 0;
  }

  /**
   * Dispatches command-line arguments to the tool via the <code>ToolRunner</code>.
   */
  public static void main(String[] args) throws Exception {
    LOG.info("Running " + CountClueWarcRecordsNew.class.getCanonicalName() + " with args "
        + Arrays.toString(args));
    ToolRunner.run(new CountClueWarcRecordsNew(), args);
  }
}
