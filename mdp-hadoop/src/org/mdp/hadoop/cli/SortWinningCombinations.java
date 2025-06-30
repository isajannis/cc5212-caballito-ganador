package org.mdp.hadoop.cli;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class SortWinningCombinations {

    public static class SortMapper 
        extends Mapper<Text, Text, IntWritable, Text> {
        
        private IntWritable countWritable = new IntWritable();
        
        @Override
        public void map(Text key, Text value, Context context)
                throws IOException, InterruptedException {
            
            try {
               
                int count = Integer.parseInt(value.toString());
                countWritable.set(count);
                
               
                context.write(countWritable, key);
            } catch (NumberFormatException e) {
                
            }
        }
    }

    public static class DescendingReducer 
        extends Reducer<IntWritable, Text, Text, IntWritable> {
        
        @Override
        public void reduce(IntWritable key, Iterable<Text> values, 
                          Context context) throws IOException, InterruptedException {
            
         
            for (Text value : values) {
                context.write(value, key);
            }
        }
    }

    public static class DescendingComparator 
        extends IntWritable.Comparator {
        
        @Override
        public int compare(byte[] b1, int s1, int l1, 
                           byte[] b2, int s2, int l2) {
         
            return -super.compare(b1, s1, l1, b2, s2, l2);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        if (args.length != 2) {
            System.err.println("Usage: SortWinningCombinations <input> <output>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "Sort Winning Combinations");
        job.setJarByClass(SortWinningCombinations.class);
        
       
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        
        job.setMapperClass(SortMapper.class);
        job.setReducerClass(DescendingReducer.class);
        
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(Text.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        
     
        job.setSortComparatorClass(DescendingComparator.class);
        
      
        job.setNumReduceTasks(1);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}