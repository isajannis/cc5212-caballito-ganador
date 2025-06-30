package org.mdp.hadoop.cli;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class MostWinningAge {

    public static class AgeWinMapper 
        extends Mapper<Object, Text, Text, IntWritable> {
        
        private final static IntWritable one = new IntWritable(1);
        private Text ageText = new Text();
        
        @Override
        public void map(Object key, Text value, Context context
                      ) throws IOException, InterruptedException {
            
            String[] columns = value.toString().split(",", -1);
            
            
            if(columns.length < 20 || "age".equals(columns[19].trim())) {
                return;
            }
            
            String position = columns[15].trim();
            String age = columns[20].trim();
            
            
            if("1".equals(position) && !age.isEmpty()) {
                ageText.set(age);
                context.write(ageText, one);
            }
        }
    }

    public static class MaxWinningAgeReducer 
        extends Reducer<Text, IntWritable, Text, IntWritable> {
        
        private Text winningAge = new Text();
        private int maxWins = 0;
        private int currentWins = 0;
        
        @Override
        public void reduce(Text key, Iterable<IntWritable> values,
                          Context context) throws IOException, InterruptedException {
            
           
            int wins = 0;
            for(IntWritable val : values) {
                wins += val.get();
            }
            
            
            if(wins > maxWins) {
                maxWins = wins;
                winningAge.set(key);
            }
            
            
            context.write(key, new IntWritable(wins));
        }
        
        @Override
        protected void cleanup(Context context) 
            throws IOException, InterruptedException {
           
            context.write(new Text("\nAge with most wins:"), null);
            context.write(winningAge, new IntWritable(maxWins));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: MostWinningAge <input> <output>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "Most Winning Age");
        job.setJarByClass(MostWinningAge.class);
        
        job.setMapperClass(AgeWinMapper.class);
        job.setReducerClass(MaxWinningAgeReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        
        
        job.setNumReduceTasks(1);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}